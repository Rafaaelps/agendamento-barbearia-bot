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
import java.util.ArrayList;
import java.util.List;

@Service
public class AgendaService {

    private final AgendamentoRepository agendamentoRepository;
    private final ServicoRepository servicoRepository;
    private final BloqueioAgendaRepository bloqueioAgendaRepository;
    private final HorarioRepository horarioRepository; // ✨ NOVO: Conexão com a grade de horários!

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

        // ==========================================================
        // ✨ REGRA 1 (DINÂMICA): Busca o dia da semana e verifica a grade
        // 1 = Segunda, 2 = Terça ... 7 = Domingo
        // ==========================================================
        int diaSemanaId = inicio.getDayOfWeek().getValue();
        HorarioFuncionamento regrasDoDia = horarioRepository.findById(diaSemanaId).orElse(null);

        // Se não achar o dia no banco ou o dia estiver marcado como FECHADO na tela do painel
        if (regrasDoDia == null || regrasDoDia.isFechado()) {
            System.out.println("❌ Lamento, estamos fechados neste dia (" + regrasDoDia.getNomeDia() + ").");
            return false;
        }

        // Converte a String "09:00" do banco para o formato de tempo do Java
        LocalTime aberturaDoDia = LocalTime.parse(regrasDoDia.getHoraAbertura());
        LocalTime fechamentoDoDia = LocalTime.parse(regrasDoDia.getHoraFechamento());

        // Verifica se tentou agendar antes de abrir ou se o corte vai terminar depois de fechar
        if (inicio.toLocalTime().isBefore(aberturaDoDia) || fim.toLocalTime().isAfter(fechamentoDoDia)) {
            System.out.println("❌ Horário fora do expediente de trabalho deste dia.");
            return false;
        }

        // REGRA 2: Bloqueios manuais por período (Ex: Almoço)
        List<BloqueioAgenda> bloqueios = bloqueioAgendaRepository.findAll();
        for (BloqueioAgenda bloqueio : bloqueios) {
            if (bloqueio.getDataHoraInicio() == null || bloqueio.getDataHoraFim() == null) continue;

            if (inicio.isBefore(bloqueio.getDataHoraFim()) && fim.isAfter(bloqueio.getDataHoraInicio())) {
                System.out.println("❌ Agendamento recusado: Cai dentro do bloqueio de " + bloqueio.getMotivo());
                return false;
            }
        }

        // REGRA 3: Conflito com outros clientes agendados
        List<Agendamento> noBanco = agendamentoRepository.findByStatusNot("CANCELADO");
        for (Agendamento existente : noBanco) {
            if (inicio.isBefore(existente.getDataHoraFim()) && fim.isAfter(existente.getDataHoraInicio())) {
                System.out.println("❌ Horário já ocupado por outro cliente.");
                return false;
            }
        }

        // Se passou por todas as barreiras, salva no banco!
        agendamentoRepository.save(novo);
        return true;
    }

    public List<LocalTime> buscarHorariosLivres(LocalDate dataBuscada, Long servicoId) {
        List<LocalTime> horariosDisponiveis = new ArrayList<>();

        // Pega as regras de funcionamento para a data solicitada
        int diaSemanaId = dataBuscada.getDayOfWeek().getValue();
        HorarioFuncionamento regrasDoDia = horarioRepository.findById(diaSemanaId).orElse(null);

        // Se o barbeiro desligou esse dia no painel, retorna a lista vazia
        if (regrasDoDia == null || regrasDoDia.isFechado()) {
            return horariosDisponiveis;
        }

        LocalTime aberturaDoDia = LocalTime.parse(regrasDoDia.getHoraAbertura());
        LocalTime fechamentoDoDia = LocalTime.parse(regrasDoDia.getHoraFechamento());

        Servico servicoEscolhido = servicoRepository.findById(servicoId).orElseThrow();
        int duracao = servicoEscolhido.getDuracaoMinutos();
        List<Agendamento> todosNoBanco = agendamentoRepository.findByStatusNot("CANCELADO");

        // ✨ O laço agora começa na hora de abertura DINÂMICA do dia
        LocalTime horarioTeste = aberturaDoDia;

        while (horarioTeste.plusMinutes(duracao).compareTo(fechamentoDoDia) <= 0) {
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
            horarioTeste = horarioTeste.plusMinutes(30); // Pula de 30 em 30 min
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