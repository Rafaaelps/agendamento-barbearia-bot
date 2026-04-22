package com.barber.agendamento_bot.api.controller;

import com.barber.agendamento_bot.api.entity.Agendamento;
import com.barber.agendamento_bot.api.entity.BloqueioAgenda;
import com.barber.agendamento_bot.api.service.AgendaService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

    private final AgendaService agendaService;

    public AgendamentoController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    @PostMapping
    public String receberNovoAgendamento(@RequestBody Agendamento novoAgendamento) {
        boolean sucesso = agendaService.tentarAgendar(novoAgendamento);
        if (sucesso) return "✅ Sucesso! Agendamento confirmado para " + novoAgendamento.getNomeCliente();
        else return "❌ Este horário já está ocupado. Por favor, escolha outro.";
    }

    // ✨ NOVO: Endpoint para Encaixe Manual
    @PostMapping("/encaixe")
    public String realizarEncaixe(@RequestBody Agendamento novoEncaixe) {
        agendaService.forcarAgendamento(novoEncaixe);
        return "✅ Encaixe realizado com sucesso!";
    }

    @PostMapping("/bloqueios")
    public String criarBloqueio(@RequestBody BloqueioAgenda bloqueio) {
        agendaService.adicionarBloqueio(bloqueio);
        return "✅ Horário bloqueado com sucesso! Motivo: " + bloqueio.getMotivo();
    }

    @GetMapping("/livres")
    public List<LocalTime> consultarHorariosLivres(@RequestParam LocalDate data, @RequestParam Long servicoId) {
        return agendaService.buscarHorariosLivres(data, servicoId);
    }

    @GetMapping
    public List<Agendamento> listarAgendamentos() {
        return agendaService.listarTodosOsAgendamentos();
    }

    @GetMapping("/bloqueios")
    public List<BloqueioAgenda> listarBloqueios() {
        return agendaService.listarBloqueios();
    }

    @PutMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id) {
        agendaService.cancelarAgendamento(id);
        return "Agendamento cancelado com sucesso!";
    }

    @PutMapping("/{id}/concluir")
    public String concluir(@PathVariable Long id) {
        agendaService.concluirAgendamento(id);
        return "💰 Agendamento marcado como concluído!";
    }

    @PutMapping("/{id}/valor")
    public String alterarValor(@PathVariable Long id, @RequestParam java.math.BigDecimal novoValor) {
        agendaService.atualizarValor(id, novoValor);
        return "💸 Valor alterado com sucesso!";
    }
}