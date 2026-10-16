package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.PedidoEntregaDTO;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final PedidoAuditoriaService pedidoAuditoriaService;
    private final PedidoCalculoService pedidoCalculoService;
    private final PedidoEntregaService pedidoEntregaService;
    private final PedidoRegrasService pedidoRegrasService;

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public Pedido criarPedido(Pedido pedido) {
        pedidoRegrasService.validarPedido(pedido);
        pedido.setId(null);
        pedido.gerarId();
        pedido.setNumeroPedido(proximoNumeroPedido());
        pedido.setStatus(StatusPedido.RECEBIDO);
        pedido.setValorTotal(pedidoCalculoService.calcularValorTotal(pedido));
        pedido.setResponsavelRegistro(pedidoRegrasService.usuarioAutenticado());
        pedido.setDataHoraEntrada(LocalDateTime.now());
        pedido.setResponsavelSaida(null);
        pedido.setDataHoraSaida(null);
        pedido.setJustificativaCancelamento(null);

        Pedido salvo = pedidoRepository.save(pedido);
        pedidoAuditoriaService.registrar("INCLUSAO", salvo, Map.of(), null);
        return salvo;
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    public List<PedidoEntregaDTO> listarPedidosParaEntrega() {
        return pedidoEntregaService.converterLista(pedidoRepository.findAll());
    }

    public PedidoEntregaDTO pedidoParaEntrega(String id) {
        return pedidoEntregaService.converter(buscarPedido(id));
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public Pedido atualizarPedido(String id, Pedido dados) {
        if (dados == null) {
            throw pedidoRegrasService.erro("Os dados do pedido devem ser informados.");
        }
        Pedido pedido = buscarPedido(id);
        if (pedido.getStatus() != StatusPedido.RECEBIDO) {
            throw pedidoRegrasService.erro("Somente pedidos recebidos podem ser editados.");
        }

        prepararDadosParaEdicao(pedido, dados);
        Map<String, String> dadosAnteriores = pedidoAuditoriaService.dadosPedido(pedido);
        copiarDadosEditaveis(pedido, dados);
        BigDecimal descontoAnterior = pedidoRegrasService.percentualDoPedido(pedido);
        pedido.setPercentualDesconto(dados.getPercentualDesconto());
        pedido.setValorTotal(pedidoCalculoService.calcularValorTotal(pedido));

        Pedido salvo = pedidoRepository.save(pedido);
        String operacao = descontoAnterior.compareTo(pedido.getPercentualDesconto()) == 0
                ? "ALTERACAO" : "DESCONTO";
        pedidoAuditoriaService.registrar(operacao, salvo, dadosAnteriores, null);
        return salvo;
    }

    public void excluirPedido(String id) {
        throw pedidoRegrasService.erro(
                "Pedidos devem ser cancelados com justificativa; o histórico é preservado.");
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(String id, BigDecimal percentual) {
        Pedido pedido = buscarPedido(id);
        if (!List.of(StatusPedido.RECEBIDO, StatusPedido.EM_PREPARACAO, StatusPedido.PRONTO)
                .contains(pedido.getStatus())) {
            throw pedidoRegrasService.erro(
                    "Descontos só podem ser alterados antes da saída do pedido.");
        }
        pedidoRegrasService.validarPercentual(percentual);
        Map<String, String> antes = pedidoAuditoriaService.dadosPedido(pedido);
        pedido.setPercentualDesconto(percentual);
        pedido.setValorTotal(pedidoCalculoService.calcularValorTotal(pedido));

        Pedido salvo = pedidoRepository.save(pedido);
        pedidoAuditoriaService.registrar("DESCONTO", salvo, antes, null);
        return salvo;
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE', 'ENTREGADOR')")
    public Pedido atualizarStatus(String id, String status) {
        Pedido pedido = buscarPedido(id);
        StatusPedido novoStatus = StatusPedido.fromValue(status);
        pedidoRegrasService.validarTransicao(pedido.getStatus(), novoStatus);

        Map<String, String> antes = pedidoAuditoriaService.dadosPedido(pedido);
        pedido.setStatus(novoStatus);
        registrarSaidaQuandoNecessario(pedido);

        Pedido salvo = pedidoRepository.save(pedido);
        pedidoAuditoriaService.registrar("ALTERACAO_STATUS", salvo, antes, null);
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelarPedido(String id, String justificativa) {
        Pedido pedido = buscarPedido(id);
        pedidoRegrasService.exigirGerente();
        if (pedidoRegrasService.isBlank(justificativa)) {
            throw pedidoRegrasService.erro("A justificativa do cancelamento é obrigatória.");
        }
        if (!List.of(StatusPedido.RECEBIDO, StatusPedido.EM_PREPARACAO, StatusPedido.PRONTO)
                .contains(pedido.getStatus())) {
            throw pedidoRegrasService.erro(
                    "O pedido só pode ser cancelado antes da saída para entrega ou retirada.");
        }

        Map<String, String> antes = pedidoAuditoriaService.dadosPedido(pedido);
        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setJustificativaCancelamento(justificativa.trim());
        Pedido salvo = pedidoRepository.save(pedido);
        pedidoAuditoriaService.registrar("CANCELAMENTO", salvo, antes,
                pedido.getJustificativaCancelamento());
        return salvo;
    }

    public boolean usuarioEhEntregador() {
        return pedidoRegrasService.usuarioEhEntregador();
    }

    private void prepararDadosParaEdicao(Pedido pedido, Pedido dados) {
        if (!pedidoRegrasService.podeAplicarDesconto()) {
            if (dados.getPercentualDesconto() != null) {
                if (dados.getPercentualDesconto().compareTo(
                        pedidoRegrasService.percentualDoPedido(pedido)) != 0) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Somente o GERENTE pode alterar descontos.");
                }
            }
            dados.setPercentualDesconto(BigDecimal.ZERO);
            pedidoRegrasService.validarPedido(dados);
            dados.setPercentualDesconto(pedidoRegrasService.percentualDoPedido(pedido));
            return;
        }
        pedidoRegrasService.validarPedido(dados);
    }

    private void copiarDadosEditaveis(Pedido pedido, Pedido dados) {
        pedido.setClienteId(dados.getClienteId());
        pedido.setTipoPizza(dados.getTipoPizza());
        pedido.setQuantidade(dados.getQuantidade());
        pedido.setSabor(dados.getSabor());
        pedido.setItens(dados.getItens());
    }

    private void registrarSaidaQuandoNecessario(Pedido pedido) {
        if (List.of(StatusPedido.SAIU_PARA_ENTREGA, StatusPedido.RETIRADO)
                .contains(pedido.getStatus())) {
            pedido.setResponsavelSaida(pedidoRegrasService.usuarioAutenticado());
            pedido.setDataHoraSaida(LocalDateTime.now());
        }
    }

    private Pedido buscarPedido(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Pedido não encontrado."));
    }

    private int proximoNumeroPedido() {
        return pedidoRepository.findTopByOrderByNumeroPedidoDesc()
                .map(pedido -> pedido.getNumeroPedido() == null
                        ? 1 : pedido.getNumeroPedido() + 1)
                .orElse(1);
    }
}
