package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.service.DashboardService;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
public class DashboardController {
    private final DashboardService service;
    public DashboardController(DashboardService service) { this.service = service; }

    @GetMapping("/resumo")
    public DashboardService.Resumo resumo(@RequestParam(required = false) String inicio,
            @RequestParam(required = false) String fim, @RequestParam(defaultValue = "dia") String agrupamento,
            @RequestParam(required = false) String status) {
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        try {
            return service.consultar(inicio == null ? hoje.withDayOfMonth(1) : LocalDate.parse(inicio),
                    fim == null ? hoje : LocalDate.parse(fim), agrupamento, StatusPedido.fromValue(status));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Datas devem usar o formato AAAA-MM-DD.");
        }
    }

    @GetMapping("/filtros")
    public Map<String, Object> filtros() {
        return Map.of("situacoes", StatusPedido.values(), "agrupamentos", List.of("dia", "mes"),
                "maximoDias", 366, "fusoHorario", "America/Sao_Paulo");
    }
}
