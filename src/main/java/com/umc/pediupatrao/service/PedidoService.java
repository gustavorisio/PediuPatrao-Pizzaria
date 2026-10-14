package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.PedidoAuditoria;
import com.umc.pediupatrao.dto.PedidoEntregaDTO;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
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
import java.util.ArrayList;
import java.util.Locale;

@Service
public class PedidoService {

    private static final BigDecimal DESCONTO_MAXIMO = new BigDecimal("20");

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;

    public PedidoService(PedidoRepository pedidoRepository, ClienteRepository clienteRepository,
            ProdutoRepository produtoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
    }

    // Persiste um pedido recém-criado pelo atendimento.
    public Pedido criarPedido(Pedido pedido) {
        validarPedido(pedido);
        pedido.setStatus("RECEBIDO");
        pedido.setValorTotal(calcularValorTotal(pedido));
        pedido.setResponsavelRegistro(usuarioAutenticado());
        pedido.setDataHoraEntrada(LocalDateTime.now());
        pedido.setResponsavelSaida(null);
        pedido.setDataHoraSaida(null);
        pedido.setJustificativaCancelamento(null);
        pedido.setAuditoria(new ArrayList<>());
        registrarAuditoria(pedido, "RECEBIMENTO", null, pedido.getStatus(), null,
                BigDecimal.ZERO, pedido.getPercentualDesconto(), null, pedido.getValorTotal());
        return pedidoRepository.save(pedido);
    }

    private void validarPedido(Pedido pedido) {
        if (pedido == null || pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw erro("O pedido deve possuir ao menos um item.");
        }
        if (isBlank(pedido.getClienteId()) || !clienteRepository.existsById(pedido.getClienteId())) {
            throw erro("Selecione um cliente cadastrado.");
        }

        for (var item : pedido.getItens()) {
            if (item == null || isBlank(item.getProdutoId())) {
                throw erro("Selecione um produto cadastrado para cada item.");
            }
            if (item.getQuantidade() <= 0) {
                throw erro("A quantidade de cada item deve ser maior que zero.");
            }
            var produto = produtoRepository.findById(item.getProdutoId())
                    .orElseThrow(() -> erro("Produto não encontrado."));
            if (produto.getPreco() == null || produto.getPreco() < 0) {
                throw erro("O produto selecionado não possui preço válido.");
            }
            item.setPrecoUnitario(BigDecimal.valueOf(produto.getPreco()));
            item.setDescricao(produto.getNome());
        }

        BigDecimal desconto = pedido.getPercentualDesconto();
        if (desconto == null) {
            pedido.setPercentualDesconto(BigDecimal.ZERO);
        } else if (desconto.compareTo(BigDecimal.ZERO) < 0
                || desconto.compareTo(DESCONTO_MAXIMO) > 0) {
            throw erro("O desconto deve estar entre 0% e 20%.");
        } else if (desconto.compareTo(BigDecimal.ZERO) > 0
                && !usuarioTemRole("GERENTE") && !usuarioTemRole("ATENDENTE")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Somente o ATENDENTE ou GERENTE pode aplicar descontos.");
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

    public boolean usuarioEhEntregador() {
        return usuarioTemRole("ENTREGADOR");
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

    public List<PedidoEntregaDTO> listarPedidosParaEntrega() {
        return pedidoRepository.findAll().stream()
                .map(this::transformarParaEntrega)
                .toList();
    }

    public PedidoEntregaDTO pedidoParaEntrega(String id) {
        return transformarParaEntrega(buscarPedido(id));
    }

    private PedidoEntregaDTO transformarParaEntrega(Pedido pedido) {
        var cliente = clienteRepository.findById(pedido.getClienteId()).orElse(null);
        if (cliente == null) {
            return new PedidoEntregaDTO(pedido.getId(), "-", "-");
        }
        return new PedidoEntregaDTO(pedido.getId(), enderecoCliente(cliente),
                cliente.getTelefone());
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

    public Pedido atualizarPedido(String id, Pedido dados) {
        if (dados == null) {
            throw erro("Os dados do pedido devem ser informados.");
        }
        Pedido pedido = buscarPedido(id);
        if (!"RECEBIDO".equals(pedido.getStatus())) {
            throw erro("Somente pedidos recebidos podem ser editados.");
        }

        validarPedido(dados);
        pedido.setClienteId(dados.getClienteId());
        pedido.setTipoPizza(dados.getTipoPizza());
        pedido.setQuantidade(dados.getQuantidade());
        pedido.setSabor(dados.getSabor());
        pedido.setItens(dados.getItens());
        BigDecimal descontoAnterior = percentualDoPedido(pedido);
        BigDecimal valorAnterior = pedido.getValorTotal();
        pedido.setValorTotal(calcularValorTotal(pedido));
        registrarAuditoria(pedido, "ALTERACAO_PEDIDO", pedido.getStatus(), pedido.getStatus(),
                null, descontoAnterior, pedido.getPercentualDesconto(),
                valorAnterior, pedido.getValorTotal());
        return pedidoRepository.save(pedido);
    }

    public void excluirPedido(String id) {
        Pedido pedido = buscarPedido(id);
        if (!"RECEBIDO".equals(pedido.getStatus())) {
            throw erro("Somente pedidos recebidos podem ser excluídos.");
        }
        pedidoRepository.deleteById(id);
    }

    // Altera apenas o status, preservando os demais dados do pedido.
    public Pedido atualizarStatus(String id, String status) {
        Pedido pedido = buscarPedido(id);
        String novoStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        validarTransicao(pedido.getStatus(), novoStatus);

        String statusAnterior = pedido.getStatus();
        String usuario = usuarioAutenticado();
        pedido.setStatus(novoStatus);
        if ("SAIU_PARA_ENTREGA".equals(novoStatus) || "RETIRADO".equals(novoStatus)) {
            pedido.setResponsavelSaida(usuario);
            pedido.setDataHoraSaida(LocalDateTime.now());
        }
        registrarAuditoria(pedido, "ALTERACAO_STATUS", statusAnterior, novoStatus,
                null, pedido.getPercentualDesconto(), pedido.getPercentualDesconto(),
                pedido.getValorTotal(), pedido.getValorTotal());
        return pedidoRepository.save(pedido);
    }

    public Pedido cancelarPedido(String id, String justificativa) {
        Pedido pedido = buscarPedido(id);
        exigirGerente();
        if (isBlank(justificativa)) {
            throw erro("A justificativa do cancelamento é obrigatória.");
        }
        if (!List.of("RECEBIDO", "EM_PREPARACAO", "PRONTO").contains(pedido.getStatus())) {
            throw erro("O pedido só pode ser cancelado antes da saída para entrega ou retirada.");
        }

        String statusAnterior = pedido.getStatus();
        pedido.setStatus("CANCELADO");
        pedido.setJustificativaCancelamento(justificativa.trim());
        registrarAuditoria(pedido, "CANCELAMENTO", statusAnterior, "CANCELADO",
                pedido.getJustificativaCancelamento(), pedido.getPercentualDesconto(),
                pedido.getPercentualDesconto(), pedido.getValorTotal(), pedido.getValorTotal());
        return pedidoRepository.save(pedido);
    }

    private Pedido buscarPedido(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));
    }

    private void validarTransicao(String statusAtual, String novoStatus) {
        if (statusAtual == null || novoStatus.isBlank()) {
            throw erro("O status do pedido deve ser informado.");
        }
        boolean valida = ("RECEBIDO".equals(statusAtual) && "EM_PREPARACAO".equals(novoStatus))
                || ("EM_PREPARACAO".equals(statusAtual) && "PRONTO".equals(novoStatus))
                || ("PRONTO".equals(statusAtual)
                && List.of("SAIU_PARA_ENTREGA", "RETIRADO").contains(novoStatus))
                || (List.of("SAIU_PARA_ENTREGA", "RETIRADO").contains(statusAtual)
                && "FINALIZADO".equals(novoStatus));
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

    private BigDecimal percentualDoPedido(Pedido pedido) {
        return pedido.getPercentualDesconto() == null
                ? BigDecimal.ZERO : pedido.getPercentualDesconto();
    }

    private void registrarAuditoria(Pedido pedido, String operacao, String statusAnterior,
            String statusPosterior, String justificativa, BigDecimal descontoAnterior,
            BigDecimal descontoPosterior, BigDecimal valorAnterior, BigDecimal valorPosterior) {
        if (pedido.getAuditoria() == null) {
            pedido.setAuditoria(new ArrayList<>());
        }
        PedidoAuditoria registro = new PedidoAuditoria();
        registro.setOperacao(operacao);
        registro.setUsuario(usuarioAutenticado());
        registro.setDataHora(LocalDateTime.now());
        registro.setStatusAnterior(statusAnterior);
        registro.setStatusPosterior(statusPosterior);
        registro.setJustificativa(justificativa);
        registro.setDescontoAnterior(descontoAnterior);
        registro.setDescontoPosterior(descontoPosterior);
        registro.setValorTotalAnterior(valorAnterior);
        registro.setValorTotalPosterior(valorPosterior);
        pedido.getAuditoria().add(registro);
    }

}
