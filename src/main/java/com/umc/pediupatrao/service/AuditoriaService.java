package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Auditoria;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;


    public void registrar(String operacao, String entidade, String entidadeId,
            Map<String, String> antes, Map<String, String> depois, String justificativa) {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null) {
            throw new org.springframework.security.access.AccessDeniedException("Autenticação obrigatória para auditar.");
        }
        if (!autenticacao.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Autenticação obrigatória para auditar.");
        }
        if (autenticacao instanceof AnonymousAuthenticationToken) {
            throw new org.springframework.security.access.AccessDeniedException("Autenticação obrigatória para auditar.");
        }
        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(autenticacao.getName());
        auditoria.setPerfil(autenticacao.getAuthorities().stream()
                .map(autoridade -> autoridade.getAuthority().replace("ROLE_", ""))
                .findFirst().orElse(""));
        auditoria.setDataHora(LocalDateTime.now());
        auditoria.setOperacao(operacao);
        auditoria.setEntidade(entidade);
        auditoria.setEntidadeId(entidadeId);
        Map<String, String> alteracoesAnteriores = new java.util.LinkedHashMap<>();
        Map<String, String> alteracoesNovas = new java.util.LinkedHashMap<>();
        java.util.Set<String> campos = new java.util.LinkedHashSet<>(antes.keySet());
        campos.addAll(depois.keySet());
        // Grava só os campos que mudaram, incluindo os que foram removidos.
        for (String campo : campos) {
            if (!java.util.Objects.equals(antes.get(campo), depois.get(campo))) {
                alteracoesAnteriores.put(campo, antes.get(campo));
                alteracoesNovas.put(campo, depois.get(campo));
            }
        }
        auditoria.setValoresAnteriores(alteracoesAnteriores);
        auditoria.setValoresNovos(alteracoesNovas);
        auditoria.setJustificativa(justificativa);
        auditoriaRepository.save(auditoria);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public List<Auditoria> listar() {
        return auditoriaRepository.findAll();
    }
}
