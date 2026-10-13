package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.HorarioFuncionamento;
import com.umc.pediupatrao.service.HorarioFuncionamentoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/horarios")
public class HorarioFuncionamentoController {
    private final HorarioFuncionamentoService service;

    public HorarioFuncionamentoController(HorarioFuncionamentoService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    public ResponseEntity<?> listar() { return ResponseEntity.ok(service.listarTodos()); }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<HorarioFuncionamento> salvar(@RequestBody HorarioFuncionamento horario) {
        return ResponseEntity.ok(service.salvar(horario));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<HorarioFuncionamento> atualizar(@PathVariable String id,
            @RequestBody HorarioFuncionamento horario) {
        horario.setId(id);
        return ResponseEntity.ok(service.salvar(horario));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
