package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.service.PedidoService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    // Atendimento e gestão podem registrar novos pedidos.
    public Pedido criarPedido(@RequestBody Pedido pedido) {
        return pedidoService.criarPedido(pedido);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE', 'ENTREGADOR')")
    // Entregadores consultam os pedidos para acompanhar as entregas.
    public List<?> listarPedidos() {
        if (pedidoService.usuarioEhEntregador()) {
            return pedidoService.listarPedidosParaEntrega();
        }
        return pedidoService.listarPedidos();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE', 'ENTREGADOR')")
    // Atualiza somente o status do pedido identificado na URL.
    public Object atualizarStatus(@PathVariable String id, @RequestParam String status) {
        Pedido pedido = pedidoService.atualizarStatus(id, status);
        if (pedidoService.usuarioEhEntregador()) {
            return pedidoService.pedidoParaEntrega(pedido.getId());
        }
        return pedido;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    public Pedido atualizarPedido(@PathVariable String id, @RequestBody Pedido pedido) {
        return pedidoService.atualizarPedido(id, pedido);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Void> excluirPedido(@PathVariable String id) {
        pedidoService.excluirPedido(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelarPedido(@PathVariable String id, @RequestParam String justificativa) {
        return pedidoService.cancelarPedido(id, justificativa);
    }
}
