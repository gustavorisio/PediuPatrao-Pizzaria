package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.Optional;

@Service
public class UsuarioService {

    private static final Set<String> ROLES_PERMITIDAS = Set.of("ADMIN", "GERENTE", "ATENDENTE");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

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
            if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
                throw new IllegalArgumentException("A senha é obrigatória.");
            }
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            usuarioRepository.save(usuario);
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Não foi possível salvar o usuário.", exception);
        }
    }

    // Deleta um usuário pelo ID
    @PreAuthorize("hasRole('ADMIN')")
    public void deletarUsuario(String id) {
        try {
            usuarioRepository.deleteById(id);
        } catch (DataAccessException exception) {
            throw new IllegalArgumentException("Não foi possível excluir o usuário.", exception);
        }
    }

    // Lista todos os usuários
    @PreAuthorize("hasRole('ADMIN')")
    public Iterable<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    // Atualiza o usuário usuários
    @PreAuthorize("hasRole('ADMIN') or @usuarioService.isCurrentUser(#id)")
    public Usuario atualizarUsuario(String id, Usuario usuarioAtualizado) {
        try {
            Usuario usuarioExistente = usuarioRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
            boolean administrador = temRole("ADMIN");

            usuarioExistente.setUsername(usuarioAtualizado.getUsername());
            if (administrador) {
                validarRole(usuarioAtualizado.getRole());
                usuarioExistente.setRole(usuarioAtualizado.getRole());
            } else if (usuarioAtualizado.getRole() != null
                    && !usuarioAtualizado.getRole().equals(usuarioExistente.getRole())) {
                throw new IllegalArgumentException("Somente ADMIN pode alterar perfis.");
            }

            if (usuarioAtualizado.getPassword() != null && !usuarioAtualizado.getPassword().isBlank()) {
                usuarioExistente.setPassword(passwordEncoder.encode(usuarioAtualizado.getPassword()));
            }

            return usuarioRepository.save(usuarioExistente);
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Não foi possível atualizar o usuário.", exception);
        }
    }

    public boolean isCurrentUser(String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && buscarPorIdSemAutorizacao(id)
                        .map(usuario -> usuario.getUsername().equals(authentication.getName()))
                        .orElse(false);
    }

    private Optional<Usuario> buscarPorIdSemAutorizacao(String id) {
        return usuarioRepository.findById(id);
    }

    private boolean temRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    private void validarRole(String role) {
        if (role == null || !ROLES_PERMITIDAS.contains(role)) {
            throw new IllegalArgumentException("A role deve ser ADMIN, GERENTE ou ATENDENTE.");
        }
    }
}
