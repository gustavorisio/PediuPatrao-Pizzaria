package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final MongoTemplate mongo;
    public DashboardService(MongoTemplate mongo) { this.mongo = mongo; }

    public record Indicadores(long pedidos, long vendas, long cancelados, long emAndamento,
                              BigDecimal faturamento, BigDecimal ticketMedio) {}
    public record Venda(String periodo, long pedidos, BigDecimal faturamento) {}
    public record Situacao(StatusPedido status, long quantidade) {}
    public record ProdutoVendido(String produtoId, String descricao, long quantidade, BigDecimal valorBruto) {}
    public record PedidoResumo(String id, Integer numeroPedido, LocalDateTime entrada,
                               StatusPedido status, BigDecimal valorTotal) {}
    public record Resumo(LocalDate inicio, LocalDate fim, String agrupamento, StatusPedido status,
                         LocalDateTime atualizadoEm, Indicadores indicadores, List<Venda> vendas,
                         List<Situacao> situacoes, List<ProdutoVendido> produtos, List<PedidoResumo> pedidosRecentes) {}

    public Resumo consultar(LocalDate inicio, LocalDate fim, String agrupamento, StatusPedido status) {
        validar(inicio, fim, agrupamento);
        Criteria criterio = Criteria.where("dataHoraEntrada").gte(inicio.atStartOfDay())
                .lt(fim.plusDays(1).atStartOfDay());
        if (status != null) criterio.and("status").is(status);
        Query query = Query.query(criterio);
        query.fields().include("numeroPedido").include("dataHoraEntrada").include("status")
                .include("valorTotal").include("itens");
        return calcular(inicio, fim, agrupamento, status, mongo.find(query, Pedido.class));
    }

    private void validar(LocalDate inicio, LocalDate fim, String agrupamento) {
        if (inicio == null || fim == null || fim.isBefore(inicio)
                || ChronoUnit.DAYS.between(inicio, fim) > 365) {
            throw new IllegalArgumentException("Informe um período de 1 a 366 dias, com início anterior ou igual ao fim.");
        }
        if (!Set.of("dia", "mes").contains(agrupamento))
            throw new IllegalArgumentException("Agrupamento deve ser dia ou mes.");
    }

    // Uma única leitura alimenta todos os painéis, mantendo os totais consistentes.
    public Resumo calcular(LocalDate inicio, LocalDate fim, String agrupamento, StatusPedido status, List<Pedido> pedidos) {
        validar(inicio, fim, agrupamento);
        Map<String, List<Pedido>> porPeriodo = new LinkedHashMap<>();
        for (LocalDate dia = inicio; !dia.isAfter(fim); dia = dia.plusDays(1))
            porPeriodo.putIfAbsent(chave(dia, agrupamento), new ArrayList<>());
        Map<StatusPedido, Long> situacoes = new EnumMap<>(StatusPedido.class);
        for (StatusPedido s : StatusPedido.values()) situacoes.put(s, 0L);
        Map<String, ProdutoVendido> produtos = new HashMap<>();
        BigDecimal receita = BigDecimal.ZERO;
        long vendas = 0, cancelados = 0, abertos = 0;
        for (Pedido p : pedidos) {
            if (p.getStatus() != null) situacoes.merge(p.getStatus(), 1L, Long::sum);
            if (p.getStatus() == StatusPedido.CANCELADO) { cancelados++; continue; }
            if (p.getStatus() != StatusPedido.FINALIZADO) abertos++;
            vendas++;
            receita = receita.add(valor(p));
            porPeriodo.get(chave(p.getDataHoraEntrada().toLocalDate(), agrupamento)).add(p);
            if (p.getItens() == null) continue; // Pedidos legados sem itens não entram no ranking.
            for (ItemPedido item : p.getItens()) {
                if (item == null || item.getProdutoId() == null || item.getQuantidade() <= 0 || item.getPrecoUnitario() == null) continue;
                ProdutoVendido anterior = produtos.get(item.getProdutoId());
                long quantidade = item.getQuantidade() + (anterior == null ? 0 : anterior.quantidade());
                BigDecimal bruto = item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade()))
                        .add(anterior == null ? BigDecimal.ZERO : anterior.valorBruto());
                produtos.put(item.getProdutoId(), new ProdutoVendido(item.getProdutoId(),
                        item.getDescricao() == null ? "Produto sem descrição" : item.getDescricao(), quantidade, dinheiro(bruto)));
            }
        }
        BigDecimal ticket = vendas == 0 ? dinheiro(BigDecimal.ZERO) : receita.divide(BigDecimal.valueOf(vendas), 2, RoundingMode.HALF_UP);
        return new Resumo(inicio, fim, agrupamento, status, LocalDateTime.now(ZoneId.of("America/Sao_Paulo")),
                new Indicadores(pedidos.size(), vendas, cancelados, abertos, dinheiro(receita), ticket),
                porPeriodo.entrySet().stream().map(e -> new Venda(e.getKey(), e.getValue().size(),
                        dinheiro(e.getValue().stream().map(this::valor).reduce(BigDecimal.ZERO, BigDecimal::add)))).toList(),
                situacoes.entrySet().stream().map(e -> new Situacao(e.getKey(), e.getValue())).toList(),
                produtos.values().stream().sorted(Comparator.comparingLong(ProdutoVendido::quantidade).reversed()
                        .thenComparing(ProdutoVendido::produtoId)).limit(10).toList(),
                pedidos.stream().sorted(Comparator.comparing(Pedido::getDataHoraEntrada).reversed()
                        .thenComparing(p -> Objects.toString(p.getId(), ""))).limit(20)
                        .map(p -> new PedidoResumo(p.getId(), p.getNumeroPedido(), p.getDataHoraEntrada(), p.getStatus(), dinheiro(valor(p)))).toList());
    }

    private String chave(LocalDate data, String agrupamento) {
        return "mes".equals(agrupamento) ? YearMonth.from(data).toString() : data.toString();
    }
    private BigDecimal valor(Pedido pedido) { return pedido.getValorTotal() == null ? BigDecimal.ZERO : pedido.getValorTotal(); }
    private BigDecimal dinheiro(BigDecimal valor) { return valor.setScale(2, RoundingMode.HALF_UP); }
}
