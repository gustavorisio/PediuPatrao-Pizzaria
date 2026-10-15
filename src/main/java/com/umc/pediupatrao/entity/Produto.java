package com.umc.pediupatrao.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "produtos")
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String nome;
    // Pizza, Lanche, Bebida ou Opcional
    @Enumerated(EnumType.STRING)
    private TipoProduto tipo;
    private String tipoPizza;
    private String sabor;
    private Double preco;
    private Boolean ativo;

    public void gerarId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
