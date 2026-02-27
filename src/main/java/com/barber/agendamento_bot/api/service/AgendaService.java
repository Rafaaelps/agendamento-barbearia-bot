package com.barber.agendamento_bot.api.service;

import com.barber.agendamento_bot.api.entity.Agendamento;
import com.barber.agendamento_bot.api.entity.BloqueioAgenda;
import com.barber.agendamento_bot.api.entity.Servico;
import com.barber.agendamento_bot.api.repository.AgendamentoRepository;
import com.barber.agendamento_bot.api.repository.BloqueioAgendaRepository;
import com.barber.agendamento_bot.api.repository.ServicoRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AgendaService {

    private final AgendamentoRepository agendamentoRepository;
    private final ServicoRepository servicoRepository;
    private final BloqueioAgendaRepository bloqueioAgendaRepository;

    // ✨ CONFIGURAÇÃO DO EXPEDIENTE (Basta mudar aqui!)
    // Exemplo: LocalTime.of(9, 30) significa 09:30 da manhã.
    private final LocalTime HORA_ABERTURA = LocalTime.of(8, 0);
    private final LocalTime HORA_FECHAMENTO = LocalTime.of(19, 0);

    public AgendaService(AgendamentoRepository agendamentoRepository, ServicoRepository servicoRepository, BloqueioAgendaRepository bloqueioAgendaRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.servicoRepository = servicoRepository;
        this.bloqueioAgendaRepository = bloqueioAgendaRepository;
    }

    public boolean tentarAgendar(Agendamento novo) {
        Servico servicoCompleto = servicoRepository.findById(novo.getServicoEscolhido().getId()).orElseThrow();
        novo.setServicoEscolhido(servicoCompleto);

        LocalDateTime inicio = novo.getDataHoraInicio();
        LocalDateTime fim = inicio.plusMinutes(servicoCompleto.getDuracaoMinutos());

        novo.setValorFinal(servicoCompleto.getPreco());
        novo.setDataHoraFim(fim);
        novo.setStatus("CONFIRMADO");

        // REGRA 1: Dias de folga fixos (Não atende de Domingo)
        if (inicio.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            System.out.println("❌ Lamento, domingo não tem atendimento.");
            return false;
        }

        // ✨ REGRA NOVA: Escudo de Expediente (Bloqueia a madrugada e o pós-expediente)
        if (inicio.toLocalTime().isBefore(HORA_ABERTURA) || fim.toLocalTime().isAfter(HORA_FECHAMENTO)) {
            System.out.println("❌ Horário fora do expediente de trabalho.");
            return false;
        }

        // REGRA 2: Bloqueios manuais por período
        List<BloqueioAgenda> bloqueios = bloqueioAgendaRepository.findAll();
        for (BloqueioAgenda bloqueio : bloqueios) {
            if (bloqueio.getDataHoraInicio() == null || bloqueio.getDataHoraFim() == null) continue;

            if (inicio.isBefore(bloqueio.getDataHoraFim()) && fim.isAfter(bloqueio.getDataHoraInicio())) {
                System.out.println("❌ Agendamento recusado: Cai dentro do bloqueio de " + bloqueio.getMotivo());
                return false;
            }
        }

        // REGRA 3: Conflito com outros clientes
        List<Agendamento> noBanco = agendamentoRepository.findByStatusNot("CANCELADO");
        for (Agendamento existente : noBanco) {
            if (inicio.isBefore(existente.getDataHoraFim()) && fim.isAfter(existente.getDataHoraInicio())) {
                System.out.println("❌ Horário já ocupado por outro cliente.");
                return false;
            }
        }

        agendamentoRepository.save(novo);
        return true;
    }

    public List<LocalTime> buscarHorariosLivres(LocalDate dataBuscada, Long servicoId) {
        List<LocalTime> horariosDisponiveis = new ArrayList<>();
        Servico servicoEscolhido = servicoRepository.findById(servicoId).orElseThrow();
        int duracao = servicoEscolhido.getDuracaoMinutos();
        List<Agendamento> todosNoBanco = agendamentoRepository.findByStatusNot("CANCELADO");

        // ✨ O robô agora usa as variáveis mestras para calcular a agenda!
        LocalTime horarioTeste = HORA_ABERTURA;

        while (horarioTeste.plusMinutes(duracao).compareTo(HORA_FECHAMENTO) <= 0) {
            LocalDateTime inicioTentativa = LocalDateTime.of(dataBuscada, horarioTeste);
            LocalDateTime fimTentativa = inicioTentativa.plusMinutes(duracao);
            boolean temConflito = false;

            for (Agendamento existente : todosNoBanco) {
                if (existente.getDataHoraInicio().toLocalDate().equals(dataBuscada)) {
                    if (inicioTentativa.isBefore(existente.getDataHoraFim()) && fimTentativa.isAfter(existente.getDataHoraInicio())) {
                        temConflito = true;
                        break;
                    }
                }
            }

            if (!temConflito) horariosDisponiveis.add(horarioTeste);
            horarioTeste = horarioTeste.plusMinutes(30);
        }

        return horariosDisponiveis;
    }

    public List<Agendamento> listarTodosOsAgendamentos() {
        return agendamentoRepository.findAll();
    }

    public void cancelarAgendamento(Long id) {
        Agendamento agendamento = agendamentoRepository.findById(id).orElseThrow();
        agendamento.setStatus("CANCELADO");
        agendamentoRepository.save(agendamento);
    }

    public Agendamento buscarAgendamentoAtivoPorTelefone(String telefone) {
        List<Agendamento> lista = agendamentoRepository.findByTelefoneClienteAndStatusNot(telefone, "CANCELADO");
        if (!lista.isEmpty()) return lista.get(0);
        return null;
    }

    public BloqueioAgenda adicionarBloqueio(BloqueioAgenda novoBloqueio) {
        System.out.println("🔒 Bloqueando agenda de: " + novoBloqueio.getDataHoraInicio() + " até " + novoBloqueio.getDataHoraFim() + " | Motivo: " + novoBloqueio.getMotivo());
        return bloqueioAgendaRepository.save(novoBloqueio);
    }

    public void concluirAgendamento(Long id) {
        Agendamento agendamento = agendamentoRepository.findById(id).orElseThrow(() -> new RuntimeException("Agendamento não encontrado!"));
        agendamento.setStatus("CONCLUIDO");
        agendamentoRepository.save(agendamento);
        System.out.println("💰 Serviço concluído com sucesso! ID: " + id);
    }

    public void atualizarValor(Long id, java.math.BigDecimal novoValor) {
        Agendamento agendamento = agendamentoRepository.findById(id).orElseThrow(() -> new RuntimeException("Agendamento não encontrado!"));
        agendamento.setValorFinal(novoValor);
        agendamentoRepository.save(agendamento);
        System.out.println("💸 Valor do agendamento " + id + " alterado para: " + novoValor);
    }

    public java.util.List<BloqueioAgenda> listarBloqueios() {
        return bloqueioAgendaRepository.findAll();
    }
}