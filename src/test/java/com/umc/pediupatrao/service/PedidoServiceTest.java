package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.*;
import com.umc.pediupatrao.repository.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PedidoServiceTest {
    private final PedidoRepository repository = mock(PedidoRepository.class);
    private final EntregadorRepository entregadores = mock(EntregadorRepository.class);
    private final ClienteRepository clientes = mock(ClienteRepository.class);
    private final ProdutoRepository produtos = mock(ProdutoRepository.class);
    private final AuditoriaRepository auditorias = mock(AuditoriaRepository.class);
    private final PedidoRegrasService regras = new PedidoRegrasService(clientes, produtos);
    private final PedidoService service = new PedidoService(repository, entregadores,
            new PedidoAuditoriaService(new AuditoriaService(auditorias)), new PedidoCalculoService(), regras);

    @BeforeEach void configurar() {
        autenticar("GERENTE");
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(clientes.existsById("cliente")).thenReturn(true);
        Produto produto = new Produto(); produto.setId("pizza"); produto.setNome("Pizza"); produto.setPreco(25.0);
        when(produtos.findById("pizza")).thenReturn(Optional.of(produto));
    }
    @AfterEach void limpar() { SecurityContextHolder.clearContext(); }
    private void autenticar(String role) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("operador", "", "ROLE_" + role));
    }
    private Pedido pedido(StatusPedido status) {
        Pedido p = new Pedido(); p.setId("pedido"); p.setClienteId("cliente"); p.setStatus(status);
        p.setPercentualDesconto(BigDecimal.ZERO); p.setValorTotal(new BigDecimal("50.00"));
        ItemPedido item = new ItemPedido(); item.setProdutoId("pizza"); item.setDescricao("Pizza");
        item.setQuantidade(2); item.setPrecoUnitario(new BigDecimal("25.00"));
        p.setItens(List.of(item));
        when(repository.findById("pedido")).thenReturn(Optional.of(p)); return p;
    }
    @Test void criacaoCalculaNoServidorLimpaSaidaEPersisteAuditoria() {
        Pedido p = pedido(StatusPedido.FINALIZADO); p.setEntregadorSaidaId("forjado");
        p.setEntregadorSaidaNome("Forjado"); p.setEntregadorSaidaTelefone("123");
        p.getItens().getFirst().setPrecoUnitario(new BigDecimal("999.00"));
        p.setPercentualDesconto(new BigDecimal("10"));
        Pedido salvo = service.criarPedido(p);
        assertThat(salvo.getStatus()).isEqualTo(StatusPedido.RECEBIDO);
        assertThat(salvo.getValorTotal()).isEqualByComparingTo("45.00");
        assertThat(salvo.getItens().getFirst().getPrecoUnitario()).isEqualByComparingTo("25");
        assertThat(salvo.getResponsavelRegistro()).isEqualTo("operador");
        assertThat(salvo.getDataHoraEntrada()).isNotNull();
        assertThat(salvo.getEntregadorSaidaId()).isNull();
        assertThat(salvo.getEntregadorSaidaNome()).isNull();
        assertThat(salvo.getEntregadorSaidaTelefone()).isNull();
        var captor = ArgumentCaptor.forClass(Auditoria.class); verify(auditorias).save(captor.capture());
        assertThat(captor.getValue().getOperacao()).isEqualTo("INCLUSAO");
        assertThat(captor.getValue().getUsuario()).isEqualTo("operador");
        assertThat(captor.getValue().getValoresNovos()).containsEntry("valorTotal", "45.00");
    }
    @ParameterizedTest @EnumSource(value = StatusPedido.class, names = {"RECEBIDO", "EM_PREPARACAO", "PRONTO"})
    void gerenteCancelaAntesDaSaidaComJustificativaEAuditoria(StatusPedido status) {
        pedido(status); var salvo = service.cancelarPedido("pedido", "  Cliente desistiu  ");
        assertThat(salvo.getStatus()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(salvo.getJustificativaCancelamento()).isEqualTo("Cliente desistiu");
        verify(auditorias).save(argThat(a -> a.getOperacao().equals("CANCELAMENTO") && a.getJustificativa().equals("Cliente desistiu")));
    }
    @ParameterizedTest @EnumSource(value = StatusPedido.class, names = {"SAIU_PARA_ENTREGA", "RETIRADO", "FINALIZADO", "CANCELADO"})
    void naoCancelaAposSaidaOuTermino(StatusPedido status) {
        pedido(status); assertThatThrownBy(() -> service.cancelarPedido("pedido", "Motivo")).isInstanceOf(ResponseStatusException.class);
        verify(repository, never()).save(any()); verifyNoInteractions(auditorias);
    }
    @Test void exigeJustificativaDeCancelamento() {
        pedido(StatusPedido.RECEBIDO);
        assertThatThrownBy(() -> service.cancelarPedido("pedido", " ")).isInstanceOf(ResponseStatusException.class);
        verify(repository, never()).save(any());
    }
    @Test void atendenteNaoAplicaDescontoNaCriacaoNemAlteraExistente() {
        autenticar("ATENDENTE"); var p = pedido(StatusPedido.RECEBIDO); p.setPercentualDesconto(new BigDecimal("10"));
        assertThatThrownBy(() -> service.criarPedido(p)).isInstanceOf(ResponseStatusException.class);
        var dados = new Pedido(); dados.setPercentualDesconto(new BigDecimal("20"));
        assertThatThrownBy(() -> service.atualizarPedido("pedido", dados)).isInstanceOf(ResponseStatusException.class);
        verify(repository, never()).save(any());
    }
    @Test void edicaoPorAtendentePreservaDescontoAnterior() {
        autenticar("ATENDENTE"); var atual = pedido(StatusPedido.RECEBIDO); atual.setPercentualDesconto(new BigDecimal("10"));
        var dados = new Pedido(); dados.setClienteId("cliente"); dados.setItens(atual.getItens());
        var salvo = service.atualizarPedido("pedido", dados);
        assertThat(salvo.getPercentualDesconto()).isEqualByComparingTo("10");
        assertThat(salvo.getValorTotal()).isEqualByComparingTo("45");
    }
    @Test void validaLimitesDeDesconto() {
        pedido(StatusPedido.RECEBIDO);
        assertThatThrownBy(() -> service.aplicarDesconto("pedido", new BigDecimal("20.01"))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.aplicarDesconto("pedido", new BigDecimal("-1"))).isInstanceOf(ResponseStatusException.class);
        var p = service.aplicarDesconto("pedido", new BigDecimal("20"));
        // A alteração usa preços históricos persistidos, não o catálogo atual.
        assertThat(p.getValorTotal()).isEqualByComparingTo("40.00");
        verify(auditorias).save(argThat(a -> a.getOperacao().equals("DESCONTO")));
    }
    @Test void transicaoNaoPermitePularEtapasOuCancelarPorStatus() {
        pedido(StatusPedido.RECEBIDO);
        assertThatThrownBy(() -> service.atualizarStatus("pedido", "FINALIZADO", null)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.atualizarStatus("pedido", "CANCELADO", null)).isInstanceOf(ResponseStatusException.class);
        verify(repository, never()).save(any());
    }
    @Test void saidaExigeEntregadorCadastradoESalvaHistorico() {
        pedido(StatusPedido.PRONTO);
        assertThatThrownBy(() -> service.atualizarStatus("pedido", "SAIU_PARA_ENTREGA", null)).isInstanceOf(ResponseStatusException.class);
        pedido(StatusPedido.PRONTO);
        Entregador e = new Entregador(); e.setId("entregador"); e.setNome("João"); e.setTelefone("123");
        when(entregadores.findById("entregador")).thenReturn(Optional.of(e));
        Pedido salvo = service.atualizarStatus("pedido", "SAIU_PARA_ENTREGA", "entregador");
        assertThat(salvo.getEntregadorSaidaId()).isEqualTo("entregador");
        assertThat(salvo.getDataHoraSaida()).isNotNull();
        verify(auditorias).save(argThat(a -> a.getOperacao().equals("ALTERACAO_STATUS")));
    }
    @Test void naoExcluiHistoricoNemEditaPedidoForaDaSituacaoInicial() {
        pedido(StatusPedido.PRONTO);
        assertThatThrownBy(() -> service.atualizarPedido("pedido", new Pedido())).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.excluirPedido("pedido")).isInstanceOf(ResponseStatusException.class);
        verify(repository, never()).deleteById(anyString()); verify(repository, never()).save(any());
    }
    @Test void clienteProdutoEQuantidadeSaoValidados() {
        var p = pedido(StatusPedido.RECEBIDO); p.setClienteId("inexistente");
        assertThatThrownBy(() -> service.criarPedido(p)).isInstanceOf(ResponseStatusException.class);
        p.setClienteId("cliente"); p.getItens().getFirst().setQuantidade(0);
        assertThatThrownBy(() -> service.criarPedido(p)).isInstanceOf(ResponseStatusException.class);
        p.getItens().getFirst().setQuantidade(1); p.getItens().getFirst().setProdutoId("inexistente");
        assertThatThrownBy(() -> service.criarPedido(p)).isInstanceOf(ResponseStatusException.class);
        verify(repository, never()).save(any());
    }
}
