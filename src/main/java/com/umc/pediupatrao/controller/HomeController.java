package com.umc.pediupatrao.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.entity.Entregador;
import com.umc.pediupatrao.entity.HorarioFuncionamento;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.EntregadorService;
import com.umc.pediupatrao.service.HorarioFuncionamentoService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.ProdutoService;
import com.umc.pediupatrao.service.UsuarioService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.prepost.PreAuthorize;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    private final PedidoService pedidoService;

    private final ClienteService clienteService;

    private final ProdutoService produtoService;

    private final UsuarioService usuarioService;

    private final EntregadorService entregadorService;

    private final HorarioFuncionamentoService horarioFuncionamentoService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("content", "home :: content");
        return "layout";
    }

    // Renderiza a lista administrativa de usuários.
    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public String listarUsuarios(Model model) {
        List<Usuario> usuarios = (List<Usuario>) usuarioService.listarTodos();
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("content", "usuarios/lista :: content");
        log.info("Carregando fragmento: usuarios/lista :: content");
        return "layout";
    }

    @GetMapping("/usuarios/novo")
    @PreAuthorize("hasRole('ADMIN')")
    // Abre o formulário de criação de usuário.
    public String novoUsuarioForm(Model model) {
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("content", "usuarios/form :: content");
        return "layout";
    }

    @GetMapping("/usuarios/editar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    // Carrega o usuário existente ou retorna à lista com uma mensagem de erro.
    public String editarUsuarioForm(@PathVariable String id, Model model,
            RedirectAttributes redirectAttributes) {
        boolean encontrado = usuarioService.buscarPorId(id).map(u -> {
            model.addAttribute("usuario", u);
            return true;
        }).orElse(false);

        if (!encontrado) {
            redirectAttributes.addFlashAttribute("erro", "Usuário não encontrado.");
            return "redirect:/usuarios";
        }

        model.addAttribute("content", "usuarios/form :: content");
        return "layout";
    }

    @PostMapping("/usuarios/salvar")
    @PreAuthorize("hasRole('ADMIN')")
    // Persiste o novo usuário e informa o resultado na próxima página.
    public String salvarUsuario(@ModelAttribute Usuario usuario,
            RedirectAttributes redirectAttributes) {
        usuarioService.salvarUsuario(usuario);
        redirectAttributes.addFlashAttribute("sucesso", "Usuário criado com sucesso!");
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/editar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    // Atualiza o usuário indicado pela rota.
    public String atualizarUsuario(@PathVariable String id,
            @ModelAttribute Usuario usuario,
            RedirectAttributes redirectAttributes) {
        usuarioService.atualizarUsuario(id, usuario);
        redirectAttributes.addFlashAttribute("sucesso", "Usuário atualizado com sucesso!");
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/deletar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    // Remove o usuário e redireciona para a listagem administrativa.
    public String deletarUsuario(@PathVariable String id,
            RedirectAttributes redirectAttributes) {
        usuarioService.deletarUsuario(id);
        redirectAttributes.addFlashAttribute("sucesso", "Usuário removido com sucesso!");
        return "redirect:/usuarios";
    }

    // Renderiza os pedidos disponíveis para a operação.
    @GetMapping("/pedidos")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE', 'ENTREGADOR')")
    public String pedidos(Model model) {
        if (pedidoService.usuarioEhEntregador()) {
            model.addAttribute("pedidos", pedidoService.listarPedidosParaEntrega());
        } else {
            model.addAttribute("pedidos", pedidoService.listarPedidos());
        }
        model.addAttribute("content", "pedidos :: content");
        log.info("Carregando fragmento: pedidos :: content");
        return "layout";
    }

    // Lista os funcionários operacionais sem incluir a conta ADMIN.
    @GetMapping("/funcionarios")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public String funcionarios(Model model) {
        model.addAttribute("funcionarios", usuarioService.listarFuncionarios());
        model.addAttribute("content", "funcionarios/lista :: content");
        return "layout";
    }

    @GetMapping("/entregadores")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    // Exibe os entregadores cadastrados para a gestão.
    public String entregadores(Model model) {
        model.addAttribute("entregadores", entregadorService.listarTodos());
        model.addAttribute("content", "entregadores/lista :: content");
        return "layout";
    }

    @PostMapping("/entregadores/salvar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    // Salva um novo entregador enviado pelo formulário.
    public String salvarEntregador(@ModelAttribute Entregador entregador,
            RedirectAttributes redirectAttributes) {
        entregadorService.salvar(entregador);
        redirectAttributes.addFlashAttribute("sucesso", "Entregador salvo com sucesso!");
        return "redirect:/entregadores";
    }

    @PostMapping("/entregadores/deletar/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    // Remove o entregador e mantém o usuário na mesma tela.
    public String deletarEntregador(@PathVariable String id, RedirectAttributes redirectAttributes) {
        entregadorService.excluir(id);
        redirectAttributes.addFlashAttribute("sucesso", "Entregador removido com sucesso!");
        return "redirect:/entregadores";
    }

    @GetMapping("/horarios")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    // Mostra os horários que orientam o funcionamento da pizzaria.
    public String horarios(Model model) {
        model.addAttribute("horarios", horarioFuncionamentoService.listarTodos());
        model.addAttribute("content", "horarios/lista :: content");
        return "layout";
    }

    @PostMapping("/horarios/salvar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    // Salva uma configuração de horário enviada pela gestão.
    public String salvarHorario(@ModelAttribute HorarioFuncionamento horario,
            RedirectAttributes redirectAttributes) {
        horarioFuncionamentoService.salvar(horario);
        redirectAttributes.addFlashAttribute("sucesso", "Horário salvo com sucesso!");
        return "redirect:/horarios";
    }

    @PostMapping("/horarios/deletar/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    // Exclui uma configuração de horário selecionada.
    public String deletarHorario(@PathVariable String id, RedirectAttributes redirectAttributes) {
        horarioFuncionamentoService.excluir(id);
        redirectAttributes.addFlashAttribute("sucesso", "Horário removido com sucesso!");
        return "redirect:/horarios";
    }

    // Renderiza o catálogo de produtos para consulta e manutenção.
    @GetMapping("/produtos")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    public String produtos(Model model) {
        List<Produto> produtos = produtoService.listarProdutos();
        model.addAttribute("produtos", produtos);
        model.addAttribute("content", "produtos/lista :: content");
        log.info("Carregando fragmento: produtos/lista :: content");
        return "layout";
    }

    @PostMapping("/produtos/salvar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public String salvarProduto(@ModelAttribute Produto produto,
            RedirectAttributes redirectAttributes) {
        produtoService.salvar(produto);
        redirectAttributes.addFlashAttribute("sucesso", "Produto salvo com sucesso!");
        return "redirect:/produtos";
    }

    @GetMapping("/produtos/editar/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public String editarProduto(@PathVariable String id, Model model,
            RedirectAttributes redirectAttributes) {
        return produtoService.buscarPorId(id)
                .map(produto -> {
                    model.addAttribute("produto", produto);
                    model.addAttribute("content", "produtos/produto-form :: content");
                    return "layout";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("erro", "Produto não encontrado.");
                    return "redirect:/produtos";
                });
    }

    @PostMapping("/produtos/deletar/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public String deletarProduto(@PathVariable String id,
            RedirectAttributes redirectAttributes) {
        if (produtoService.buscarPorId(id).isEmpty()) {
            redirectAttributes.addFlashAttribute("erro", "Produto não encontrado.");
        } else {
            produtoService.excluir(id);
            redirectAttributes.addFlashAttribute("sucesso", "Produto excluído com sucesso!");
        }
        return "redirect:/produtos";
    }

    // Renderiza a lista de clientes para o atendimento.
    @GetMapping("/clientes")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    public String clientes(Model model) {
        List<Cliente> clientes = clienteService.listarClientes();
        model.addAttribute("clientes", clientes);
        model.addAttribute("content", "clientes/lista :: content");
        log.info("Carregando fragmento: clientes/lista :: content");
        return "layout";
    }

    @GetMapping("/clientes/novo")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    // Abre o formulário de cadastro de cliente.
    public String novoClienteForm(Model model) {
        model.addAttribute("cliente", new Cliente());
        model.addAttribute("content", "clientes/form :: content");
        return "layout";
    }

    @PostMapping("/clientes/salvar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    // Salva o cliente e retorna à listagem.
    public String salvarCliente(@ModelAttribute Cliente cliente,
            RedirectAttributes redirectAttributes) {
        clienteService.salvar(cliente);
        redirectAttributes.addFlashAttribute("sucesso", "Cliente criado com sucesso!");
        return "redirect:/clientes";
    }

    @GetMapping("/clientes/editar/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    // Carrega os dados do cliente para edição.
    public String editarClienteForm(@PathVariable String id, Model model,
            RedirectAttributes redirectAttributes) {
        boolean encontrado = clienteService.buscarPorId(id).map(c -> {
            model.addAttribute("cliente", c);
            return true;
        }).orElse(false);

        if (!encontrado) {
            redirectAttributes.addFlashAttribute("erro", "Cliente não encontrado.");
            return "redirect:/clientes";
        }

        model.addAttribute("content", "clientes/form :: content");
        return "layout";
    }

    @PostMapping("/clientes/editar/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    // Mantém o ID da URL ao salvar a edição do cliente.
    public String atualizarCliente(@PathVariable String id,
            @ModelAttribute Cliente cliente,
            RedirectAttributes redirectAttributes) {
        cliente.setId(id);
        clienteService.salvar(cliente);
        redirectAttributes.addFlashAttribute("sucesso", "Cliente atualizado com sucesso!");
        return "redirect:/clientes";
    }

    // Abre a página de configurações permitida para a gestão.
    @GetMapping("/configuracoes")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public String configuracoes(Model model) {
        model.addAttribute("content", "configuracoes :: content");
        return "layout";
    }

    @GetMapping("/teste")
    // Mantém a rota de teste disponível para validações locais.
    public String teste() {
        return "teste";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    // Exibe a área administrativa apenas para a role ADMIN.
    public String admin() {
        return "admin";
    }
}
