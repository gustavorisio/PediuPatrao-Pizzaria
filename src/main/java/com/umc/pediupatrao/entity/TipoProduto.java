package com.umc.pediupatrao.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum TipoProduto {
    PIZZA,
    LANCHE,
    BEBIDA,
    OPCIONAL;

    @JsonCreator
    public static TipoProduto fromValue(String value) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Tipo de produto inválido: " + value, exception);
        }
    }

    @JsonValue
    public String getValue() {
        return name();
    }

    public String getLabel() {
        return switch (this) {
            case PIZZA -> "Pizza";
            case LANCHE -> "Lanche";
            case BEBIDA -> "Bebida";
            case OPCIONAL -> "Opcional";
        };
    }
}
