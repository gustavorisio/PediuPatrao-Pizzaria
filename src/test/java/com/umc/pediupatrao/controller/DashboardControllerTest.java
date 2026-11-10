package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.service.DashboardService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean DashboardService service;
    @MockitoBean UsuarioDetailsService users;

    @Test void anonimoRecebe401Json() throws Exception {
        mvc.perform(get("/api/dashboard/resumo")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").exists());
    }
    @Test @WithMockUser(roles = "ATENDENTE") void atendenteRecebe403() throws Exception {
        mvc.perform(get("/api/dashboard/resumo")).andExpect(status().isForbidden()).andExpect(jsonPath("$.message").exists());
        verifyNoInteractions(service);
    }
    @Test @WithMockUser(roles = "ADMIN") void administradorPodeConsultarFiltros() throws Exception {
        mvc.perform(get("/api/dashboard/filtros")).andExpect(status().isOk())
                .andExpect(jsonPath("$.situacoes.length()").value(7)).andExpect(jsonPath("$.maximoDias").value(366));
    }
    @Test @WithMockUser(roles = "GERENTE") void gerenteRecebeResumoJson() throws Exception {
        var date = LocalDate.of(2026, 11, 1);
        var result = new DashboardService(null).calcular(date, date, "dia", StatusPedido.FINALIZADO, List.of());
        when(service.consultar(date, date, "dia", StatusPedido.FINALIZADO)).thenReturn(result);
        mvc.perform(get("/api/dashboard/resumo").param("inicio", "2026-11-01").param("fim", "2026-11-01").param("status", "FINALIZADO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.indicadores.pedidos").value(0))
                .andExpect(jsonPath("$.vendas.length()").value(1)).andExpect(jsonPath("$.status").value("FINALIZADO"));
    }
    @Test @WithMockUser(roles = "ADMIN") void dataMalformadaRetorna400() throws Exception {
        mvc.perform(get("/api/dashboard/resumo").param("inicio", "2026-02-30"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Datas devem usar o formato AAAA-MM-DD."));
    }
    @Test @WithMockUser(roles = "ADMIN") void statusInvalidoRetorna400() throws Exception {
        mvc.perform(get("/api/dashboard/resumo").param("status", "INEXISTENTE"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
    }
    @Test @WithMockUser(roles = "ADMIN") void regraInvalidaRetorna400() throws Exception {
        when(service.consultar(any(), any(), eq("hora"), isNull())).thenThrow(new IllegalArgumentException("Agrupamento deve ser dia ou mes."));
        mvc.perform(get("/api/dashboard/resumo").param("agrupamento", "hora"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Agrupamento deve ser dia ou mes."));
    }
    @Test @WithMockUser(roles = "GERENTE") void falhaDoBancoRetorna503SemDetalhesInternos() throws Exception {
        when(service.consultar(any(), any(), anyString(), isNull())).thenThrow(new DataAccessResourceFailureException("segredo de conexão"));
        mvc.perform(get("/api/dashboard/resumo")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Dados temporariamente indisponíveis. Tente atualizar novamente."));
    }
    @Test @WithMockUser(roles = "ADMIN") void padraoUsaMesAtualDeBrasilia() throws Exception {
        mvc.perform(get("/api/dashboard/resumo")).andExpect(status().isOk());
        LocalDate hoje = LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo"));
        verify(service).consultar(hoje.withDayOfMonth(1), hoje, "dia", null);
    }
}
