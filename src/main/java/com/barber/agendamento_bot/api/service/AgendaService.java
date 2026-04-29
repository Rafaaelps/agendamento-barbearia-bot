package com.barber.agendamento_bot.api.service;

import com.barber.agendamento_bot.api.entity.Agendamento;
import com.barber.agendamento_bot.api.entity.BloqueioAgenda;
import com.barber.agendamento_bot.api.entity.HorarioFuncionamento;
import com.barber.agendamento_bot.api.entity.Servico;
import com.barber.agendamento_bot.api.entity.Usuario;
import com.barber.agendamento_bot.api.repository.AgendamentoRepository;
import com.barber.agendamento_bot.api.repository.BloqueioAgendaRepository;
import com.barber.agendamento_bot.api.repository.HorarioRepository;
import com.barber.agendamento_bot.api.repository.ServicoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    // ✨ REGRA 1: Verifica se o cliente já atingiu 2 agendamentos ativos no mesmo dia
    public boolean atingiuLimiteDiario(String telefone, LocalDate data) {
        LocalDateTime inicioDia = data.atStartOfDay();
        LocalDateTime fimDia = data.atTime(23, 59, 59);

        long total = agendamentoRepository.countByTelefoneClienteAndDataHoraInicioBetweenAndStatusIn(
                telefone, inicioDia, fimDia, Arrays.asList("AGENDADO", "CONFIRMADO")
        );
        return total >= 2;
    }

    // ✨ REGRA 2: Só pega agendamentos que ainda vão acontecer, ignorando concluídos
    public Agendamento buscarAgendamentoAtivoPorTelefone(String telefone) {
        List<Agendamento> ativos = agendamentoRepository.findByTelefoneClienteAndStatusInOrderByDataHoraInicioAsc(
                telefone, Arrays.asList("AGENDADO", "CONFIRMADO")
        );
        return ativos.isEmpty() ? null : ativos.get(0);
    }

    // ✨ REGRA 3: O Motor de Horários com a trava do "Loop Infinito" resolvida
    public List<LocalTime> buscarHorariosLivres(LocalDate dataBuscada, Long servicoId, Usuario barbeiro) {
        List<LocalTime> horariosDisponiveis = new ArrayList<>();
        if (barbeiro == null) return horariosDisponiveis;

        int diaSemanaId = dataBuscada.getDayOfWeek().getValue();
        HorarioFuncionamento regrasDoDia = horarioRepository.findByDiaDaSemanaAndDonoDoRegistro(diaSemanaId, barbeiro).orElse(null);

        if (regrasDoDia == null) {
            String[] dias = {"", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"};
            for (int i = 1; i <= 7; i++) {
                HorarioFuncionamento h = new HorarioFuncionamento();
                h.setDiaDaSemana(i);
                h.setNomeDia(dias[i]);
                h.setHoraAbertura("09:00");
                h.setHoraFechamento("19:00");
                h.setFechado(i == 7);
                h.setDonoDoRegistro(barbeiro);
                horarioRepository.save(h);
                if (i == diaSemanaId) regrasDoDia = h;
            }
        }

        if (regrasDoDia.isFechado()) return horariosDisponiveis;

        String horaAb = (regrasDoDia.getHoraAbertura() == null || regrasDoDia.getHoraAbertura().isEmpty()) ? "09:00" : regrasDoDia.getHoraAbertura();
        String horaFc = (regrasDoDia.getHoraFechamento() == null || regrasDoDia.getHoraFechamento().isEmpty()) ? "19:00" : regrasDoDia.getHoraFechamento();

        LocalTime aberturaDoDia = LocalTime.parse(horaAb);
        LocalTime fechamentoDoDia = LocalTime.parse(horaFc);

        Servico servicoEscolhido = servicoRepository.findById(servicoId).orElseThrow();
        int duracao = servicoEscolhido.getDuracaoMinutos();

        List<Agendamento> todosNoBanco = agendamentoRepository.findByStatusNotAndDonoDoRegistro("CANCELADO", barbeiro);
        List<BloqueioAgenda> todosBloqueios = bloqueioAgendaRepository.findAllByDonoDoRegistro(barbeiro);

        ZoneId fusoBR = ZoneId.of("America/Sao_Paulo");
        LocalDateTime momentoAtual = LocalDateTime.now(fusoBR);

        LocalTime horarioTeste = aberturaDoDia;

        while (true) {
            LocalTime fimTentativaTime = horarioTeste.plusMinutes(duracao);

            // Trava de Segurança contra a meia-noite (Evita o loop infinito)
            if (fimTentativaTime.isBefore(horarioTeste) || fimTentativaTime.isAfter(fechamentoDoDia)) {
                if (!fechamentoDoDia.equals(LocalTime.MIDNIGHT)) {
                    break;
                }
            }

            LocalDateTime inicioTentativaCompleto = LocalDateTime.of(dataBuscada, horarioTeste);
            LocalDateTime fimTentativaCompleta = inicioTentativaCompleto.plusMinutes(duracao);

            if (inicioTentativaCompleto.isBefore(momentoAtual.plusMinutes(15))) {
                LocalTime proximo = horarioTeste.plusMinutes(30);
                if (proximo.isBefore(horarioTeste)) break;
                horarioTeste = proximo;
                continue;
            }

            boolean temConflito = false;

            for (Agendamento existente : todosNoBanco) {
                if (existente.getDataHoraInicio().toLocalDate().equals(dataBuscada)) {
                    if (inicioTentativaCompleto.isBefore(existente.getDataHoraFim()) && fimTentativaCompleta.isAfter(existente.getDataHoraInicio())) {
                        temConflito = true; break;
                    }
                }
            }

            if (!temConflito) {
                for (BloqueioAgenda bloqueio : todosBloqueios) {
                    if (bloqueio.getDataHoraInicio() == null || bloqueio.getDataHoraFim() == null) continue;
                    if (bloqueio.getDataHoraInicio().toLocalDate().equals(dataBuscada)) {
                        if (inicioTentativaCompleto.isBefore(bloqueio.getDataHoraFim()) && fimTentativaCompleta.isAfter(bloqueio.getDataHoraInicio())) {
                            temConflito = true; break;
                        }
                    }
                }
            }

            if (!temConflito) horariosDisponiveis.add(horarioTeste);

            LocalTime proximo = horarioTeste.plusMinutes(30);
            if (proximo.isBefore(horarioTeste)) break;
            horarioTeste = proximo;
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