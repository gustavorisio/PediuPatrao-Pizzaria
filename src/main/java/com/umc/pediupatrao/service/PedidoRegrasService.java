package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PedidoRegrasService {

    private static final BigDecimal DESCONTO_MAXIMO = new BigDecimal("20");

    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;

    public void validarPedido(Pedido pedido) {
        validarEstrutura(pedido);
        validarCliente(pedido);
        validarItens(pedido);
        validarDesconto(pedido);
    }

    public String usuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authenticationInvalida(authentication)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação obrigatória.");
        }
        if (isBlank(authentication.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "É necessário estar autenticado para registrar um pedido.");
        }
        return authentication.getName();
    }

    public boolean podeAplicarDesconto() {
        return usuarioTemRole("GERENTE");
    }

    public void exigirGerente() {
        if (!podeAplicarDesconto()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente o GERENTE pode cancelar pedidos.");
        }
    }

    public void validarPercentual(BigDecimal percentual) {
        if (percentualInvalido(percentual)) {
            throw erro("O desconto deve estar entre 0% e 20%.");
        }
    }

    public void validarTransicao(StatusPedido statusAtual, StatusPedido novoStatus) {
        if (statusAusente(statusAtual, novoStatus)) {
            throw erro("O status do pedido deve ser informado.");
        }
        if (!transicaoPermitida(statusAtual, novoStatus)) {
            throw erro("Transição de status inválida.");
        }
    }

    public BigDecimal percentualDoPedido(Pedido pedido) {
        return pedido.getPercentualDesconto() == null
                ? BigDecimal.ZERO
                : pedido.getPercentualDesconto();
    }

    public ResponseStatusException erro(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }

    public boolean isBlank(String valor) {
        if (valor == null) {
            return true;
        }
        return valor.isBlank();
    }

    private void validarEstrutura(Pedido pedido) {
        if (pedido == null) {
            throw erro("O pedido deve possuir ao menos um item.");
        }
        if (pedido.getItens() == null) {
            throw erro("O pedido deve possuir ao menos um item.");
        }
        if (pedido.getItens().isEmpty()) {
            throw erro("O pedido deve possuir ao menos um item.");
        }
    }

    private void validarCliente(Pedido pedido) {
        if (isBlank(pedido.getClienteId())) {
            throw erro("Selecione um cliente cadastrado.");
        }
        if (!clienteRepository.existsById(pedido.getClienteId())) {
            throw erro("Selecione um cliente cadastrado.");
        }
    }

    private void validarItens(Pedido pedido) {
        for (ItemPedido item : pedido.getItens()) {
            if (item == null) {
                throw erro("Selecione um produto cadastrado para cada item.");
            }
            if (isBlank(item.getProdutoId())) {
                throw erro("Selecione um produto cadastrado para cada item.");
            }
            if (item.getQuantidade() <= 0) {
                throw erro("A quantidade de cada item deve ser maior que zero.");
            }
            Produto produto = produtoRepository.findById(item.getProdutoId())
                    .orElseThrow(() -> erro("Produto não encontrado."));
            if (precoInvalido(produto)) {
                throw erro("O produto selecionado não possui preço válido.");
            }
            item.setPrecoUnitario(BigDecimal.valueOf(produto.getPreco()));
            item.setDescricao(produto.getNome());
        }
    }

    private void validarDesconto(Pedido pedido) {
        BigDecimal desconto = pedido.getPercentualDesconto();
        if (desconto == null) {
            pedido.setPercentualDesconto(BigDecimal.ZERO);
            return;
        }
        validarPercentual(desconto);
        if (desconto.signum() > 0) {
            if (!podeAplicarDesconto()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Somente o GERENTE pode aplicar descontos.");
            }
        }
    }

    private boolean usuarioTemRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (("ROLE_" + role).equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private boolean authenticationInvalida(Authentication authentication) {
        if (authentication == null) {
            return true;
        }
        if (!authentication.isAuthenticated()) {
            return true;
        }
        return authentication instanceof AnonymousAuthenticationToken;
    }

    private boolean percentualInvalido(BigDecimal percentual) {
        if (percentual == null) {
            return true;
        }
        if (percentual.signum() < 0) {
            return true;
        }
        return percentual.compareTo(DESCONTO_MAXIMO) > 0;
    }

    private boolean statusAusente(StatusPedido atual, StatusPedido novo) {
        if (atual == null) {
            return true;
        }
        return novo == null;
    }

    private boolean precoInvalido(Produto produto) {
        if (produto.getPreco() == null) {
            return true;
        }
        if (!Double.isFinite(produto.getPreco())) {
            return true;
        }
        return produto.getPreco() < 0;
    }

    private boolean transicaoPermitida(StatusPedido atual, StatusPedido novo) {
        return switch (atual) {
            case RECEBIDO -> novo == StatusPedido.EM_PREPARACAO;
            case EM_PREPARACAO -> novo == StatusPedido.PRONTO;
            case PRONTO -> List.of(StatusPedido.SAIU_PARA_ENTREGA, StatusPedido.RETIRADO).contains(novo);
            case SAIU_PARA_ENTREGA, RETIRADO -> novo == StatusPedido.FINALIZADO;
            default -> false;
        };
    }
}
