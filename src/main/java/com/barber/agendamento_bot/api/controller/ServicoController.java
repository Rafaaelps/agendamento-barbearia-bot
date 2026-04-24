package com.barber.agendamento_bot.api.controller;

import com.barber.agendamento_bot.api.entity.Servico;
import com.barber.agendamento_bot.api.entity.Usuario;
import com.barber.agendamento_bot.api.repository.ServicoRepository;
import com.barber.agendamento_bot.api.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/servicos")
public class ServicoController {

    private final ServicoRepository servicoRepository;
    private final UsuarioRepository usuarioRepository;

    public ServicoController(ServicoRepository servicoRepository, UsuarioRepository usuarioRepository) {
        this.servicoRepository = servicoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    private Usuario getUsuarioLogado() {
        String login = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByLogin(login).orElse(null);
    }

    @GetMapping
    public List<Servico> listarTodosOsServicos() {
        Usuario logado = getUsuarioLogado();
        if (logado == null) return List.of();

        List<Servico> lista;
        if (logado.getPerfil().equals("ADMIN") || logado.getPerfil().equals("ROLE_ADMIN")) {
            lista = servicoRepository.findAll();
        } else {
            lista = servicoRepository.findAllByDonoDoRegistro(logado);
        }

        // Filtra para mandar para a tela APENAS os que não estão na lixeira
        return lista.stream().filter(s -> s.getAtivo() == null || s.getAtivo()).collect(Collectors.toList());
    }

    @PostMapping
    public Servico criar(@RequestBody Servico servico) {
        Usuario logado = getUsuarioLogado();
        if (logado != null) servico.setDonoDoRegistro(logado);
        servico.setAtivo(true);
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

    // ✨ NOVO: A Rota de Exclusão Segura
    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluirServico(@PathVariable Long id) {
        return servicoRepository.findById(id).map(servico -> {
            servico.setAtivo(false);
            servicoRepository.save(servico);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}