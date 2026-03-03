package com.barber.agendamento_bot.api.service;

import com.barber.agendamento_bot.api.entity.Agendamento;
import com.barber.agendamento_bot.api.entity.BloqueioAgenda;
import com.barber.agendamento_bot.api.entity.HorarioFuncionamento;
import com.barber.agendamento_bot.api.entity.Servico;
import com.barber.agendamento_bot.api.repository.AgendamentoRepository;
import com.barber.agendamento_bot.api.repository.BloqueioAgendaRepository;
import com.barber.agendamento_bot.api.repository.HorarioRepository;
import com.barber.agendamento_bot.api.repository.ServicoRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class AgendaService {

    private final AgendamentoRepository agendamentoRepository;
    private final ServicoRepository servicoRepository;
    private final BloqueioAgendaRepository bloqueioAgendaRepository;
    private final HorarioRepository horarioRepository;

    public AgendaService(AgendamentoRepository agendamentoRepository, ServicoRepository servicoRepository, BloqueioAgendaRepository bloqueioAgendaRepository, HorarioRepository horarioRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.servicoRepository = servicoRepository;
        this.bloqueioAgendaRepository = bloqueioAgendaRepository;
        this.horarioRepository = horarioRepository;
    }

    public boolean tentarAgendar(Agendamento novo) {
        Servico servicoCompleto = servicoRepository.findById(novo.getServicoEscolhido().getId()).orElseThrow();
        novo.setServicoEscolhido(servicoCompleto);

        LocalDateTime inicio = novo.getDataHoraInicio();
        LocalDateTime fim = inicio.plusMinutes(servicoCompleto.getDuracaoMinutos());

        novo.setValorFinal(servicoCompleto.getPreco());
        novo.setDataHoraFim(fim);
        novo.setStatus("CONFIRMADO");

        // BLINDAGEM CONTRA O PASSADO
        ZoneId fusoBR = ZoneId.of("America/Sao_Paulo");
        if (inicio.isBefore(LocalDateTime.now(fusoBR))) {
            return false;
        }

        // REGRA 1: Verifica a grade
        int diaSemanaId = inicio.getDayOfWeek().getValue();
        HorarioFuncionamento regrasDoDia = horarioRepository.findById(diaSemanaId).orElse(null);

        if (regrasDoDia == null || regrasDoDia.isFechado()) {
            return false;
        }

        LocalTime aberturaDoDia = LocalTime.parse(regrasDoDia.getHoraAbertura());
        LocalTime fechamentoDoDia = LocalTime.parse(regrasDoDia.getHoraFechamento());

        if (inicio.toLocalTime().isBefore(aberturaDoDia) || fim.toLocalTime().isAfter(fechamentoDoDia)) {
            return false;
        }

        // REGRA 2: Bloqueios manuais por período
        List<BloqueioAgenda> bloqueios = bloqueioAgendaRepository.findAll();
        for (BloqueioAgenda bloqueio : bloqueios) {
            if (bloqueio.getDataHoraInicio() == null || bloqueio.getDataHoraFim() == null) continue;

            if (inicio.isBefore(bloqueio.getDataHoraFim()) && fim.isAfter(bloqueio.getDataHoraInicio())) {
                return false;
            }
        }

        // REGRA 3: Conflito com outros clientes agendados
        List<Agendamento> noBanco = agendamentoRepository.findByStatusNot("CANCELADO");
        for (Agendamento existente : noBanco) {
            if (inicio.isBefore(existente.getDataHoraFim()) && fim.isAfter(existente.getDataHoraInicio())) {
                return false;
            }
        }

        agendamentoRepository.save(novo);
        return true;
    }

    public List<LocalTime> buscarHorariosLivres(LocalDate dataBuscada, Long servicoId) {
        List<LocalTime> horariosDisponiveis = new ArrayList<>();

        int diaSemanaId = dataBuscada.getDayOfWeek().getValue();
        HorarioFuncionamento regrasDoDia = horarioRepository.findById(diaSemanaId).orElse(null);

        if (regrasDoDia == null || regrasDoDia.isFechado()) {
            return horariosDisponiveis;
        }

        LocalTime aberturaDoDia = LocalTime.parse(regrasDoDia.getHoraAbertura());
        LocalTime fechamentoDoDia = LocalTime.parse(regrasDoDia.getHoraFechamento());

        Servico servicoEscolhido = servicoRepository.findById(servicoId).orElseThrow();
        int duracao = servicoEscolhido.getDuracaoMinutos();

        // ✨ AS DUAS TABELAS DE VERIFICAÇÃO (Agendamentos e Bloqueios)
        List<Agendamento> todosNoBanco = agendamentoRepository.findByStatusNot("CANCELADO");
        List<BloqueioAgenda> todosBloqueios = bloqueioAgendaRepository.findAll();

        ZoneId fusoBR = ZoneId.of("America/Sao_Paulo");
        LocalDate dataDeHoje = LocalDate.now(fusoBR);
        LocalTime horaAtual = LocalTime.now(fusoBR);

        LocalTime horarioTeste = aberturaDoDia;

        while (horarioTeste.plusMinutes(duracao).compareTo(fechamentoDoDia) <= 0) {

            if (dataBuscada.equals(dataDeHoje) && horarioTeste.isBefore(horaAtual.plusMinutes(15))) {
                horarioTeste = horarioTeste.plusMinutes(30);
                continue;
            }

            LocalDateTime inicioTentativa = LocalDateTime.of(dataBuscada, horarioTeste);
            LocalDateTime fimTentativa = inicioTentativa.plusMinutes(duracao);
            boolean temConflito = false;

            // 1. Verifica choque com outros clientes
            for (Agendamento existente : todosNoBanco) {
                if (existente.getDataHoraInicio().toLocalDate().equals(dataBuscada)) {
                    if (inicioTentativa.isBefore(existente.getDataHoraFim()) && fimTentativa.isAfter(existente.getDataHoraInicio())) {
                        temConflito = true;
                        break;
                    }
                }
            }

            // ✨ 2. Verifica choque com bloqueios manuais (Ex: Almoço / Consulta)
            if (!temConflito) {
                for (BloqueioAgenda bloqueio : todosBloqueios) {
                    if (bloqueio.getDataHoraInicio() == null || bloqueio.getDataHoraFim() == null) continue;

                    if (bloqueio.getDataHoraInicio().toLocalDate().equals(dataBuscada)) {
                        if (inicioTentativa.isBefore(bloqueio.getDataHoraFim()) && fimTentativa.isAfter(bloqueio.getDataHoraInicio())) {
                            temConflito = true;
                            break;
                        }
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
        return bloqueioAgendaRepository.save(novoBloqueio);
    }

    public void concluirAgendamento(Long id) {
        Agendamento agendamento = agendamentoRepository.findById(id).orElseThrow(() -> new RuntimeException("Agendamento não encontrado!"));
        agendamento.setStatus("CONCLUIDO");
        agendamentoRepository.save(agendamento);
    }

    public void atualizarValor(Long id, java.math.BigDecimal novoValor) {
        Agendamento agendamento = agendamentoRepository.findById(id).orElseThrow(() -> new RuntimeException("Agendamento não encontrado!"));
        agendamento.setValorFinal(novoValor);
        agendamentoRepository.save(agendamento);
    }

    public java.util.List<BloqueioAgenda> listarBloqueios() {
        return bloqueioAgendaRepository.findAll();
    }
}