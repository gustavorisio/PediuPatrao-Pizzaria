package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final AuditoriaService auditoriaService;


    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }
    public void excluir(String id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
        auditoriaService.registrar("EXCLUSAO", "CLIENTE", id,
                dadosCliente(cliente), new HashMap<>(), null);
        clienteRepository.deleteById(id);
    }

    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    public Cliente salvar(Cliente cliente) {
        Map<String, String> antes = new HashMap<>();
        String operacao = "INCLUSAO";
        if (cliente.getId() != null) {
            // Lê os dados antigos antes de salvar o telefone ou endereço novo.
            Cliente antigo = clienteRepository.findById(cliente.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado para atualização."));
            antes = dadosCliente(antigo);
            operacao = "ALTERACAO";
        }
        Cliente salvo = clienteRepository.save(cliente);
        auditoriaService.registrar(operacao, "CLIENTE", salvo.getId(), antes, dadosCliente(salvo), null);
        return salvo;
    }
    private Map<String, String> dadosCliente(Cliente cliente) {
        Map<String, String> dados = new HashMap<>();
        dados.put("nome", cliente.getNome());
        dados.put("telefone", cliente.getTelefone());
        dados.put("cep", cliente.getCep());
        dados.put("endereco", cliente.getEndereco());
        dados.put("logradouro", cliente.getLogradouro());
        dados.put("numero", cliente.getNumero());
        dados.put("complemento", cliente.getComplemento());
        dados.put("bairro", cliente.getBairro());
        dados.put("cidade", cliente.getCidade());
        dados.put("estado", cliente.getEstado());
        return dados;
    }

}
