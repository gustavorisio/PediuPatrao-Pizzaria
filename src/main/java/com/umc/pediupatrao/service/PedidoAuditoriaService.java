package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.PedidoAuditoria;
import com.umc.pediupatrao.entity.StatusPedido;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PedidoAuditoriaService {

    private final AuditoriaService auditoriaService;

    public PedidoAuditoriaService(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    public void prepararRecebimento(Pedido pedido) {
        registrarHistorico(pedido, "RECEBIMENTO", null, pedido.getStatus(), null,
                BigDecimal.ZERO, pedido.getPercentualDesconto(), null, pedido.getValorTotal());
    }

    public void registrarRecebimento(Pedido pedido) {
        registrarPersistencia("INCLUSAO", pedido, new HashMap<>(), dadosPedido(pedido), null);
        log.info("Pedido recebido: id={}, numero={}", pedido.getId(), pedido.getNumeroPedido());
    }

    public void prepararAlteracao(Pedido pedido, BigDecimal descontoAnterior, BigDecimal valorAnterior) {
        registrarHistorico(pedido, "ALTERACAO_PEDIDO", pedido.getStatus(), pedido.getStatus(),
                null, descontoAnterior, pedido.getPercentualDesconto(),
                valorAnterior, pedido.getValorTotal());
    }

    public void registrarAlteracao(Pedido pedido, BigDecimal descontoAnterior, BigDecimal valorAnterior) {
        String operacao = descontoAnterior.compareTo(pedido.getPercentualDesconto()) != 0
                ? "DESCONTO"
                : "ALTERACAO";
        registrarPersistencia(operacao, pedido, dadosPedidoAntes(pedido, descontoAnterior, valorAnterior),
                dadosPedido(pedido), null);
        log.info("Pedido alterado: id={}, operacao={}", pedido.getId(), operacao);
    }

    public void prepararAlteracaoStatus(Pedido pedido, StatusPedido statusAnterior) {
        registrarHistorico(pedido, "ALTERACAO_STATUS", statusAnterior, pedido.getStatus(),
                null, pedido.getPercentualDesconto(), pedido.getPercentualDesconto(),
                pedido.getValorTotal(), pedido.getValorTotal());
    }

    public void registrarAlteracaoStatus(Pedido pedido, StatusPedido statusAnterior) {
        registrarPersistencia("ALTERACAO_STATUS", pedido,
                mapa("status", statusValue(statusAnterior)),
                mapa("status", statusValue(pedido.getStatus())), null);
        log.info("Status do pedido alterado: id={}, anterior={}, atual={}",
                pedido.getId(), statusValue(statusAnterior), statusValue(pedido.getStatus()));
    }

    public void prepararCancelamento(Pedido pedido, StatusPedido statusAnterior,
            String justificativa) {
        registrarHistorico(pedido, "CANCELAMENTO", statusAnterior, pedido.getStatus(),
                justificativa, pedido.getPercentualDesconto(), pedido.getPercentualDesconto(),
                pedido.getValorTotal(), pedido.getValorTotal());
    }

    public void registrarCancelamento(Pedido pedido, StatusPedido statusAnterior,
            String justificativa) {
        registrarPersistencia("CANCELAMENTO", pedido,
                mapa("status", statusValue(statusAnterior)),
                mapa("status", statusValue(pedido.getStatus())), justificativa);
        log.info("Pedido cancelado: id={}, statusAnterior={}",
                pedido.getId(), statusValue(statusAnterior));
    }

    private void registrarPersistencia(String operacao, Pedido pedido,
            Map<String, String> antes, Map<String, String> depois, String justificativa) {
        auditoriaService.registrar(operacao, "PEDIDO", pedido.getId(), antes, depois, justificativa);
    }

    private void registrarHistorico(Pedido pedido, String operacao, StatusPedido statusAnterior,
            StatusPedido statusPosterior, String justificativa, BigDecimal descontoAnterior,
            BigDecimal descontoPosterior, BigDecimal valorAnterior, BigDecimal valorPosterior) {
        if (pedido.getAuditoria() == null) {
            pedido.setAuditoria(new ArrayList<>());
        }
        PedidoAuditoria registro = new PedidoAuditoria();
        registro.setOperacao(operacao);
        registro.setUsuario(usuarioAtual());
        registro.setDataHora(LocalDateTime.now());
        registro.setStatusAnterior(statusValue(statusAnterior));
        registro.setStatusPosterior(statusValue(statusPosterior));
        registro.setJustificativa(justificativa);
        registro.setDescontoAnterior(descontoAnterior);
        registro.setDescontoPosterior(descontoPosterior);
        registro.setValorTotalAnterior(valorAnterior);
        registro.setValorTotalPosterior(valorPosterior);
        pedido.getAuditoria().add(registro);
    }

    private Map<String, String> dadosPedido(Pedido pedido) {
        return mapa("status", statusValue(pedido.getStatus()),
                "percentualDesconto", String.valueOf(pedido.getPercentualDesconto()),
                "valorTotal", String.valueOf(pedido.getValorTotal()));
    }

    private Map<String, String> dadosPedidoAntes(Pedido pedido, BigDecimal desconto,
            BigDecimal valorTotal) {
        return mapa("status", statusValue(pedido.getStatus()),
                "percentualDesconto", String.valueOf(desconto),
                "valorTotal", String.valueOf(valorTotal));
    }

    private Map<String, String> mapa(String... valores) {
        Map<String, String> resultado = new HashMap<>();
        for (int i = 0; i + 1 < valores.length; i += 2) {
            resultado.put(valores[i], valores[i + 1]);
        }
        return resultado;
    }

    private String statusValue(StatusPedido status) {
        return status == null ? null : status.name();
    }

    private String usuarioAtual() {
        return auditoriaService.usuarioAtual();
    }
}
