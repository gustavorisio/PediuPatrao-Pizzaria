package com.umc.pediupatrao.dto;

import java.time.LocalDateTime;

public class PedidoEntregaDTO {

    private String numeroPedido;
    private String endereco;
    private String telefone;
    private LocalDateTime dataHoraEntrada;

    public PedidoEntregaDTO(String numeroPedido, String endereco, String telefone,
            LocalDateTime dataHoraEntrada) {
        this.numeroPedido = numeroPedido;
        this.endereco = endereco;
        this.telefone = telefone;
        this.dataHoraEntrada = dataHoraEntrada;
    }

    public String getNumeroPedido() {
        return numeroPedido;
    }

    public String getEndereco() {
        return endereco;
    }

    public String getTelefone() {
        return telefone;
    }

    public LocalDateTime getDataHoraEntrada() {
        return dataHoraEntrada;
    }
}
