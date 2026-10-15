package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.TipoProduto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoDTO {

    private String id;
    private String nome;
    private TipoProduto tipo;
    private String tipoPizza;
    private String sabor;
    private Double preco;
    private Boolean ativo;

    public ProdutoDTO(Produto produto) {
        this(produto.getId(), produto.getNome(), produto.getTipo(), produto.getTipoPizza(),
                produto.getSabor(), produto.getPreco(), produto.getAtivo());
    }

    public Produto toEntity() {
        Produto produto = new Produto();
        produto.setId(id);
        produto.setNome(nome);
        produto.setTipo(tipo);
        produto.setTipoPizza(tipoPizza);
        produto.setSabor(sabor);
        produto.setPreco(preco);
        produto.setAtivo(ativo);
        return produto;
    }
}
