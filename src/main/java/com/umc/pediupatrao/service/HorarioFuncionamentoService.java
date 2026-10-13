package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.HorarioFuncionamento;
import com.umc.pediupatrao.repository.HorarioFuncionamentoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HorarioFuncionamentoService {
    private final HorarioFuncionamentoRepository repository;

    public HorarioFuncionamentoService(HorarioFuncionamentoRepository repository) {
        this.repository = repository;
    }

    public List<HorarioFuncionamento> listarTodos() { return repository.findAll(); }
    public HorarioFuncionamento salvar(HorarioFuncionamento horario) { return repository.save(horario); }
    public void excluir(String id) { repository.deleteById(id); }
}
