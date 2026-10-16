package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Entregador;
import com.umc.pediupatrao.repository.EntregadorRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EntregadorService {
    private final EntregadorRepository repository;

    // Lista os entregadores cadastrados para a gestão da operação.
    public List<Entregador> listarTodos() {
        return repository.findAll();
    }

    // Persiste tanto novos entregadores quanto alterações existentes.
    public Entregador salvar(Entregador entregador) {
        return repository.save(entregador);
    }

    // Remove um entregador que não deve mais participar da operação.
    public void excluir(String id) {
        repository.deleteById(id);
    }
}
