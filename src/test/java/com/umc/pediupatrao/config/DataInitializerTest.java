package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.*;
import com.umc.pediupatrao.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataInitializerTest {
    private final UsuarioRepository repository = mock(UsuarioRepository.class);

    @Test void criaAdministradorComSenhaBcryptQuandoAusente() throws Exception {
        new DataInitializer().initDatabase(repository).run();
        var captor = ArgumentCaptor.forClass(Usuario.class); verify(repository).save(captor.capture());
        Usuario admin = captor.getValue();
        assertThat(admin.getUsername()).isEqualTo("admin");
        assertThat(admin.getRole()).isEqualTo(PerfilUsuario.ADMIN);
        assertThat(admin.getPassword()).isNotEqualTo("teste123");
        assertThat(new BCryptPasswordEncoder().matches("teste123", admin.getPassword())).isTrue();
    }

    @Test void naoSubstituiSenhaOuPerfilDoAdministradorExistente() throws Exception {
        Usuario admin = new Usuario(); admin.setPassword("senha-alterada"); admin.setRole(PerfilUsuario.ADMIN);
        when(repository.findByUsername("admin")).thenReturn(Optional.of(admin));
        new DataInitializer().initDatabase(repository).run();
        verify(repository, never()).save(any());
        assertThat(admin.getPassword()).isEqualTo("senha-alterada");
    }
}
