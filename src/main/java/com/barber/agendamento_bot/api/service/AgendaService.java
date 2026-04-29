package com.barber.agendamento_bot.api.service;

import com.barber.agendamento_bot.api.entity.*;
import com.barber.agendamento_bot.api.repository.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class AgendaService {

    private final AgendamentoRepository agendamentoRepository;
    private final HorarioRepository horarioRepository;
    private final ServicoRepository servicoRepository;
    private final BloqueioAgendaRepository bloqueioAgendaRepository;

    public AgendaService(AgendamentoRepository agendamentoRepository, HorarioRepository horarioRepository, ServicoRepository servicoRepository, BloqueioAgendaRepository bloqueioAgendaRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.horarioRepository = horarioRepository;
        this.servicoRepository = servicoRepository;
        this.bloqueioAgendaRepository = bloqueioAgendaRepository;
    }

    public List<Agendamento> listarTodosOsAgendamentos() {
        return agendamentoRepository.findAll();
    }

    public List<BloqueioAgenda> listarBloqueios() {
        return bloqueioAgendaRepository.findAll();
    }

    public void forcarAgendamento(Agendamento agendamento) {
        Servico s = servicoRepository.findById(agendamento.getServicoEscolhido().getId()).orElseThrow();
        agendamento.setDataHoraFim(agendamento.getDataHoraInicio().plusMinutes(s.getDuracaoMinutos()));
        agendamento.setStatus("AGENDADO");
        agendamentoRepository.save(agendamento);
    }

    public void adicionarBloqueio(BloqueioAgenda bloqueio) {
        bloqueioAgendaRepository.save(bloqueio);
    }

    public void removerBloqueio(Long id) {
        bloqueioAgendaRepository.deleteById(id);
    }

    public void cancelarAgendamento(Long id) {
        agendamentoRepository.findById(id).ifPresent(ag -> {
            ag.setStatus("CANCELADO");
            agendamentoRepository.save(ag);
        });
    }

    public void concluirAgendamento(Long id) {
        agendamentoRepository.findById(id).ifPresent(ag -> {
            ag.setStatus("CONCLUIDO");
            agendamentoRepository.save(ag);
        });
    }

    public void atualizarValor(Long id, BigDecimal novoValor) {
        agendamentoRepository.findById(id).ifPresent(ag -> {
            ag.setValorFinal(novoValor);
            agendamentoRepository.save(ag);
        });
    }

    public void confirmarPresenca(Long id) {
        agendamentoRepository.findById(id).ifPresent(ag -> {
            ag.setStatus("CONFIRMADO");
            agendamentoRepository.save(ag);
        });
    }

    public boolean atingiuLimiteDiario(String telefone, LocalDate data) {
        LocalDateTime inicioDia = data.atStartOfDay();
        LocalDateTime fimDia = data.atTime(23, 59, 59);
        long total = agendamentoRepository.countByTelefoneClienteAndDataHoraInicioBetweenAndStatusIn(
                telefone, inicioDia, fimDia, Arrays.asList("AGENDADO", "CONFIRMADO")
        );
        return total >= 2;
    }

    public Agendamento buscarAgendamentoAtivoPorTelefone(String telefone) {
        List<Agendamento> ativos = agendamentoRepository.findByTelefoneClienteAndStatusInOrderByDataHoraInicioAsc(
                telefone, Arrays.asList("AGENDADO", "CONFIRMADO")
        );
        return ativos.isEmpty() ? null : ativos.get(0);
    }

    public List<LocalTime> buscarHorariosLivres(LocalDate dataBuscada, Long servicoId, Usuario barbeiro) {
        List<LocalTime> horariosDisponiveis = new ArrayList<>();
        if (barbeiro == null) return horariosDisponiveis;

        int diaSemanaId = dataBuscada.getDayOfWeek().getValue();
        HorarioFuncionamento regrasDoDia = horarioRepository.findByDiaDaSemanaAndDonoDoRegistro(diaSemanaId, barbeiro).orElse(null);

        if (regrasDoDia == null || regrasDoDia.isFechado()) return horariosDisponiveis;

        LocalTime abertura = LocalTime.parse(regrasDoDia.getHoraAbertura());
        LocalTime fechamento = LocalTime.parse(regrasDoDia.getHoraFechamento());

        Servico servico = servicoRepository.findById(servicoId).orElseThrow();
        int duracao = servico.getDuracaoMinutos();

        // ✨ CORREÇÃO CRÍTICA: Agora ele busca APENAS o que está ocupado de verdade.
        // Se o status for CONCLUIDO ou CANCELADO, ele não entra nessa lista e o horário fica LIVRE.
        List<Agendamento> ocupados = agendamentoRepository.findByDonoDoRegistroAndStatusIn(barbeiro, Arrays.asList("AGENDADO", "CONFIRMADO"));
        List<BloqueioAgenda> bloqueios = bloqueioAgendaRepository.findAllByDonoDoRegistro(barbeiro);

        ZoneId fusoBR = ZoneId.of("America/Sao_Paulo");
        LocalDateTime agora = LocalDateTime.now(fusoBR);
        LocalTime horarioTeste = abertura;

        while (true) {
            LocalTime fimTeste = horarioTeste.plusMinutes(duracao);
            if (fimTeste.isBefore(horarioTeste) || fimTeste.isAfter(fechamento)) break;

            LocalDateTime inicioCompleto = LocalDateTime.of(dataBuscada, horarioTeste);
            LocalDateTime fimCompleto = inicioCompleto.plusMinutes(duracao);

            // Bloqueia horários no passado
            if (inicioCompleto.isBefore(agora.plusMinutes(10))) {
                horarioTeste = horarioTeste.plusMinutes(30);
                continue;
            }

            boolean conflito = false;
            for (Agendamento ag : ocupados) {
                if (ag.getDataHoraInicio().toLocalDate().equals(dataBuscada)) {
                    if (inicioCompleto.isBefore(ag.getDataHoraFim()) && fimCompleto.isAfter(ag.getDataHoraInicio())) {
                        conflito = true; break;
                    }
                }
            }

            if (!conflito) {
                for (BloqueioAgenda bq : bloqueios) {
                    if (bq.getDataHoraInicio().toLocalDate().equals(dataBuscada)) {
                        if (inicioCompleto.isBefore(bq.getDataHoraFim()) && fimCompleto.isAfter(bq.getDataHoraInicio())) {
                            conflito = true; break;
                        }
                    }
                }
            }

            if (!conflito) horariosDisponiveis.add(horarioTeste);

            horarioTeste = horarioTeste.plusMinutes(30);
            if (horarioTeste.equals(LocalTime.MIDNIGHT)) break;
        }

        return horariosDisponiveis;
    }

    public boolean tentarAgendar(Agendamento agendamento) {
        List<LocalTime> livres = buscarHorariosLivres(
                agendamento.getDataHoraInicio().toLocalDate(),
                agendamento.getServicoEscolhido().getId(),
                agendamento.getDonoDoRegistro()
        );

        if (livres.contains(agendamento.getDataHoraInicio().toLocalTime())) {
            agendamento.setStatus("AGENDADO");
            Servico s = servicoRepository.findById(agendamento.getServicoEscolhido().getId()).orElseThrow();
            agendamento.setDataHoraFim(agendamento.getDataHoraInicio().plusMinutes(s.getDuracaoMinutos()));
            agendamentoRepository.save(agendamento);
            return true;
        }
        return false;
    }
}