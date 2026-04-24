package com.barber.agendamento_bot.api.controller;

import com.barber.agendamento_bot.api.entity.Servico;
import com.barber.agendamento_bot.api.entity.Usuario;
import com.barber.agendamento_bot.api.repository.ServicoRepository;
import com.barber.agendamento_bot.api.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servicos")
public class ServicoController {

    private final ServicoRepository servicoRepository;
    private final UsuarioRepository usuarioRepository;

    public ServicoController(ServicoRepository servicoRepository, UsuarioRepository usuarioRepository) {
        this.servicoRepository = servicoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // ✨ AJUDA A DESCOBRIR QUEM ESTÁ LOGADO
    private Usuario getUsuarioLogado() {
        String login = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByLogin(login).orElse(null);
    }

    @GetMapping
    public List<Servico> listarTodosOsServicos() {
        Usuario logado = getUsuarioLogado();
        if (logado == null) return List.of();

        // ADMIN vê tudo, Barbeiro vê os dele
        if (logado.getPerfil().equals("ADMIN") || logado.getPerfil().equals("ROLE_ADMIN")) {
            return servicoRepository.findAll();
        }
        return servicoRepository.findAllByDonoDoRegistro(logado);
    }

    @PostMapping
    public Servico criar(@RequestBody Servico servico) {
        Usuario logado = getUsuarioLogado();
        if (logado != null) servico.setDonoDoRegistro(logado); // ✨ Salva no nome dele
        return servicoRepository.save(servico);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Servico> atualizar(@PathVariable Long id, @RequestBody Servico dadosAtualizados) {
        return servicoRepository.findById(id).map(servico -> {
            servico.setNome(dadosAtualizados.getNome());
            servico.setPreco(dadosAtualizados.getPreco());
            servico.setDuracaoMinutos(dadosAtualizados.getDuracaoMinutos());
            return ResponseEntity.ok(servicoRepository.save(servico));
        }).orElse(ResponseEntity.notFound().build());
    }
}