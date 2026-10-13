package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    public List<Cliente> listarClientes() {
        return clienteService.listarClientes();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Void> excluirCliente(@PathVariable String id) {
        clienteService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    public ResponseEntity<Cliente> salvarCliente(@RequestBody Cliente cliente) {
        Cliente salvo = clienteService.salvar(cliente);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    public ResponseEntity<Cliente> atualizarCliente(@PathVariable String id, @RequestBody Cliente cliente) {
        cliente.setId(id);
        Cliente atualizado = clienteService.salvar(cliente);
        return ResponseEntity.ok(atualizado);
    }
}
