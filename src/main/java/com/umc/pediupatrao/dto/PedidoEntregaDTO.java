package com.umc.pediupatrao.dto;

import java.time.LocalDateTime;

public class PedidoEntregaDTO {

    private String numeroPedido;
    private String endereco;
    private LocalDateTime dataHoraEntrada;

    public PedidoEntregaDTO(String numeroPedido, String endereco, LocalDateTime dataHoraEntrada) {
        this.numeroPedido = numeroPedido;
        this.endereco = endereco;
        this.dataHoraEntrada = dataHoraEntrada;
    }

    public String getNumeroPedido() {
        return numeroPedido;
    }

    public String getEndereco() {
        return endereco;
    }

    public LocalDateTime getDataHoraEntrada() {
        return dataHoraEntrada;
    }
}
