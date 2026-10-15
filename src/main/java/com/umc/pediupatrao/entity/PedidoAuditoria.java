package com.umc.pediupatrao.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PedidoAuditoria {

    private String operacao;
    private String usuario;
    private LocalDateTime dataHora;
    private String statusAnterior;
    private String statusPosterior;
    private String justificativa;
    private BigDecimal descontoAnterior;
    private BigDecimal descontoPosterior;
    private BigDecimal valorTotalAnterior;
    private BigDecimal valorTotalPosterior;

    public String getOperacao() {
        return operacao;
    }

    public void setOperacao(String operacao) {
        this.operacao = operacao;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getStatusAnterior() {
        return statusAnterior;
    }

    public void setStatusAnterior(String statusAnterior) {
        this.statusAnterior = statusAnterior;
    }

    public String getStatusPosterior() {
        return statusPosterior;
    }

    public void setStatusPosterior(String statusPosterior) {
        this.statusPosterior = statusPosterior;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }

    public BigDecimal getDescontoAnterior() {
        return descontoAnterior;
    }

    public void setDescontoAnterior(BigDecimal descontoAnterior) {
        this.descontoAnterior = descontoAnterior;
    }

    public BigDecimal getDescontoPosterior() {
        return descontoPosterior;
    }

    public void setDescontoPosterior(BigDecimal descontoPosterior) {
        this.descontoPosterior = descontoPosterior;
    }

    public BigDecimal getValorTotalAnterior() {
        return valorTotalAnterior;
    }

    public void setValorTotalAnterior(BigDecimal valorTotalAnterior) {
        this.valorTotalAnterior = valorTotalAnterior;
    }

    public BigDecimal getValorTotalPosterior() {
        return valorTotalPosterior;
    }

    public void setValorTotalPosterior(BigDecimal valorTotalPosterior) {
        this.valorTotalPosterior = valorTotalPosterior;
    }
}
