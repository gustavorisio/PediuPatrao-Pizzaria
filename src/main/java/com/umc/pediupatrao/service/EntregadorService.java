package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Entregador;
import com.umc.pediupatrao.repository.EntregadorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EntregadorService {
    private final EntregadorRepository repository;

    public EntregadorService(EntregadorRepository repository) {
        this.repository = repository;
    }

    public List<Entregador> listarTodos() { return repository.findAll(); }
    public Entregador salvar(Entregador entregador) { return repository.save(entregador); }
    public void excluir(String id) { repository.deleteById(id); }
}
