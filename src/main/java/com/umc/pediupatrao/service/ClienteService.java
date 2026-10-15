package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final AuditoriaService auditoriaService;

    public ClienteService(ClienteRepository clienteRepository, AuditoriaService auditoriaService) {
        this.clienteRepository = clienteRepository;
        this.auditoriaService = auditoriaService;
    }

    public Cliente novoCliente(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    // Método para excluir cliente
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

    // Método para salvar um novo cliente ou atualizar um cliente existente
    public Cliente salvar(Cliente cliente) {
        // Se o cliente não tem ID (novo cliente), salva como novo
        if (cliente.getId() == null) {
            Cliente salvo = clienteRepository.save(cliente);
            auditoriaService.registrar("INCLUSAO", "CLIENTE", salvo.getId(),
                    new HashMap<>(), dadosCliente(salvo), null);
            return salvo;
        } // Se já tem ID (cliente existente), atualiza
        else {
            // Verifica se o cliente existe antes de atualizar
            if (clienteRepository.existsById(cliente.getId())) {
                Cliente antigo = clienteRepository.findById(cliente.getId()).orElseThrow();
                Cliente salvo = clienteRepository.save(cliente);
                auditoriaService.registrar("ALTERACAO", "CLIENTE", salvo.getId(),
                        dadosCliente(antigo), dadosCliente(salvo), null);
                return salvo;
            } else {
                throw new IllegalArgumentException("Cliente não encontrado para atualização.");
            }
        }
    }

    private Map<String, String> dadosCliente(Cliente cliente) {
        Map<String, String> dados = new HashMap<>();
        dados.put("nome", cliente.getNome());
        dados.put("telefone", cliente.getTelefone());
        dados.put("logradouro", cliente.getLogradouro());
        dados.put("numero", cliente.getNumero());
        dados.put("complemento", cliente.getComplemento());
        dados.put("bairro", cliente.getBairro());
        dados.put("cidade", cliente.getCidade());
        dados.put("estado", cliente.getEstado());
        return dados;
    }

}
