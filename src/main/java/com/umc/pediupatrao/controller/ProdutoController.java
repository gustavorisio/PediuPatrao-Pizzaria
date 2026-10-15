package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.ProdutoDTO;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.service.ProdutoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
public class ProdutoController {

    private final ProdutoService produtoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    // Atendimento consulta o catálogo, enquanto a gestão controla sua manutenção.
    public List<ProdutoDTO> listarProdutos() {
        return produtoService.listarProdutos().stream()
                .map(ProdutoDTO::new)
                .toList();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    // A remoção do catálogo é uma operação exclusiva da gestão.
    public ResponseEntity<Void> excluirProduto(@PathVariable String id) {
        produtoService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    // Registra um novo item no catálogo de produtos.
    public ResponseEntity<ProdutoDTO> salvarProduto(@RequestBody ProdutoDTO produtoDTO) {
        Produto salvo = produtoService.salvar(produtoDTO.toEntity());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ProdutoDTO(salvo));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    // Atualiza o produto correspondente ao ID informado na rota.
    public ResponseEntity<ProdutoDTO> atualizarProduto(@PathVariable String id,
            @RequestBody ProdutoDTO produtoDTO) {
        produtoDTO.setId(id);
        Produto atualizado = produtoService.salvar(produtoDTO.toEntity());
        return ResponseEntity.ok(new ProdutoDTO(atualizado));
    }
}
