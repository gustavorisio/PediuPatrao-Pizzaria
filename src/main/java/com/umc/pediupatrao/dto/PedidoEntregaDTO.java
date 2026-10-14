package com.umc.pediupatrao.dto;

public class PedidoEntregaDTO {

    private String numeroPedido;
    private String endereco;
    private String telefone;

    public PedidoEntregaDTO(String numeroPedido, String endereco, String telefone) {
        this.numeroPedido = numeroPedido;
        this.endereco = endereco;
        this.telefone = telefone;
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
}
