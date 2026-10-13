package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    // Persiste um pedido recém-criado pelo atendimento.
    public Pedido criarPedido(Pedido pedido) {
        return pedidoRepository.save(pedido);
    }

    // Entrega à aplicação os pedidos necessários para acompanhamento.
    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    // Altera apenas o status, preservando os demais dados do pedido.
    public Pedido atualizarStatus(String id, String status) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow();
        pedido.setStatus(status);
        return pedidoRepository.save(pedido);
    }

}
