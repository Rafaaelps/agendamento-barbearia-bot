package com.barber.agendamento_bot.api.controller;

import com.barber.agendamento_bot.api.entity.Agendamento;
import com.barber.agendamento_bot.api.entity.BloqueioAgenda;
import com.barber.agendamento_bot.api.entity.Usuario;
import com.barber.agendamento_bot.api.repository.UsuarioRepository;
import com.barber.agendamento_bot.api.service.AgendaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

    private final AgendaService agendaService;
    private final UsuarioRepository usuarioRepository;

    public AgendamentoController(AgendaService agendaService, UsuarioRepository usuarioRepository) {
        this.agendaService = agendaService;
        this.usuarioRepository = usuarioRepository;
    }

    private Usuario getUsuarioLogado() {
        String login = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByLogin(login).orElse(null);
    }

    @GetMapping
    public List<Agendamento> listarTodos() {
        Usuario logado = getUsuarioLogado();
        List<Agendamento> todos = agendaService.listarTodosOsAgendamentos();
        if (logado == null) return List.of();

        if (logado.getPerfil().equals("SUPER_ADMIN")) {
            return todos;
        } else if (logado.getPerfil().equals("ADMIN") || logado.getPerfil().equals("ROLE_ADMIN")) {
            return todos.stream().filter(a -> {
                Usuario dono = a.getDonoDoRegistro();
                if (dono == null) return true; // Mostra encaixes órfãos antigos para o Admin
                // ✨ ALTERAÇÃO: Agora verifica se o perfil é "BARBEIRO"
                return dono.getId().equals(logado.getId()) || dono.getPerfil().equals("BARBEIRO");
            }).collect(Collectors.toList());
        } else {
            return todos.stream()
                    .filter(a -> a.getDonoDoRegistro() != null && a.getDonoDoRegistro().getId().equals(logado.getId()))
                    .collect(Collectors.toList());
        }
    }

    @GetMapping("/bloqueios")
    public List<BloqueioAgenda> listarBloqueios() {
        Usuario logado = getUsuarioLogado();
        List<BloqueioAgenda> todos = agendaService.listarBloqueios();
        if (logado == null) return List.of();

        if (logado.getPerfil().equals("SUPER_ADMIN")) {
            return todos;
        } else if (logado.getPerfil().equals("ADMIN") || logado.getPerfil().equals("ROLE_ADMIN")) {
            return todos.stream().filter(b -> {
                Usuario dono = b.getDonoDoRegistro();
                if (dono == null) return true;
                // ✨ ALTERAÇÃO: Agora verifica se o perfil é "BARBEIRO"
                return dono.getId().equals(logado.getId()) || dono.getPerfil().equals("BARBEIRO");
            }).collect(Collectors.toList());
        } else {
            return todos.stream()
                    .filter(b -> b.getDonoDoRegistro() != null && b.getDonoDoRegistro().getId().equals(logado.getId()))
                    .collect(Collectors.toList());
        }
    }

    @PostMapping("/encaixe")
    public ResponseEntity<?> realizarEncaixe(@RequestBody Agendamento agendamento) {
        agendaService.forcarAgendamento(agendamento);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bloqueios")
    public ResponseEntity<?> adicionarBloqueio(@RequestBody BloqueioAgenda bloqueio) {
        agendaService.adicionarBloqueio(bloqueio);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelar(@PathVariable Long id) {
        agendaService.cancelarAgendamento(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/concluir")
    public ResponseEntity<?> concluir(@PathVariable Long id) {
        agendaService.concluirAgendamento(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/valor")
    public ResponseEntity<?> atualizarValor(@PathVariable Long id, @RequestParam BigDecimal novoValor) {
        agendaService.atualizarValor(id, novoValor);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/horarios-livres")
    public ResponseEntity<List<LocalTime>> buscarHorariosLivres(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate data,
            @RequestParam Long servicoId) {
        Usuario logado = getUsuarioLogado();
        List<LocalTime> horarios = agendaService.buscarHorariosLivres(data, servicoId, logado);
        return ResponseEntity.ok(horarios);
    }
}