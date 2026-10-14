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
    // A equipe de atendimento pode consultar os horários para orientar clientes.
    public ResponseEntity<?> listar() { return ResponseEntity.ok(service.listarTodos()); }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    // Somente a gestão pode cadastrar um novo horário de funcionamento.
    public ResponseEntity<HorarioFuncionamento> salvar(@RequestBody HorarioFuncionamento horario) {
        return ResponseEntity.ok(service.salvar(horario));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    // Atualiza o horário identificado pela rota sem aceitar outro ID no corpo.
    public ResponseEntity<HorarioFuncionamento> atualizar(@PathVariable String id,
            @RequestBody HorarioFuncionamento horario) {
        horario.setId(id);
        return ResponseEntity.ok(service.salvar(horario));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    // Remove uma configuração de horário administrada pela gerência.
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
