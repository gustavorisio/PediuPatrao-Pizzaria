package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Entregador;
import com.umc.pediupatrao.repository.EntregadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
public class EntregadorService {
    private final EntregadorRepository repository;

    // Lista os entregadores cadastrados para a gestão da operação.
    public List<Entregador> listarTodos() {
        return repository.findAll();
    }

    // Persiste tanto novos entregadores quanto alterações existentes.
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public Entregador salvar(Entregador entregador) {
        return repository.save(entregador);
    }

    // Remove um entregador que não deve mais participar da operação.
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public void excluir(String id) {
        repository.deleteById(id);
    }
}
