package com.umc.pediupatrao.controller;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(annotations = org.springframework.web.bind.annotation.RestController.class)
public class ErroApiHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> regraDeNegocio(ResponseStatusException erro) {
        return ResponseEntity.status(erro.getStatusCode())
                .body(Map.of("message", erro.getReason() == null ? "Operação inválida." : erro.getReason()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> dadosInvalidos(IllegalArgumentException erro) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", erro.getMessage()));
    }
}
