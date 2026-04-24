package com.barber.agendamento_bot.api.service;

import com.barber.agendamento_bot.api.entity.LogAtividade;
import com.barber.agendamento_bot.api.entity.Usuario;
import com.barber.agendamento_bot.api.repository.LogAtividadeRepository;
import com.barber.agendamento_bot.api.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LogService {

    private final LogAtividadeRepository logRepository;
    private final UsuarioRepository usuarioRepository;

    public LogService(LogAtividadeRepository logRepository, UsuarioRepository usuarioRepository) {
        this.logRepository = logRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // Função que os outros Services vão chamar para registrar uma ação
    public void registrarAcao(String modulo, String acao, String detalhes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) return;

        usuarioRepository.findByLogin(auth.getName()).ifPresent(usuario -> {
            LogAtividade log = new LogAtividade();
            log.setUsuarioResponsavel(usuario);
            log.setModulo(modulo);
            log.setAcao(acao);
            log.setDetalhes(detalhes);
            log.setDataHora(LocalDateTime.now());
            logRepository.save(log);
        });
    }
}