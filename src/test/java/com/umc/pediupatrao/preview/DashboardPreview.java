package com.umc.pediupatrao.preview;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.controller.*;
import com.umc.pediupatrao.entity.*;
import com.umc.pediupatrao.service.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Prévia isolada com dados fictícios, disponível apenas no classpath de teste. */
@Configuration
@EnableAutoConfiguration(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class, MongoRepositoriesAutoConfiguration.class})
@Import({SecurityConfig.class, DashboardController.class, DashboardErroHandler.class, ErroApiHandler.class, DashboardPreview.Pages.class})
public class DashboardPreview {
    public static void main(String[] args) {
        var app = new SpringApplication(DashboardPreview.class);
        app.setDefaultProperties(Map.of("server.port", "8081", "server.address", "127.0.0.1"));
        app.run(args);
    }
    @Bean UsuarioDetailsService usuarioDetailsService() {
        var service = mock(UsuarioDetailsService.class);
        when(service.loadUserByUsername("demo")).thenReturn(User.withUsername("demo")
                .password(new BCryptPasswordEncoder().encode("dashboard-demo")).roles("ADMIN").build());
        return service;
    }
    @Bean DashboardService dashboardService() {
        var mongo = mock(MongoTemplate.class);
        List<Pedido> pedidos = new ArrayList<>();
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        for (int i = 0; i < 42; i++) {
            Pedido p = new Pedido(); p.setId("demo-" + i); p.setNumeroPedido(1000 + i);
            p.setDataHoraEntrada(hoje.withDayOfMonth(1 + i % hoje.getDayOfMonth()).atTime(18 + i % 5, i % 60));
            p.setStatus(StatusPedido.values()[i % StatusPedido.values().length]);
            ItemPedido item = new ItemPedido(); item.setProdutoId("produto-" + i % 4);
            item.setDescricao(List.of("Pizza calabresa", "Pizza marguerita", "Refrigerante 2 L", "Pizza portuguesa").get(i % 4));
            item.setPrecoUnitario(new BigDecimal(i % 4 == 2 ? "14.00" : "49.90")); item.setQuantidade(1 + i % 3);
            p.setItens(List.of(item)); p.setValorTotal(item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())));
            pedidos.add(p);
        }
        when(mongo.find(any(Query.class), eq(Pedido.class))).thenAnswer(call -> {
            var query = ((Query) call.getArgument(0)).getQueryObject();
            var range = (org.bson.Document) query.get("dataHoraEntrada");
            LocalDateTime inicio = (LocalDateTime) range.get("$gte"), fim = (LocalDateTime) range.get("$lt");
            return pedidos.stream().filter(p -> !p.getDataHoraEntrada().isBefore(inicio) && p.getDataHoraEntrada().isBefore(fim))
                    .filter(p -> !query.containsKey("status") || p.getStatus() == query.get("status")).toList();
        });
        return new DashboardService(mongo);
    }
    @Controller static class Pages {
        @GetMapping("/login") String login() { return "login"; }
        @GetMapping("/") String home() { return "redirect:/dashboard"; }
        @GetMapping("/dashboard") String dashboard(Model model) {
            model.addAttribute("content", "dashboard :: content"); return "layout";
        }
    }
}
