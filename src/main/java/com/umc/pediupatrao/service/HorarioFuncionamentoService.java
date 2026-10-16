package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.HorarioFuncionamento;
import com.umc.pediupatrao.repository.HorarioFuncionamentoRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HorarioFuncionamentoService {
    private final HorarioFuncionamentoRepository repository;

    // Consulta os horários exibidos para a equipe e para o atendimento.
    public List<HorarioFuncionamento> listarTodos() {
        return repository.findAll();
    }

    // Persiste a configuração de funcionamento de um dia da semana.
    public HorarioFuncionamento salvar(HorarioFuncionamento horario) {
        return repository.save(horario);
    }

    // Exclui uma configuração de horário pelo seu identificador.
    public void excluir(String id) {
        repository.deleteById(id);
    }
}
