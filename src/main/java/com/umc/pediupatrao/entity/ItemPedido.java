package com.umc.pediupatrao.entity;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ItemPedido {

    private String produtoId;
    private String descricao;
    private int quantidade;
    private BigDecimal precoUnitario;
}
