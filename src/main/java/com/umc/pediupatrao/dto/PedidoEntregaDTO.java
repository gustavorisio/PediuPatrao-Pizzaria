package com.umc.pediupatrao.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PedidoEntregaDTO {
    private String numeroPedido;
    private String endereco;
    private LocalDateTime dataHoraEntrada;
}
