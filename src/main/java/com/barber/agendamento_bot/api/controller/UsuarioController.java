package com.barber.agendamento_bot.api.controller;

import com.barber.agendamento_bot.api.entity.Usuario;
import com.barber.agendamento_bot.api.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
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
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            Optional<Usuario> usuario = usuarioRepository.findByLogin(auth.getName());
            if(usuario.isPresent()) return ResponseEntity.ok(usuario.get());
        }
        return ResponseEntity.status(401).build();
    }

    // ✨ ROTA PARA PEGAR AS TAXAS PESSOAIS
    @GetMapping("/taxas")
    public ResponseEntity<Map<String, Double>> getMinhasTaxas() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            Usuario usuario = usuarioRepository.findByLogin(auth.getName()).orElse(null);
            if (usuario != null) {
                return ResponseEntity.ok(Map.of(
                        "credito", usuario.getTaxaCredito() != null ? usuario.getTaxaCredito() : 5.0,
                        "debito", usuario.getTaxaDebito() != null ? usuario.getTaxaDebito() : 2.0
                ));
            }
        }
        return ResponseEntity.status(401).build();
    }

    // ✨ ROTA PARA SALVAR AS TAXAS PESSOAIS
    @PostMapping("/taxas")
    public ResponseEntity<?> salvarMinhasTaxas(@RequestBody Map<String, Double> payload) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            Usuario usuario = usuarioRepository.findByLogin(auth.getName()).orElse(null);
            if (usuario != null) {
                usuario.setTaxaCredito(payload.getOrDefault("credito", 5.0));
                usuario.setTaxaDebito(payload.getOrDefault("debito", 2.0));
                usuarioRepository.save(usuario);
                return ResponseEntity.ok().build();
            }
        }
        return ResponseEntity.status(401).build();
    }
}