package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Optional;
import java.util.Set;

@Service
public class ProdutoService {

    private static final Set<String> TIPOS_PERMITIDOS = Set.of("Pizza", "Lanche", "Bebida", "Opcional");

    @Autowired
    private ProdutoRepository produtoRepository;

    public Produto novoProduto(Produto produto) {
        return produtoRepository.save(produto);
    }

    public List<Produto> listarProdutos() {
        return produtoRepository.findAll();
    }

    public Optional<Produto> buscarPorId(String id) {
        return produtoRepository.findById(id);
    }

    // Método para excluir cliente
    public void excluir(String id) {
        produtoRepository.deleteById(id);
    }

    // Método para salvar um novo cliente ou atualizar um cliente existente
    public Produto salvar(Produto produto) {
        validarProduto(produto);
        if (produto.getAtivo() == null) {
            produto.setAtivo(false);
        }

        // Se o cliente não tem ID (novo cliente), salva como novo
        if (produto.getId() == null) {
            return produtoRepository.save(produto); // Cria um novo cliente
        } // Se já tem ID (cliente existente), atualiza
        else {
            // Verifica se o cliente existe antes de atualizar
            if (produtoRepository.existsById(produto.getId())) {
                return produtoRepository.save(produto); // Atualiza o cliente existente
            } else {
                throw new IllegalArgumentException("Cliente não encontrado para atualização.");
            }
        }
    }

    private void validarProduto(Produto produto) {
        if (produto == null || produto.getNome() == null || produto.getNome().isBlank()) {
            throw new IllegalArgumentException("O nome do produto é obrigatório.");
        }
        if (!TIPOS_PERMITIDOS.contains(produto.getTipo())) {
            throw new IllegalArgumentException(
                    "O tipo deve ser Pizza, Lanche, Bebida ou Opcional.");
        }
        if ("Pizza".equals(produto.getTipo())
                && (produto.getTipoPizza() == null || produto.getTipoPizza().isBlank()
                        || produto.getSabor() == null || produto.getSabor().isBlank())) {
            throw new IllegalArgumentException(
                    "Tipo de pizza e sabor são obrigatórios para produtos do tipo Pizza.");
        }
        if (produto.getPreco() == null || produto.getPreco() < 0) {
            throw new IllegalArgumentException("O preço deve ser informado e não pode ser negativo.");
        }
    }
}
