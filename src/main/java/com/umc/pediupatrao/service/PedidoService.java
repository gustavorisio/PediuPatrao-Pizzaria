package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.dto.PedidoEntregaDTO;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import org.springframework.security.core.GrantedAuthority;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private static final BigDecimal DESCONTO_MAXIMO = new BigDecimal("20");

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final PedidoAuditoriaService pedidoAuditoriaService;
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public Pedido criarPedido(Pedido pedido) {
        validarPedido(pedido);
        // Ignora o ID enviado para não sobrescrever outro pedido.
        pedido.setId(null);
        pedido.gerarId();
        pedido.setNumeroPedido(proximoNumeroPedido());
        pedido.setStatus(StatusPedido.RECEBIDO);
        pedido.setValorTotal(calcularValorTotal(pedido));
        pedido.setResponsavelRegistro(usuarioAutenticado());
        pedido.setDataHoraEntrada(LocalDateTime.now());
        pedido.setResponsavelSaida(null);
        pedido.setDataHoraSaida(null);
        pedido.setJustificativaCancelamento(null);
        Pedido salvo = pedidoRepository.save(pedido);
        pedidoAuditoriaService.registrar("INCLUSAO", salvo, Map.of(), null);
        return salvo;
    }

    private void validarPedido(Pedido pedido) {
        if (pedido == null) {
            throw erro("O pedido deve possuir ao menos um item.");
        }
        if (pedido.getItens() == null) {
            throw erro("O pedido deve possuir ao menos um item.");
        }
        if (pedido.getItens().isEmpty()) {
            throw erro("O pedido deve possuir ao menos um item.");
        }
        if (isBlank(pedido.getClienteId())) {
            throw erro("Selecione um cliente cadastrado.");
        }
        if (!clienteRepository.existsById(pedido.getClienteId())) {
            throw erro("Selecione um cliente cadastrado.");
        }

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
            if (produto.getPreco() == null) {
                throw erro("O produto selecionado não possui preço válido.");
            }
            if (!Double.isFinite(produto.getPreco())) {
                throw erro("O produto selecionado não possui preço válido.");
            }
            if (produto.getPreco() < 0) {
                throw erro("O produto selecionado não possui preço válido.");
            }
            // O preço vem do cadastro do produto, não do navegador.
            item.setPrecoUnitario(BigDecimal.valueOf(produto.getPreco()));
            item.setDescricao(produto.getNome());
        }

        BigDecimal desconto = pedido.getPercentualDesconto();
        if (desconto == null) {
            pedido.setPercentualDesconto(BigDecimal.ZERO);
        } else {
            validarPercentual(desconto);
            if (desconto.signum() > 0) {
                if (!podeAplicarDesconto()) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Somente o GERENTE pode aplicar descontos.");
                }
            }
        }
    }

    private BigDecimal calcularValorTotal(Pedido pedido) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedido item : pedido.getItens()) {
            BigDecimal valorItem = item.getPrecoUnitario()
                    .multiply(BigDecimal.valueOf(item.getQuantidade()));
            subtotal = subtotal.add(valorItem);
        }
        BigDecimal fatorDesconto = BigDecimal.ONE.subtract(
                pedido.getPercentualDesconto().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        return subtotal.multiply(fatorDesconto).setScale(2, RoundingMode.HALF_UP);
    }

    private String usuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação obrigatória.");
        }
        if (!authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação obrigatória.");
        }
        if (authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação obrigatória.");
        }
        if (isBlank(authentication.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "É necessário estar autenticado para registrar um pedido.");
        }
        return authentication.getName();
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

    private boolean podeAplicarDesconto() {
        return usuarioTemRole("GERENTE");
    }

    public boolean usuarioEhEntregador() {
        return usuarioTemRole("ENTREGADOR");
    }

    private ResponseStatusException erro(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }

    private boolean isBlank(String valor) {
        if (valor == null) {
            return true;
        }
        return valor.isBlank();
    }
    // Entrega à aplicação os pedidos necessários para acompanhamento.
    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    public List<PedidoEntregaDTO> listarPedidosParaEntrega() {
        List<Pedido> pedidos = pedidoRepository.findAll();
        List<PedidoEntregaDTO> resultado = new ArrayList<>();
        for (Pedido pedido : pedidos) {
            resultado.add(transformarParaEntrega(pedido));
        }
        return resultado;
    }

    public PedidoEntregaDTO pedidoParaEntrega(String id) {
        return transformarParaEntrega(buscarPedido(id));
    }

    private PedidoEntregaDTO transformarParaEntrega(Pedido pedido) {
        // O entregador recebe só o número, o endereço e a data de entrada.
        Cliente cliente = clienteRepository.findById(pedido.getClienteId()).orElse(null);
        if (cliente == null) {
            return new PedidoEntregaDTO(numeroParaExibicao(pedido), "-",
                    pedido.getDataHoraEntrada());
        }
        return new PedidoEntregaDTO(numeroParaExibicao(pedido), enderecoCliente(cliente),
                pedido.getDataHoraEntrada());
    }

    private String numeroParaExibicao(Pedido pedido) {
        if (pedido.getNumeroPedido() != null) {
            return "Pedido " + pedido.getNumeroPedido();
        }
        return "Pedido " + pedido.getId();
    }

    private int proximoNumeroPedido() {
        return pedidoRepository.findTopByOrderByNumeroPedidoDesc()
                .map(pedido -> pedido.getNumeroPedido() == null
                        ? 1
                        : pedido.getNumeroPedido() + 1)
                .orElse(1);
    }

    private String enderecoCliente(com.umc.pediupatrao.entity.Cliente cliente) {
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
        if (!isBlank(parte)) {
            if (endereco.length() > 0) {
                endereco.append(", ");
            }
            endereco.append(parte);
        }
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public Pedido atualizarPedido(String id, Pedido dados) {
        if (dados == null) {
            throw erro("Os dados do pedido devem ser informados.");
        }
        Pedido pedido = buscarPedido(id);
        if (pedido.getStatus() != StatusPedido.RECEBIDO) {
            throw erro("Somente pedidos recebidos podem ser editados.");
        }
        if (!podeAplicarDesconto()) {
            if (dados.getPercentualDesconto() != null) {
                if (dados.getPercentualDesconto().compareTo(percentualDoPedido(pedido)) != 0) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Somente o GERENTE pode alterar descontos.");
                }
            }
            // Valida os itens sem desconto novo e depois mantém o desconto já autorizado.
            dados.setPercentualDesconto(BigDecimal.ZERO);
            validarPedido(dados);
            dados.setPercentualDesconto(percentualDoPedido(pedido));
        } else {
            validarPedido(dados);
        }
        // Guarda os valores antes dos setters para comparar na auditoria.
        Map<String, String> dadosAnteriores = pedidoAuditoriaService.dadosPedido(pedido);
        pedido.setClienteId(dados.getClienteId());
        pedido.setTipoPizza(dados.getTipoPizza());
        pedido.setQuantidade(dados.getQuantidade());
        pedido.setSabor(dados.getSabor());
        pedido.setItens(dados.getItens());
        BigDecimal descontoAnterior = percentualDoPedido(pedido);
        pedido.setPercentualDesconto(dados.getPercentualDesconto());
        pedido.setValorTotal(calcularValorTotal(pedido));
        Pedido salvo = pedidoRepository.save(pedido);
        pedidoAuditoriaService.registrar(descontoAnterior.compareTo(pedido.getPercentualDesconto()) == 0 ? "ALTERACAO" : "DESCONTO", salvo, dadosAnteriores, null);
        return salvo;
    }

    public void excluirPedido(String id) {
        throw erro("Pedidos devem ser cancelados com justificativa; o histórico é preservado.");
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(String id, BigDecimal percentual) {
        Pedido pedido = buscarPedido(id);
        if (!List.of(StatusPedido.RECEBIDO, StatusPedido.EM_PREPARACAO, StatusPedido.PRONTO)
                .contains(pedido.getStatus())) {
            throw erro("Descontos só podem ser alterados antes da saída do pedido.");
        }
        validarPercentual(percentual);
        Map<String, String> antes = pedidoAuditoriaService.dadosPedido(pedido);
        pedido.setPercentualDesconto(percentual);
        pedido.setValorTotal(calcularValorTotal(pedido));
        Pedido salvo = pedidoRepository.save(pedido);
        pedidoAuditoriaService.registrar("DESCONTO", salvo, antes, null);
        return salvo;
    }
    // Altera apenas o status, preservando os demais dados do pedido.
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE', 'ENTREGADOR')")
    public Pedido atualizarStatus(String id, String status) {
        Pedido pedido = buscarPedido(id);
        StatusPedido novoStatus = StatusPedido.fromValue(status);
        validarTransicao(pedido.getStatus(), novoStatus);

        Map<String, String> antes = pedidoAuditoriaService.dadosPedido(pedido);

        String usuario = usuarioAutenticado();
        pedido.setStatus(novoStatus);
        // A retirada no balcão também conta como saída do pedido.
        if (List.of(StatusPedido.SAIU_PARA_ENTREGA, StatusPedido.RETIRADO).contains(novoStatus)) {
            pedido.setResponsavelSaida(usuario);
            pedido.setDataHoraSaida(LocalDateTime.now());
        }
        Pedido salvo = pedidoRepository.save(pedido);
        pedidoAuditoriaService.registrar("ALTERACAO_STATUS", salvo, antes, null);
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelarPedido(String id, String justificativa) {
        Pedido pedido = buscarPedido(id);
        exigirGerente();
        if (isBlank(justificativa)) {
            throw erro("A justificativa do cancelamento é obrigatória.");
        }
        if (!List.of(StatusPedido.RECEBIDO, StatusPedido.EM_PREPARACAO, StatusPedido.PRONTO).contains(pedido.getStatus())) {
            throw erro("O pedido só pode ser cancelado antes da saída para entrega ou retirada.");
        }


        Map<String, String> antes = pedidoAuditoriaService.dadosPedido(pedido);
        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setJustificativaCancelamento(justificativa.trim());
        Pedido salvo = pedidoRepository.save(pedido);
        pedidoAuditoriaService.registrar("CANCELAMENTO", salvo, antes, pedido.getJustificativaCancelamento());
        return salvo;
    }

    private Pedido buscarPedido(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));
    }

    private void validarTransicao(StatusPedido statusAtual, StatusPedido novoStatus) {
        if (statusAtual == null) {
            throw erro("O status do pedido deve ser informado.");
        }
        if (novoStatus == null) {
            throw erro("O status do pedido deve ser informado.");
        }
        if (usuarioEhEntregador()) {
            boolean permitido = false;
            if (statusAtual == StatusPedido.PRONTO) {
                permitido = novoStatus == StatusPedido.SAIU_PARA_ENTREGA;
            }
            if (statusAtual == StatusPedido.SAIU_PARA_ENTREGA) {
                permitido = novoStatus == StatusPedido.FINALIZADO;
            }
            if (statusAtual == StatusPedido.RETIRADO) {
                permitido = novoStatus == StatusPedido.FINALIZADO;
            }
            if (!permitido) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "O entregador só pode atualizar o status da entrega.");
            }
        }
        boolean valida = false;
        switch (statusAtual) {
            case RECEBIDO:
                valida = novoStatus == StatusPedido.EM_PREPARACAO;
                break;
            case EM_PREPARACAO:
                valida = novoStatus == StatusPedido.PRONTO;
                break;
            case PRONTO:
                if (novoStatus == StatusPedido.SAIU_PARA_ENTREGA) {
                    valida = true;
                }
                if (novoStatus == StatusPedido.RETIRADO) {
                    valida = true;
                }
                break;
            case SAIU_PARA_ENTREGA:
            case RETIRADO:
                valida = novoStatus == StatusPedido.FINALIZADO;
                break;
            default:
                // Pedidos finalizados ou cancelados não mudam mais de status.
                break;
        }
        if (!valida) {
            throw erro("Transição de status inválida.");
        }
    }
    private void exigirGerente() {
        if (!usuarioTemRole("GERENTE")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente o GERENTE pode cancelar pedidos.");
        }
    }

    private void validarPercentual(BigDecimal percentual) {
        if (percentual == null) {
            throw erro("O desconto deve estar entre 0% e 20%.");
        }
        if (percentual.signum() < 0) {
            throw erro("O desconto deve estar entre 0% e 20%.");
        }
        if (percentual.compareTo(DESCONTO_MAXIMO) > 0) {
            throw erro("O desconto deve estar entre 0% e 20%.");
        }
    }

    private BigDecimal percentualDoPedido(Pedido pedido) {
        return pedido.getPercentualDesconto() == null
                ? BigDecimal.ZERO
                : pedido.getPercentualDesconto();
    }

}
