package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PedidoService {

    private static final BigDecimal DESCONTO_MAXIMO = new BigDecimal("20");

    private final PedidoRepository pedidoRepository;

    public PedidoService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    // Persiste um pedido recém-criado pelo atendimento.
    public Pedido criarPedido(Pedido pedido) {
        validarPedido(pedido);
        pedido.setStatus("RECEBIDO");
        pedido.setValorTotal(calcularValorTotal(pedido));
        pedido.setResponsavelRegistro(usuarioAutenticado());
        pedido.setDataHoraEntrada(LocalDateTime.now());
        return pedidoRepository.save(pedido);
    }

    private void validarPedido(Pedido pedido) {
        if (pedido == null || pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw erro("O pedido deve possuir ao menos um item.");
        }

        for (var item : pedido.getItens()) {
            if (item == null
                    || (isBlank(item.getProdutoId()) && isBlank(item.getDescricao()))) {
                throw erro("Cada item deve informar um produto ou uma descrição.");
            }
            if (item.getQuantidade() <= 0) {
                throw erro("A quantidade de cada item deve ser maior que zero.");
            }
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) < 0) {
                throw erro("O preço unitário de cada item deve ser informado e não pode ser negativo.");
            }
        }

        BigDecimal desconto = pedido.getPercentualDesconto();
        if (desconto == null) {
            pedido.setPercentualDesconto(BigDecimal.ZERO);
        } else if (desconto.compareTo(BigDecimal.ZERO) < 0
                || desconto.compareTo(DESCONTO_MAXIMO) > 0) {
            throw erro("O desconto deve estar entre 0% e 20%.");
        } else if (desconto.compareTo(BigDecimal.ZERO) > 0 && !usuarioTemRole("GERENTE")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Somente o GERENTE pode aplicar descontos.");
        }
    }

    private BigDecimal calcularValorTotal(Pedido pedido) {
        BigDecimal subtotal = pedido.getItens().stream()
                .map(item -> item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal fatorDesconto = BigDecimal.ONE.subtract(
                pedido.getPercentualDesconto().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        return subtotal.multiply(fatorDesconto).setScale(2, RoundingMode.HALF_UP);
    }

    private String usuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "É necessário estar autenticado para registrar um pedido.");
        }
        return authentication.getName();
    }

    private boolean usuarioTemRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> ("ROLE_" + role).equals(authority.getAuthority()));
    }

    private ResponseStatusException erro(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }

    private boolean isBlank(String valor) {
        return valor == null || valor.isBlank();
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
