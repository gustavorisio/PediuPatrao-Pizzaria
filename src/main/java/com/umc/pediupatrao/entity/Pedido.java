package com.umc.pediupatrao.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "pedidos")
@Getter
@Setter
@NoArgsConstructor
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private Integer numeroPedido;
    private String clienteId;
    private String tipoPizza;
    private int quantidade;
    private String sabor;
    private StatusPedido status;
    private List<ItemPedido> itens;
    private BigDecimal percentualDesconto;
    private BigDecimal valorTotal;
    private String responsavelRegistro;
    private LocalDateTime dataHoraEntrada;
    private String responsavelSaida;
    private LocalDateTime dataHoraSaida;
    private String justificativaCancelamento;

    public void gerarId() {
        // MongoDB não executa @GeneratedValue; o UUID precisa ser atribuído pela aplicação.
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
