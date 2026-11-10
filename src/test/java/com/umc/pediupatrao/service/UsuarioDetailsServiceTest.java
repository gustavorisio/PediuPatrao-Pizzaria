package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.*;
import com.umc.pediupatrao.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioDetailsServiceTest {
    private final UsuarioRepository repository = mock(UsuarioRepository.class);
    private final UsuarioDetailsService service = new UsuarioDetailsService(repository);

    @ParameterizedTest @EnumSource(PerfilUsuario.class)
    void carregaHashEPermissaoDoUsuario(PerfilUsuario perfil) {
        Usuario usuario = new Usuario(); usuario.setUsername("ana");
        usuario.setPassword("hash-existente"); usuario.setRole(perfil);
        when(repository.findByUsername("ana")).thenReturn(Optional.of(usuario));
        var details = service.loadUserByUsername("ana");
        assertThat(details.getUsername()).isEqualTo("ana");
        assertThat(details.getPassword()).isEqualTo("hash-existente");
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_" + perfil.name());
    }

    @Test void usuarioInexistenteNaoAutentica() {
        assertThatThrownBy(() -> service.loadUserByUsername("ausente"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
