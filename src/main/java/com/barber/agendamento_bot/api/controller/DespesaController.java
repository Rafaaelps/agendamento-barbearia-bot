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
    private final UsuarioRepository usuarioRepository; // ✨ NOVO

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

        // Admin vê despesas de todos
        if (logado.getPerfil().equals("ADMIN") || logado.getPerfil().equals("ROLE_ADMIN")) {
            return repository.findAll();
        }

        // ✨ Barbeiro vê só as despesas DELE
        return repository.findAll().stream()
                .filter(d -> d.getDonoDoRegistro() != null && d.getDonoDoRegistro().getId().equals(logado.getId()))
                .collect(Collectors.toList());
    }

    @PostMapping
    public Despesa adicionarDespesa(@RequestBody Despesa despesa) {
        Usuario logado = getUsuarioLogado();
        if (logado != null) despesa.setDonoDoRegistro(logado); // ✨ Carimba a despesa no nome do Barbeiro

        despesa.setDataHora(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));
        return repository.save(despesa);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> apagarDespesa(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}