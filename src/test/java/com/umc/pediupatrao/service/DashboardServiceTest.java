package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DashboardServiceTest {
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final DashboardService service = new DashboardService(mongo);
    private final LocalDate inicio = LocalDate.of(2026, 11, 1);
    private Pedido pedido(String id, StatusPedido status, String total, int dia, int quantidade) {
        Pedido p = new Pedido(); p.setId(id); p.setStatus(status); p.setValorTotal(new BigDecimal(total));
        p.setDataHoraEntrada(inicio.withDayOfMonth(dia).atTime(12, 0));
        ItemPedido item = new ItemPedido(); item.setProdutoId("pizza"); item.setDescricao("Pizza calabresa");
        item.setQuantidade(quantidade); item.setPrecoUnitario(new BigDecimal("25.00"));
        p.setItens(List.of(item)); return p;
    }
    @Test void calculaReceitaLiquidaTicketRankingESituacoesSemCancelados() {
        var result = service.calcular(inicio, inicio.plusDays(2), "dia", null, List.of(
                pedido("1", StatusPedido.FINALIZADO, "45", 1, 2),
                pedido("2", StatusPedido.RECEBIDO, "25", 3, 1),
                pedido("3", StatusPedido.CANCELADO, "100", 1, 4)));
        assertThat(result.indicadores().pedidos()).isEqualTo(3);
        assertThat(result.indicadores().vendas()).isEqualTo(2);
        assertThat(result.indicadores().cancelados()).isEqualTo(1);
        assertThat(result.indicadores().emAndamento()).isEqualTo(1);
        assertThat(result.indicadores().faturamento()).isEqualByComparingTo("70.00");
        assertThat(result.indicadores().ticketMedio()).isEqualByComparingTo("35.00");
        assertThat(result.produtos().getFirst().quantidade()).isEqualTo(3);
        assertThat(result.produtos().getFirst().valorBruto()).isEqualByComparingTo("75.00");
        assertThat(result.vendas().get(1).faturamento()).isEqualByComparingTo("0.00");
        assertThat(result.vendas()).hasSize(3);
        assertThat(result.situacoes()).hasSize(7);
        assertThat(result.pedidosRecentes().getFirst().id()).isEqualTo("2");
    }
    @Test void periodoVazioRetornaZerosESerieCompleta() {
        var result = service.calcular(inicio, inicio.plusDays(4), "dia", null, List.of());
        assertThat(result.indicadores().pedidos()).isZero();
        assertThat(result.indicadores().ticketMedio()).isEqualByComparingTo("0");
        assertThat(result.vendas()).hasSize(5).allMatch(v -> v.pedidos() == 0);
        assertThat(result.produtos()).isEmpty();
        assertThat(result.pedidosRecentes()).isEmpty();
    }
    @Test void agrupaPorMesIncluindoMesSemMovimento() {
        var result = service.calcular(inicio, LocalDate.of(2027, 1, 1), "mes", null,
                List.of(pedido("1", StatusPedido.FINALIZADO, "10", 1, 1), pedido("2", StatusPedido.FINALIZADO, "20", 30, 1)));
        assertThat(result.vendas()).hasSize(3);
        assertThat(result.vendas().getFirst().periodo()).isEqualTo("2026-11");
        assertThat(result.vendas().getFirst().faturamento()).isEqualByComparingTo("30");
        assertThat(result.vendas().get(1).pedidos()).isZero();
    }
    @Test void validaDatasEAgrupamentoAntesDeConsultarBanco() {
        assertThatThrownBy(() -> service.consultar(inicio, inicio.minusDays(1), "dia", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.consultar(inicio, inicio.plusDays(366), "dia", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.consultar(inicio, inicio, "hora", null)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(mongo);
    }
    @Test void consultaMongoComLimitesInclusivoExclusivoESituacao() {
        when(mongo.find(any(Query.class), eq(Pedido.class))).thenReturn(List.of());
        service.consultar(inicio, inicio.plusDays(1), "dia", StatusPedido.FINALIZADO);
        var captor = ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(captor.capture(), eq(Pedido.class));
        var query = captor.getValue().getQueryObject();
        var range = (org.bson.Document) query.get("dataHoraEntrada");
        assertThat(range.get("$gte")).isEqualTo(inicio.atStartOfDay());
        assertThat(range.get("$lt")).isEqualTo(inicio.plusDays(2).atStartOfDay());
        assertThat(query.get("status")).isEqualTo(StatusPedido.FINALIZADO);
        assertThat(captor.getValue().getFieldsObject()).doesNotContainKey("clienteId");
    }
    @Test void limitaRankingEListaDePedidosEOrdenaPorQuantidade() {
        List<Pedido> pedidos = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            var p = pedido("id" + i, StatusPedido.FINALIZADO, "25", i, i);
            p.getItens().getFirst().setProdutoId("produto" + i); pedidos.add(p);
        }
        var result = service.calcular(inicio, inicio.withDayOfMonth(30), "dia", null, pedidos);
        assertThat(result.produtos()).hasSize(10);
        assertThat(result.produtos().getFirst().quantidade()).isEqualTo(25);
        assertThat(result.pedidosRecentes()).hasSize(20);
        assertThat(result.pedidosRecentes().getFirst().id()).isEqualTo("id25");
    }
    @Test void trataPedidosLegadosSemItensOuValor() {
        var p = pedido("1", StatusPedido.RECEBIDO, "0", 1, 1);
        p.setItens(null); p.setValorTotal(null);
        var result = service.calcular(inicio, inicio, "dia", null, List.of(p));
        assertThat(result.indicadores().faturamento()).isEqualByComparingTo("0");
        assertThat(result.produtos()).isEmpty();
    }
    @Test void aceitaLimiteDe366DiasEDiaUnico() {
        assertThat(service.calcular(inicio, inicio.plusDays(365), "dia", null, List.of()).vendas()).hasSize(366);
        assertThat(service.calcular(inicio, inicio, "dia", null, List.of()).vendas()).hasSize(1);
    }
}
