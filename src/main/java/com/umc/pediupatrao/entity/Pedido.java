package com.umc.pediupatrao.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "pedidos")
public class Pedido {
    @Id
    private String id;
    private String clienteId;
    private String tipoPizza;
    private int quantidade;
    private String sabor;
    private String status;
    private java.util.List<ItemPedido> itens;
    private java.math.BigDecimal percentualDesconto;
    private java.math.BigDecimal valorTotal;
    private String responsavelRegistro;
    private java.time.LocalDateTime dataHoraEntrada;
    private String responsavelSaida;
    private java.time.LocalDateTime dataHoraSaida;
    private String justificativaCancelamento;
    private java.util.List<PedidoAuditoria> auditoria = new java.util.ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public String getTipoPizza() {
        return tipoPizza;
    }

    public void setTipoPizza(String tipoPizza) {
        this.tipoPizza = tipoPizza;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public String getSabor() {
        return sabor;
    }

    public void setSabor(String sabor) {
        this.sabor = sabor;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.util.List<ItemPedido> getItens() {
        return itens;
    }

    public void setItens(java.util.List<ItemPedido> itens) {
        this.itens = itens;
    }

    public java.math.BigDecimal getPercentualDesconto() {
        return percentualDesconto;
    }

    public void setPercentualDesconto(java.math.BigDecimal percentualDesconto) {
        this.percentualDesconto = percentualDesconto;
    }

    public java.math.BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(java.math.BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public String getResponsavelRegistro() {
        return responsavelRegistro;
    }

    public void setResponsavelRegistro(String responsavelRegistro) {
        this.responsavelRegistro = responsavelRegistro;
    }

    public java.time.LocalDateTime getDataHoraEntrada() {
        return dataHoraEntrada;
    }

    public void setDataHoraEntrada(java.time.LocalDateTime dataHoraEntrada) {
        this.dataHoraEntrada = dataHoraEntrada;
    }

    public String getResponsavelSaida() {
        return responsavelSaida;
    }

    public void setResponsavelSaida(String responsavelSaida) {
        this.responsavelSaida = responsavelSaida;
    }

    public java.time.LocalDateTime getDataHoraSaida() {
        return dataHoraSaida;
    }

    public void setDataHoraSaida(java.time.LocalDateTime dataHoraSaida) {
        this.dataHoraSaida = dataHoraSaida;
    }

    public String getJustificativaCancelamento() {
        return justificativaCancelamento;
    }

    public void setJustificativaCancelamento(String justificativaCancelamento) {
        this.justificativaCancelamento = justificativaCancelamento;
    }

    public java.util.List<PedidoAuditoria> getAuditoria() {
        return auditoria;
    }

    public void setAuditoria(java.util.List<PedidoAuditoria> auditoria) {
        this.auditoria = auditoria;
    }
}
