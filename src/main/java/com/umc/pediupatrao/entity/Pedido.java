package com.umc.pediupatrao.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "pedidos")
@Entity
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
    @Enumerated(EnumType.STRING)
    private StatusPedido status;
    private List<ItemPedido> itens;
    private BigDecimal percentualDesconto;
    private BigDecimal valorTotal;
    private String responsavelRegistro;
    private LocalDateTime dataHoraEntrada;
    private String responsavelSaida;
    private LocalDateTime dataHoraSaida;
    private String justificativaCancelamento;
    private List<PedidoAuditoria> auditoria = new ArrayList<>();

    public void gerarId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
