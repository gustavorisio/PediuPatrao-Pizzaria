package com.umc.pediupatrao.controller;

import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Order(0)
@RestControllerAdvice(assignableTypes = DashboardController.class)
public class DashboardErroHandler {
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> indisponivel(DataAccessException erro) {
        return ResponseEntity.status(503).body(Map.of("message", "Dados temporariamente indisponíveis. Tente atualizar novamente."));
    }
}
