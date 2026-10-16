package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class PedidoCalculoService {

    public BigDecimal calcularValorTotal(Pedido pedido) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedido item : pedido.getItens()) {
            BigDecimal valorItem = item.getPrecoUnitario()
                    .multiply(BigDecimal.valueOf(item.getQuantidade()));
            subtotal = subtotal.add(valorItem);
        }

        BigDecimal fatorDesconto = BigDecimal.ONE.subtract(
                percentualDoPedido(pedido).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        return subtotal.multiply(fatorDesconto).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentualDoPedido(Pedido pedido) {
        return pedido.getPercentualDesconto() == null
                ? BigDecimal.ZERO
                : pedido.getPercentualDesconto();
    }
}
