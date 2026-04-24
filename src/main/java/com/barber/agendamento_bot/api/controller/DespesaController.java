package com.barber.agendamento_bot.api.controller;

import com.barber.agendamento_bot.api.entity.Despesa;
import com.barber.agendamento_bot.api.entity.Usuario;
import com.barber.agendamento_bot.api.repository.DespesaRepository;
import com.barber.agendamento_bot.api.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/despesas")
public class DespesaController {

    private final DespesaRepository repository;
    private final UsuarioRepository usuarioRepository;

    public DespesaController(DespesaRepository repository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    private Usuario getUsuarioLogado() {
        String login = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByLogin(login).orElse(null);
    }

    @GetMapping
    public List<Despesa> listarTodas() {
        Usuario logado = getUsuarioLogado();
        if (logado == null) return List.of();

        if (logado.getPerfil().equals("ADMIN") || logado.getPerfil().equals("ROLE_ADMIN")) {
            return repository.findAll();
        }

        // O funcionário só vê despesas dele, mas como só o admin vai criar agora, essa lista será sempre vazia para ele.
        return repository.findAll().stream()
                .filter(d -> d.getDonoDoRegistro() != null && d.getDonoDoRegistro().getId().equals(logado.getId()))
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<?> adicionarDespesa(@RequestBody Despesa despesa) {
        Usuario logado = getUsuarioLogado();

        // ✨ CORREÇÃO: Apenas Admin pode criar despesa
        if (logado == null || (!logado.getPerfil().equals("ADMIN") && !logado.getPerfil().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(403).body("Apenas administradores podem lançar despesas.");
        }

        despesa.setDonoDoRegistro(logado);
        despesa.setDataHora(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));
        return ResponseEntity.ok(repository.save(despesa));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> apagarDespesa(@PathVariable Long id) {
        Usuario logado = getUsuarioLogado();

        // ✨ CORREÇÃO: Apenas Admin pode excluir despesa
        if (logado == null || (!logado.getPerfil().equals("ADMIN") && !logado.getPerfil().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(403).build();
        }

        repository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}