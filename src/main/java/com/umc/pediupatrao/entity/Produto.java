package com.umc.pediupatrao.entity;

import lombok.Getter;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "produtos")
@Getter
@Setter
public class Produto {
    
    @Id
    private String id;
    private String nome;
    private String tipo; // Pizza, Esfiha, Bebida, Lanche
    private String tipoPizza;
    private String sabor;
    private Double preco;
    private Boolean ativo;
}
