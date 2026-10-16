package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Entregador;
import com.umc.pediupatrao.service.EntregadorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/entregadores")
public class EntregadorController {
    private final EntregadorService service;

    public EntregadorController(EntregadorService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    // Apenas as roles de gestão podem consultar o cadastro de entregadores.
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    // O cadastro é mantido separado dos usuários que acessam o sistema.
    public ResponseEntity<Entregador> salvar(@RequestBody Entregador entregador) {
        return ResponseEntity.ok(service.salvar(entregador));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    // O ID informado na rota identifica o entregador que será atualizado.
    public ResponseEntity<Entregador> atualizar(@PathVariable String id, @RequestBody Entregador entregador) {
        entregador.setId(id);
        return ResponseEntity.ok(service.salvar(entregador));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    // Exclui somente registros de entregadores administrados pela gerência.
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
