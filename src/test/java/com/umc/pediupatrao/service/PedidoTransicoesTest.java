package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.*;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PedidoTransicoesTest {
    private final PedidoRegrasService regras = new PedidoRegrasService(
            mock(ClienteRepository.class), mock(ProdutoRepository.class));
    private static final Set<String> PERMITIDAS = Set.of(
            "RECEBIDO/EM_PREPARACAO", "EM_PREPARACAO/PRONTO", "PRONTO/SAIU_PARA_ENTREGA",
            "PRONTO/RETIRADO", "SAIU_PARA_ENTREGA/FINALIZADO", "RETIRADO/FINALIZADO");

    static Stream<Arguments> combinacoes() {
        return Arrays.stream(StatusPedido.values()).flatMap(atual ->
                Arrays.stream(StatusPedido.values()).map(novo -> Arguments.of(atual, novo)));
    }

    @ParameterizedTest @MethodSource("combinacoes")
    void validaTodasAsCombinacoesDeStatus(StatusPedido atual, StatusPedido novo) {
        if (PERMITIDAS.contains(atual.name() + "/" + novo.name())) {
            assertThatCode(() -> regras.validarTransicao(atual, novo)).doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> regras.validarTransicao(atual, novo))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(400));
        }
    }

    @Test void exigeStatusAtualENovo() {
        assertThatThrownBy(() -> regras.validarTransicao(null, StatusPedido.RECEBIDO))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> regras.validarTransicao(StatusPedido.RECEBIDO, null))
                .isInstanceOf(ResponseStatusException.class);
    }
}
