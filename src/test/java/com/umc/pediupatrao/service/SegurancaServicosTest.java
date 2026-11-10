package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.*;
import com.umc.pediupatrao.repository.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(SegurancaServicosTest.Config.class)
class SegurancaServicosTest {
    @Configuration @EnableMethodSecurity
    static class Config {
        @Bean AuditoriaRepository auditoriaRepository() { return mock(AuditoriaRepository.class); }
        @Bean UsuarioRepository usuarioRepository() { return mock(UsuarioRepository.class); }
        @Bean AuditoriaService auditoriaService(AuditoriaRepository repo) { return new AuditoriaService(repo); }
        @Bean UsuarioService usuarioService(UsuarioRepository repo, AuditoriaService auditoria) {
            return new UsuarioService(repo, new BCryptPasswordEncoder(), auditoria);
        }
        @Bean PedidoRepository pedidoRepository() { return mock(PedidoRepository.class); }
        @Bean EntregadorRepository entregadorRepository() { return mock(EntregadorRepository.class); }
        @Bean PedidoService pedidoService(PedidoRepository repo, EntregadorRepository entregadores, AuditoriaService auditoria) {
            return new PedidoService(repo, entregadores, new PedidoAuditoriaService(auditoria),
                    new PedidoCalculoService(), new PedidoRegrasService(mock(ClienteRepository.class), mock(ProdutoRepository.class)));
        }
    }
    @Autowired AuditoriaService auditoria;
    @Autowired UsuarioService usuarios;
    @Autowired PedidoService pedidos;
    @Autowired AuditoriaRepository auditoriaRepo;
    @Autowired UsuarioRepository usuarioRepo;
    @Autowired PedidoRepository pedidoRepo;

    @BeforeEach void preparar() { reset(auditoriaRepo, usuarioRepo, pedidoRepo); }
    @AfterEach void limpar() { SecurityContextHolder.clearContext(); }
    private void autenticar(String role) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("operador", "", "ROLE_" + role));
    }
    @ParameterizedTest @ValueSource(strings = {"ADMIN", "GERENTE"})
    void gestaoConsultaAuditoriaNoServico(String role) {
        autenticar(role); when(auditoriaRepo.findAll()).thenReturn(List.of());
        assertThat(auditoria.listar()).isEmpty(); verify(auditoriaRepo).findAll();
    }
    @Test void atendenteNaoConsultaAuditoriaNemFuncionariosNoServico() {
        autenticar("ATENDENTE");
        assertThatThrownBy(() -> auditoria.listar()).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> usuarios.listarFuncionarios()).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(auditoriaRepo, usuarioRepo);
    }
    @ParameterizedTest @ValueSource(strings = {"ADMIN", "ATENDENTE"})
    void cancelamentoEDescontoExigemGerenteMesmoSemControlador(String role) {
        autenticar(role);
        assertThatThrownBy(() -> pedidos.cancelarPedido("1", "Motivo")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> pedidos.aplicarDesconto("1", java.math.BigDecimal.TEN)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(pedidoRepo);
    }
    @Test void perfilDesconhecidoNaoConsultaPedidosNoServico() {
        autenticar("DESCONHECIDO");
        assertThatThrownBy(() -> pedidos.listarPedidos()).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(pedidoRepo);
    }
    private Usuario conta() {
        Usuario conta = new Usuario(); conta.setId("1"); conta.setUsername("operador");
        conta.setRole(PerfilUsuario.ATENDENTE); conta.setPassword("hash-persistido");
        when(usuarioRepo.findById("1")).thenReturn(Optional.of(conta));
        when(usuarioRepo.save(any())).thenAnswer(call -> call.getArgument(0)); return conta;
    }
    @Test void propriaContaNaoPermiteElevacaoDePerfil() {
        autenticar("ATENDENTE"); conta(); Usuario alteracao = new Usuario();
        alteracao.setUsername("operador"); alteracao.setRole(PerfilUsuario.ADMIN);
        assertThatThrownBy(() -> usuarios.atualizarUsuario("1", alteracao)).isInstanceOf(IllegalArgumentException.class);
        verify(usuarioRepo, never()).save(any()); verifyNoInteractions(auditoriaRepo);
    }
    @Test void edicaoPropriaPreservaSenhaEPerfilESalvaAuditoriaSemSenha() {
        autenticar("ATENDENTE"); conta(); Usuario alteracao = new Usuario();
        alteracao.setUsername("operador"); alteracao.setPassword("");
        Usuario salvo = usuarios.atualizarUsuario("1", alteracao);
        assertThat(salvo.getRole()).isEqualTo(PerfilUsuario.ATENDENTE);
        assertThat(salvo.getPassword()).isEqualTo("hash-persistido");
        verify(auditoriaRepo).save(argThat(a -> a.getOperacao().equals("ALTERACAO")
                && !a.getValoresNovos().containsKey("password") && !a.getValoresAnteriores().containsKey("password")));
    }
    @Test void contaAlheiaNaoPodeSerEditada() {
        autenticar("ATENDENTE"); Usuario outra = conta(); outra.setUsername("outro");
        assertThatThrownBy(() -> usuarios.atualizarUsuario("1", new Usuario())).isInstanceOf(AccessDeniedException.class);
        verify(usuarioRepo, never()).save(any());
    }
    @Test void auditoriaPersisteSomenteDiferencasComUsuarioPerfilEJustificativa() {
        autenticar("GERENTE");
        auditoria.registrar("CANCELAMENTO", "PEDIDO", "1", Map.of("status", "RECEBIDO", "cliente", "1"),
                Map.of("status", "CANCELADO", "cliente", "1"), "Cliente desistiu");
        verify(auditoriaRepo).save(argThat(a -> a.getUsuario().equals("operador") && a.getPerfil().equals("GERENTE")
                && a.getDataHora() != null && a.getJustificativa().equals("Cliente desistiu")
                && a.getValoresAnteriores().equals(Map.of("status", "RECEBIDO"))
                && a.getValoresNovos().equals(Map.of("status", "CANCELADO"))));
    }
    @Test void naoRegistraAuditoriaSemAutenticacao() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> auditoria.registrar("TESTE", "PEDIDO", "1", Map.of(), Map.of(), null))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(auditoriaRepo);
    }
}
