package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.TipoProduto;
import com.umc.pediupatrao.exception.ProdutoException;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public Produto novoProduto(Produto produto) {
        return salvar(produto);
    }

    public List<Produto> listarProdutos() {
        return produtoRepository.findAll();
    }

    public Optional<Produto> buscarPorId(String id) {
        return produtoRepository.findById(id);
    }

    // Método para excluir produto
    public void excluir(String id) {
        produtoRepository.deleteById(id);
    }

    // Método para salvar um novo produto ou atualizar um produto existente
    public Produto salvar(Produto produto) {
        validarProduto(produto);
        if (produto.getAtivo() == null) {
            produto.setAtivo(false);
        }

        // Se o produto não tem ID (novo produto), salva como novo
        if (produto.getId() == null) {
            produto.gerarId();
            return produtoRepository.save(produto);
        }
        // Se já tem ID, verifica se o produto existe antes de atualizar
        if (produtoRepository.existsById(produto.getId())) {
            return produtoRepository.save(produto);
        }
        throw new ProdutoException("Produto não encontrado para atualização.");
    }

    private void validarProduto(Produto produto) {
        if (produto == null) {
            throw new ProdutoException("O produto é obrigatório.");
        }
        if (produto.getNome() == null) {
            throw new ProdutoException("O nome do produto é obrigatório.");
        }
        if (produto.getNome().isBlank()) {
            throw new ProdutoException("O nome do produto é obrigatório.");
        }
        if (produto.getTipo() == null) {
            throw new ProdutoException("O tipo do produto é obrigatório.");
        }
        if (TipoProduto.PIZZA.equals(produto.getTipo())) {
            if (produto.getTipoPizza() == null) {
                throw new ProdutoException("O tipo de pizza é obrigatório.");
            }
            if (produto.getTipoPizza().isBlank()) {
                throw new ProdutoException("O tipo de pizza é obrigatório.");
            }
            if (produto.getSabor() == null) {
                throw new ProdutoException("O sabor é obrigatório para pizzas.");
            }
            if (produto.getSabor().isBlank()) {
                throw new ProdutoException("O sabor é obrigatório para pizzas.");
            }
        }
        if (produto.getPreco() == null) {
            throw new ProdutoException("O preço deve ser informado.");
        }
        if (produto.getPreco() < 0) {
            throw new ProdutoException("O preço não pode ser negativo.");
        }
    }
}
