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

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    public void registrar(String operacao, String entidade, String entidadeId,
            Map<String, String> antes, Map<String, String> depois, String justificativa) {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || autenticacao instanceof AnonymousAuthenticationToken
                || !autenticacao.isAuthenticated()) {
            return;
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
        auditoria.setValoresAnteriores(antes);
        auditoria.setValoresNovos(depois);
        auditoria.setJustificativa(justificativa);
        auditoriaRepository.save(auditoria);
    }

    public List<Auditoria> listar() {
        return auditoriaRepository.findAll();
    }
}
