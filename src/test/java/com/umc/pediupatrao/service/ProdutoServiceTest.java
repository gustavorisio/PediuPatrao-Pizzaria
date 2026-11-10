package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProdutoServiceTest {
    private final ProdutoRepository repository = mock(ProdutoRepository.class);
    private final ProdutoService service = new ProdutoService(repository);

    private static Produto pizza() {
        Produto p = new Produto(); p.setNome("Calabresa"); p.setTipo("Pizza");
        p.setTipoPizza("Grande"); p.setSabor("Calabresa"); p.setPreco(35.0);
        return p;
    }

    static Stream<Consumer<Produto>> dadosInvalidos() {
        return Stream.of(p -> p.setNome(null), p -> p.setNome(" "),
                p -> p.setTipo(null), p -> p.setTipo("Outro"),
                p -> p.setTipoPizza(" "), p -> p.setSabor(null),
                p -> p.setPreco(null), p -> p.setPreco(-0.01),
                p -> p.setPreco(Double.NaN), p -> p.setPreco(Double.POSITIVE_INFINITY),
                p -> p.setPreco(Double.NEGATIVE_INFINITY));
    }

    @ParameterizedTest @MethodSource("dadosInvalidos")
    void rejeitaDadosInvalidosSemPersistir(Consumer<Produto> alterar) {
        Produto p = pizza(); alterar.accept(p);
        assertThatThrownBy(() -> service.salvar(p)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test void rejeitaProdutoAusente() {
        assertThatThrownBy(() -> service.salvar(null)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @ParameterizedTest @ValueSource(strings = {"Pizza", "Lanche", "Bebida", "Opcional"})
    void aceitaTiposDoCatalogoEPrecoZero(String tipo) {
        Produto p = pizza(); p.setTipo(tipo); p.setPreco(0.0);
        if (!tipo.equals("Pizza")) { p.setTipoPizza(null); p.setSabor(null); }
        when(repository.save(p)).thenReturn(p);
        assertThat(service.salvar(p)).isSameAs(p);
        assertThat(p.getAtivo()).isFalse();
        verify(repository).save(p);
    }

    @Test void naoAtualizaProdutoInexistente() {
        Produto p = pizza(); p.setId("ausente");
        assertThatThrownBy(() -> service.salvar(p)).isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(any());
    }

    @Test void atualizaProdutoExistentePreservandoAtivo() {
        Produto p = pizza(); p.setId("pizza"); p.setAtivo(true);
        when(repository.existsById("pizza")).thenReturn(true);
        when(repository.save(p)).thenReturn(p);
        assertThat(service.salvar(p).getAtivo()).isTrue();
        verify(repository).save(p);
    }
}
