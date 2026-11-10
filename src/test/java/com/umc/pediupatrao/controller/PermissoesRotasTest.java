package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({HomeController.class, AuditoriaController.class, EntregadorController.class,
        PedidoController.class, ProdutoController.class, ClienteController.class, UsuarioController.class})
@Import(SecurityConfig.class)
class PermissoesRotasTest {
    @Autowired MockMvc mvc;
    @MockitoBean AuditoriaService auditoria;
    @MockitoBean EntregadorService entregadores;
    @MockitoBean PedidoService pedidos;
    @MockitoBean ProdutoService produtos;
    @MockitoBean ClienteService clientes;
    @MockitoBean(name = "usuarioService") UsuarioService usuarios;
    @MockitoBean UsuarioDetailsService details;

    @ParameterizedTest
    @CsvSource({
        "ADMIN,/api/auditorias,200", "GERENTE,/api/auditorias,200", "ATENDENTE,/api/auditorias,403",
        "ADMIN,/auditorias,200", "GERENTE,/auditorias,200", "ATENDENTE,/auditorias,403",
        "ATENDENTE,/api/entregadores,200", "GERENTE,/api/entregadores,200", "ADMIN,/api/entregadores,200",
        "ATENDENTE,/entregadores,403", "ATENDENTE,/funcionarios,403", "GERENTE,/funcionarios,200",
        "GERENTE,/usuarios,403", "ATENDENTE,/usuarios,403", "GERENTE,/api/usuarios,403",
        "ATENDENTE,/api/produtos,200", "ATENDENTE,/api/clientes,200", "ADMIN,/api/pedidos,200",
        "ATENDENTE,/produtos/editar/1,403", "GERENTE,/admin,403", "DESCONHECIDO,/pedidos,403",
        "ADMIN,/rota-nao-autorizada,403", "ATENDENTE,/teste,403"
    })
    void verificaConsultasPorPerfil(String role, String rota, int expected) throws Exception {
        mvc.perform(get(rota).with(user("usuario").roles(role))).andExpect(status().is(expected));
    }

    @ParameterizedTest
    @CsvSource({
        "ATENDENTE,/api/entregadores", "ATENDENTE,/api/produtos", "GERENTE,/api/usuarios",
        "ADMIN,/api/pedidos", "ATENDENTE,/produtos/salvar", "ATENDENTE,/entregadores/salvar",
        "GERENTE,/usuarios/salvar"
    })
    void bloqueiaEscritasSemPermissaoMesmoComCsrf(String role, String rota) throws Exception {
        mvc.perform(post(rota).with(user("usuario").roles(role)).with(csrf())
                .contentType("application/json").content("{}")) .andExpect(status().isForbidden());
        verifyNoInteractions(entregadores, produtos, pedidos, usuarios);
    }

    @ParameterizedTest
    @CsvSource({"ADMIN,cancelar", "ATENDENTE,cancelar", "ADMIN,desconto", "ATENDENTE,desconto"})
    void somenteGerentePodeCancelarOuDarDesconto(String role, String operacao) throws Exception {
        mvc.perform(put("/api/pedidos/1/" + operacao).param("justificativa", "Solicitação do cliente")
                .param("percentual", "10").with(user("usuario").roles(role)).with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(pedidos);
    }

    @Test void gerentePodeCancelarEDarDesconto() throws Exception {
        mvc.perform(put("/api/pedidos/1/cancelar").param("justificativa", "Cliente desistiu")
                .with(user("gerente").roles("GERENTE")).with(csrf())).andExpect(status().isOk());
        mvc.perform(put("/api/pedidos/1/desconto").param("percentual", "10")
                .with(user("gerente").roles("GERENTE")).with(csrf())).andExpect(status().isOk());
        verify(pedidos).cancelarPedido("1", "Cliente desistiu");
        verify(pedidos).aplicarDesconto("1", new java.math.BigDecimal("10"));
    }

    @Test void csrfContinuaObrigatorioMesmoParaGerente() throws Exception {
        mvc.perform(put("/api/pedidos/1/cancelar").param("justificativa", "Teste")
                .with(user("gerente").roles("GERENTE"))).andExpect(status().isForbidden());
        verifyNoInteractions(pedidos);
    }

    @Test void atendenteNaoExcluiCliente() throws Exception {
        mvc.perform(delete("/api/clientes/1").with(user("atendente").roles("ATENDENTE")).with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(clientes);
    }

    @Test void auditoriaNaoPossuiOperacoesDeAlteracaoOuExclusao() throws Exception {
        mvc.perform(delete("/api/auditorias/1").with(user("gerente").roles("GERENTE")).with(csrf()))
                .andExpect(status().isNotFound());
        verifyNoInteractions(auditoria);
    }

    @Test void usuarioSoPodeEditarContaPropria() throws Exception {
        when(usuarios.isCurrentUser("proprio")).thenReturn(true);
        mvc.perform(put("/api/usuarios/proprio").with(user("atendente").roles("ATENDENTE")).with(csrf())
                .contentType("application/json").content("{\"username\":\"atendente\"}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/usuarios/alheio").with(user("atendente").roles("ATENDENTE")).with(csrf())
                .contentType("application/json").content("{\"username\":\"outro\"}"))
                .andExpect(status().isForbidden());
        verify(usuarios).atualizarUsuario(eq("proprio"), any());
        verify(usuarios, never()).atualizarUsuario(eq("alheio"), any());
    }

    @Test void apiSemSessaoRetorna401() throws Exception {
        mvc.perform(get("/api/auditorias")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").exists());
    }
}
