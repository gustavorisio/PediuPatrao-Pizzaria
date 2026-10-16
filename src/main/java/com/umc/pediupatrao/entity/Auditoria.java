package com.umc.pediupatrao.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "auditorias")
@Getter
@Setter
public class Auditoria {

    @Id
    private String id;
    private String usuario;
    private String perfil;
    private LocalDateTime dataHora;
    private String operacao;
    private String entidade;
    private String entidadeId;
    private Map<String, String> valoresAnteriores;
    private Map<String, String> valoresNovos;
    private String justificativa;
}
