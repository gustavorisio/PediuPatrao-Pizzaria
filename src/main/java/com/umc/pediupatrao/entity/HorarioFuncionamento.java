package com.umc.pediupatrao.entity;

import lombok.Getter;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "horariosAtendimento")
@Getter
@Setter
public class HorarioFuncionamento {
    @Id
    private String id;
    private String diaSemana;
    private String abertura;
    private String fechamento;
    // Quando verdadeiro, os horários de abertura e fechamento não são considerados.
    private boolean fechado;
}
