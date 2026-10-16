package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.ItemPedido;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PedidoAuditoriaService {
    private final AuditoriaService auditoriaService;

    public void registrar(String operacao, Pedido pedido, Map<String, String> antes, String justificativa) {
        auditoriaService.registrar(operacao, "PEDIDO", pedido.getId(), antes, dadosPedido(pedido), justificativa);
    }

    public Map<String, String> dadosPedido(Pedido pedido) {
        Map<String, String> dados = new HashMap<>();
        dados.put("status", String.valueOf(pedido.getStatus()));
        dados.put("percentualDesconto", String.valueOf(pedido.getPercentualDesconto()));
        dados.put("valorTotal", String.valueOf(pedido.getValorTotal()));
        dados.put("clienteId", pedido.getClienteId());
        dados.put("responsavelRegistro", pedido.getResponsavelRegistro());
        dados.put("dataHoraEntrada", String.valueOf(pedido.getDataHoraEntrada()));
        dados.put("responsavelSaida", pedido.getResponsavelSaida());
        dados.put("dataHoraSaida", pedido.getDataHoraSaida() == null ? null : pedido.getDataHoraSaida().toString());
        dados.put("justificativaCancelamento", pedido.getJustificativaCancelamento());
        if (pedido.getItens() != null) {
            for (int i = 0; i < pedido.getItens().size(); i++) {
                ItemPedido item = pedido.getItens().get(i);
                String campo = "itens[" + i + "]/";
                dados.put(campo + "produtoId", item.getProdutoId());
                dados.put(campo + "descricao", item.getDescricao());
                dados.put(campo + "quantidade", String.valueOf(item.getQuantidade()));
                dados.put(campo + "precoUnitario", String.valueOf(item.getPrecoUnitario()));
            }
        }
        return dados;
    }
}
