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

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

    private final AgendaService agendaService;
    private final UsuarioRepository usuarioRepository; // ✨ NOVO: Adicionado para identificar o barbeiro

    public AgendamentoController(AgendaService agendaService, UsuarioRepository usuarioRepository) {
        this.agendaService = agendaService;
        this.usuarioRepository = usuarioRepository;
    }

    // ✨ Função auxiliar para pegar quem está mexendo no sistema
    private Usuario getUsuarioLogado() {
        String login = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByLogin(login).orElse(null);
    }

    @GetMapping
    public List<Agendamento> listarTodos() {
        return agendaService.listarTodosOsAgendamentos();
    }

    @GetMapping("/bloqueios")
    public List<BloqueioAgenda> listarBloqueios() {
        return agendaService.listarBloqueios();
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

    // ✨ A CORREÇÃO ESTÁ AQUI: Agora ele envia o 'logado' como terceiro parâmetro!
    @GetMapping("/horarios-livres")
    public ResponseEntity<List<LocalTime>> buscarHorariosLivres(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate data,
            @RequestParam Long servicoId) {

        Usuario logado = getUsuarioLogado();
        List<LocalTime> horarios = agendaService.buscarHorariosLivres(data, servicoId, logado);

        return ResponseEntity.ok(horarios);
    }
}