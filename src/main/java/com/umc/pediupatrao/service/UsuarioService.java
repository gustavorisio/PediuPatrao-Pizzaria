package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.entity.PerfilUsuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    // Busca um usuário por ID
    @PreAuthorize("hasRole('ADMIN')")
    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    // Busca um usuário por username
    @PreAuthorize("hasRole('ADMIN')")
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    // Salva um novo usuário (criptografando a senha)
    @PreAuthorize("hasRole('ADMIN')")
    public void salvarUsuario(Usuario usuario) {
        try {
            validarRole(usuario.getRole());
            if (usuario.getPassword() == null) {
                throw new IllegalArgumentException("A senha é obrigatória.");
            }
            if (usuario.getPassword().isBlank()) {
                throw new IllegalArgumentException("A senha é obrigatória.");
            }
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            Usuario salvo = usuarioRepository.save(usuario);
            auditoriaService.registrar("INCLUSAO", "USUARIO", salvo.getId(),
                    Map.of(), dadosUsuario(salvo), null);
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Não foi possível salvar o usuário.", exception);
        }
    }
    @PreAuthorize("hasRole('ADMIN')")
    public void deletarUsuario(String id) {
        try {
            Usuario usuario = usuarioRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
            auditoriaService.registrar("EXCLUSAO", "USUARIO", id,
                    dadosUsuario(usuario), Map.of(), null);
            usuarioRepository.deleteById(id);
        } catch (DataAccessException exception) {
            throw new IllegalArgumentException("Não foi possível excluir o usuário.", exception);
        }
    }
    @PreAuthorize("hasRole('ADMIN')")
    public Iterable<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public List<Usuario> listarFuncionarios() {
        List<Usuario> funcionarios = new ArrayList<>();
        for (Usuario usuario : usuarioRepository.findAll()) {
            if (usuario.getRole() == PerfilUsuario.GERENTE) {
                funcionarios.add(usuario);
            } else if (usuario.getRole() == PerfilUsuario.ATENDENTE) {
                funcionarios.add(usuario);
            }
        }
        return funcionarios;
    }
    @PreAuthorize("hasRole('ADMIN') or @usuarioService.isCurrentUser(#id)")
    public Usuario atualizarUsuario(String id, Usuario usuarioAtualizado) {
        try {
            Usuario usuarioExistente = usuarioRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
            Map<String, String> antes = dadosUsuario(usuarioExistente);
            boolean administrador = temRole("ADMIN");

            usuarioExistente.setUsername(usuarioAtualizado.getUsername());
            // Editar a própria conta não dá permissão para trocar de perfil.
            if (administrador) {
                validarRole(usuarioAtualizado.getRole());
                usuarioExistente.setRole(usuarioAtualizado.getRole());
            } else if (usuarioAtualizado.getRole() != null) {
                if (usuarioAtualizado.getRole() != usuarioExistente.getRole()) {
                    throw new IllegalArgumentException("Somente ADMIN pode alterar perfis.");
                }
            }

            // Senha em branco na edição mantém a senha atual.
            if (usuarioAtualizado.getPassword() != null) {
                if (!usuarioAtualizado.getPassword().isBlank()) {
                    usuarioExistente.setPassword(passwordEncoder.encode(usuarioAtualizado.getPassword()));
                }
            }

            Usuario salvo = usuarioRepository.save(usuarioExistente);
            auditoriaService.registrar("ALTERACAO", "USUARIO", salvo.getId(),
                    antes, dadosUsuario(salvo), null);
            return salvo;
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Não foi possível atualizar o usuário.", exception);
        }
    }

    public boolean isCurrentUser(String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        if (!authentication.isAuthenticated()) {
            return false;
        }
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) {
            return false;
        }
        return usuario.getUsername().equals(authentication.getName());
    }

    private boolean temRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        for (var autoridade : authentication.getAuthorities()) {
            if (autoridade.getAuthority().equals("ROLE_" + role)) {
                return true;
            }
        }
        return false;
    }

    private void validarRole(PerfilUsuario role) {
        if (role == null) {
            throw new IllegalArgumentException("A role deve ser ADMIN, GERENTE ou ATENDENTE.");
        }
    }

    private Map<String, String> dadosUsuario(Usuario usuario) {
        Map<String, String> dados = new HashMap<>();
        dados.put("username", usuario.getUsername());
        dados.put("role", usuario.getRole() == null ? null : usuario.getRole().name());
        return dados;
    }
}
