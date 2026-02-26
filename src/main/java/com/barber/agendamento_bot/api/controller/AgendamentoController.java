package com.barber.agendamento_bot.api.controller;

import com.barber.agendamento_bot.api.entity.Agendamento;
import com.barber.agendamento_bot.api.entity.BloqueioAgenda;
import com.barber.agendamento_bot.api.service.AgendaService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController // 1. Avisa ao Spring que este é um NOVO Garçom
@RequestMapping("/api/agendamentos") // 2. O endereço específico dele!
public class AgendamentoController {

    private final AgendaService agendaService;

    // Injeta a regra de negócios
    public AgendamentoController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    // 3. O @PostMapping espera receber os dados JSON
    @PostMapping
    public String receberNovoAgendamento(@RequestBody Agendamento novoAgendamento) {

        boolean sucesso = agendaService.tentarAgendar(novoAgendamento);

        if (sucesso) {
            return "✅ Sucesso! Agendamento confirmado para " + novoAgendamento.getNomeCliente();
        } else {
            return "❌ Este horário já está ocupado. Por favor, escolha outro.";
        }
    }

    @PostMapping("/bloqueios")
    public String criarBloqueio(@RequestBody BloqueioAgenda bloqueio) {
        agendaService.adicionarBloqueio(bloqueio);
        return "✅ Horário bloqueado com sucesso! Motivo: " + bloqueio.getMotivo();
    }

    // Usamos @GetMapping porque o Postman vai apenas PEDIR uma informação, não salvar nada novo.
    // A URL final será: /api/agendamentos/livres
    @GetMapping("/livres")
    public List<LocalTime> consultarHorariosLivres(
            @RequestParam LocalDate data,
            @RequestParam Long servicoId) {

        return agendaService.buscarHorariosLivres(data, servicoId);
    }

    // O dono da barbearia vai acessar essa URL para carregar a tela
    @GetMapping
    public List<Agendamento> listarAgendamentos() {
        return agendaService.listarTodosOsAgendamentos();
    }

    // Usamos @PutMapping porque estamos ATUALIZANDO um dado existente, não criando um novo.
    // A URL vai ficar tipo: /api/agendamentos/1/cancelar
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
}