package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.PedidoEntregaDTO;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.repository.ClienteRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PedidoEntregaService {

    private final ClienteRepository clienteRepository;

    public List<PedidoEntregaDTO> converterLista(List<Pedido> pedidos) {
        List<PedidoEntregaDTO> resultado = new ArrayList<>();
        for (Pedido pedido : pedidos) {
            resultado.add(converter(pedido));
        }
        return resultado;
    }

    public PedidoEntregaDTO converter(Pedido pedido) {
        Cliente cliente = clienteRepository.findById(pedido.getClienteId()).orElse(null);
        String endereco = cliente == null ? "-" : montarEndereco(cliente);
        return new PedidoEntregaDTO(numeroParaExibicao(pedido), endereco, pedido.getDataHoraEntrada());
    }

    private String numeroParaExibicao(Pedido pedido) {
        if (pedido.getNumeroPedido() != null) {
            return "Pedido " + pedido.getNumeroPedido();
        }
        return "Pedido " + pedido.getId();
    }

    private String montarEndereco(Cliente cliente) {
        StringBuilder endereco = new StringBuilder();
        adicionarParte(endereco, cliente.getLogradouro());
        adicionarParte(endereco, cliente.getNumero());
        adicionarParte(endereco, cliente.getComplemento());
        adicionarParte(endereco, cliente.getBairro());
        adicionarParte(endereco, cliente.getCidade());
        adicionarParte(endereco, cliente.getEstado());
        if (endereco.length() == 0) {
            return cliente.getEndereco() == null ? "-" : cliente.getEndereco();
        }
        return endereco.toString();
    }

    private void adicionarParte(StringBuilder endereco, String parte) {
        if (parte == null) {
            return;
        }
        if (parte.isBlank()) {
            return;
        }
        if (endereco.length() > 0) {
            endereco.append(", ");
        }
        endereco.append(parte);
    }
}
