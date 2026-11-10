package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.repository.ClienteRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ClienteServiceTest {
    private final ClienteRepository repository = mock(ClienteRepository.class);
    private final AuditoriaService auditoria = mock(AuditoriaService.class);
    private final ClienteService service = new ClienteService(repository, auditoria);

    private Cliente cliente(String id, String telefone) {
        Cliente c = new Cliente(); c.setId(id); c.setNome("Ana"); c.setTelefone(telefone); return c;
    }

    @Test void inclusaoRegistraDadosDoClienteSalvo() {
        Cliente novo = cliente(null, "111"); Cliente salvo = cliente("1", "111");
        when(repository.save(novo)).thenReturn(salvo);
        assertThat(service.salvar(novo)).isSameAs(salvo);
        verify(auditoria).registrar(eq("INCLUSAO"), eq("CLIENTE"), eq("1"),
                argThat(m -> m.isEmpty()), argThat(m -> "111".equals(m.get("telefone"))), isNull());
    }

    @Test void alteracaoRegistraTelefoneAnteriorENovo() {
        Cliente novo = cliente("1", "222");
        when(repository.findById("1")).thenReturn(Optional.of(cliente("1", "111")));
        when(repository.save(novo)).thenReturn(novo);
        service.salvar(novo);
        verify(auditoria).registrar(eq("ALTERACAO"), eq("CLIENTE"), eq("1"),
                argThat(m -> "111".equals(m.get("telefone"))),
                argThat(m -> "222".equals(m.get("telefone"))), isNull());
    }

    @Test void exclusaoRegistraHistoricoAntesDeRemover() {
        when(repository.findById("1")).thenReturn(Optional.of(cliente("1", "111")));
        service.excluir("1");
        InOrder ordem = inOrder(auditoria, repository);
        ordem.verify(auditoria).registrar(eq("EXCLUSAO"), eq("CLIENTE"), eq("1"),
                argThat(m -> "Ana".equals(m.get("nome"))), argThat(m -> m.isEmpty()), isNull());
        ordem.verify(repository).deleteById("1");
    }

    @Test void clienteInexistenteNaoGeraAlteracaoNemExclusao() {
        assertThatThrownBy(() -> service.salvar(cliente("ausente", "111")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.excluir("ausente")).isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(any()); verify(repository, never()).deleteById(anyString());
        verifyNoInteractions(auditoria);
    }

    @Test void falhaNaAuditoriaImpedeExclusao() {
        when(repository.findById("1")).thenReturn(Optional.of(cliente("1", "111")));
        doThrow(new IllegalStateException("indisponível")).when(auditoria)
                .registrar(anyString(), anyString(), anyString(), anyMap(), anyMap(), isNull());
        assertThatThrownBy(() -> service.excluir("1")).isInstanceOf(IllegalStateException.class);
        verify(repository, never()).deleteById(anyString());
    }
}
