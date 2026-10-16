package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import lombok.RequiredArgsConstructor;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    private static final Set<String> TIPOS_PERMITIDOS = Set.of("Pizza", "Lanche", "Bebida", "Opcional");

    private final ProdutoRepository produtoRepository;

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
        if (produto.getId() != null) {
            if (!produtoRepository.existsById(produto.getId())) {
                throw new IllegalArgumentException("Produto não encontrado para atualização.");
            }
        }
        return produtoRepository.save(produto);
    }

    private void validarProduto(Produto produto) {
        if (produto == null) {
            throw new IllegalArgumentException("O nome do produto é obrigatório.");
        }
        validarTexto(produto.getNome(), "O nome do produto é obrigatório.");
        validarTexto(produto.getTipo(), "O tipo do produto é obrigatório.");
        if (!TIPOS_PERMITIDOS.contains(produto.getTipo())) {
            throw new IllegalArgumentException(
                    "O tipo deve ser Pizza, Lanche, Bebida ou Opcional.");
        }
        if ("Pizza".equals(produto.getTipo())) {
            validarTexto(produto.getTipoPizza(), "O tipo de pizza é obrigatório.");
            validarTexto(produto.getSabor(), "O sabor é obrigatório.");
        }
        if (produto.getPreco() == null) {
            throw new IllegalArgumentException("O preço deve ser informado e não pode ser negativo.");
        }
        if (produto.getPreco() < 0) {
            throw new IllegalArgumentException("O preço deve ser informado e não pode ser negativo.");
        }
    }

    private void validarTexto(String texto, String mensagem) {
        if (texto == null) {
            throw new IllegalArgumentException(mensagem);
        }
        if (texto.isBlank()) {
            throw new IllegalArgumentException(mensagem);
        }
    }
}
