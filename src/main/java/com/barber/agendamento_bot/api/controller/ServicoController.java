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

    private Usuario getUsuarioLogado() {
        String login = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByLogin(login).orElse(null);
    }

    @GetMapping
    public List<Servico> listarTodos() {
        Usuario logado = getUsuarioLogado();
        if (logado == null) return List.of();

        // ✨ O SUPER_ADMIN vê TODOS os serviços cadastrados na barbearia
        if ("SUPER_ADMIN".equals(logado.getPerfil())) {
            return servicoRepository.findAll().stream()
                    .filter(s -> s.getAtivo() == null || s.getAtivo())
                    .toList();
        }

        // O ADMIN (Sócio) vê apenas os serviços dele e dos barbeiros
        if ("ADMIN".equals(logado.getPerfil()) || "ROLE_ADMIN".equals(logado.getPerfil())) {
            return servicoRepository.findAll().stream()
                    .filter(s -> s.getAtivo() == null || s.getAtivo())
                    .filter(s -> s.getDonoDoRegistro() == null ||
                            s.getDonoDoRegistro().getId().equals(logado.getId()) ||
                            "BARBEIRO".equals(s.getDonoDoRegistro().getPerfil()))
                    .toList();
        }

        // O BARBEIRO vê apenas os serviços criados por ele mesmo
        return servicoRepository.findAllByDonoDoRegistro(logado).stream()
                .filter(s -> s.getAtivo() == null || s.getAtivo())
                .toList();
    }

    @PostMapping
    public ResponseEntity<Servico> adicionarServico(@RequestBody Servico servico) {
        Usuario logado = getUsuarioLogado();
        if (logado != null) {
            servico.setDonoDoRegistro(logado);
        }
        servico.setAtivo(true);
        return ResponseEntity.ok(servicoRepository.save(servico));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Servico> atualizarServico(@PathVariable Long id, @RequestBody Servico servicoAtualizado) {
        return servicoRepository.findById(id).map(servico -> {
            servico.setNome(servicoAtualizado.getNome());
            servico.setPreco(servicoAtualizado.getPreco());
            servico.setDuracaoMinutos(servicoAtualizado.getDuracaoMinutos());
            return ResponseEntity.ok(servicoRepository.save(servico));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluirServico(@PathVariable Long id) {
        return servicoRepository.findById(id).map(servico -> {
            servico.setAtivo(false);
            servicoRepository.save(servico);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}