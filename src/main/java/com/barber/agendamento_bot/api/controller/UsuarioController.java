package com.barber.agendamento_bot.api.controller;

import com.barber.agendamento_bot.api.entity.Usuario;
import com.barber.agendamento_bot.api.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/usuario")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    public UsuarioController(UsuarioRepository repository) {
        this.usuarioRepository = repository;
    }

    @GetMapping("/me")
    public ResponseEntity<Usuario> getUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Verifica se existe alguém logado e se não é um usuário anônimo (visitante da página de login)
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String login = auth.getName();
            Optional<Usuario> usuario = usuarioRepository.findByLogin(login);

            if(usuario.isPresent()) {
                return ResponseEntity.ok(usuario.get());
            }
        }
        return ResponseEntity.status(401).build();
    }
}