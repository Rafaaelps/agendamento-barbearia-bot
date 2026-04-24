package com.barber.agendamento_bot.api.controller;

import com.barber.agendamento_bot.api.entity.LogAtividade;
import com.barber.agendamento_bot.api.entity.Usuario;
import com.barber.agendamento_bot.api.repository.LogAtividadeRepository;
import com.barber.agendamento_bot.api.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UsuarioRepository usuarioRepository;
    private final LogAtividadeRepository logRepository;

    public AdminController(UsuarioRepository usuarioRepository, LogAtividadeRepository logRepository) {
        this.usuarioRepository = usuarioRepository;
        this.logRepository = logRepository;
    }

    @GetMapping("/funcionarios")
    public ResponseEntity<List<Usuario>> listarFuncionarios() {
        if (!isAdmin()) {
            return ResponseEntity.status(403).build();
        }

        List<Usuario> barbeiros = usuarioRepository.findAll();
        return ResponseEntity.ok(barbeiros);
    }

    @GetMapping("/auditoria")
    public ResponseEntity<List<LogAtividade>> puxarAuditoria() {
        if (!isAdmin()) {
            return ResponseEntity.status(403).build();
        }

        // Agora com o import correto (org.springframework.data.domain.Sort) lá em cima, isso funciona perfeitamente!
        List<LogAtividade> logs = logRepository.findAll(Sort.by(Sort.Direction.DESC, "dataHora"));
        return ResponseEntity.ok(logs);
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;

        // Verifica se o usuário tem a permissão de ADMIN ou ROLE_ADMIN
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));
    }
}