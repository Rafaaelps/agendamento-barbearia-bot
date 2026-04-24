package com.barber.agendamento_bot.api.service;

import com.barber.agendamento_bot.api.entity.*;
import com.barber.agendamento_bot.api.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
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

    // ✨ NOVAS INJEÇÕES
    private final UsuarioRepository usuarioRepository;
    private final LogService logService;

    public AgendaService(AgendamentoRepository agendamentoRepository, ServicoRepository servicoRepository, BloqueioAgendaRepository bloqueioAgendaRepository, HorarioRepository horarioRepository, UsuarioRepository usuarioRepository, LogService logService) {
        this.agendamentoRepository = agendamentoRepository;
        this.servicoRepository = servicoRepository;
        this.bloqueioAgendaRepository = bloqueioAgendaRepository;
        this.horarioRepository = horarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.logService = logService;
    }

    // ✨ FUNÇÃO AUXILIAR PARA DESCOBRIR QUEM ESTÁ LOGADO NO PAINEL
    private Usuario getUsuarioLogado() {
        String login = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByLogin(login).orElse(null);
    }

    public boolean tentarAgendar(Agendamento novo) {
        Servico servicoCompleto = servicoRepository.findById(novo.getServicoEscolhido().getId()).orElseThrow();
        novo.setServicoEscolhido(servicoCompleto);

        LocalDateTime inicio = novo.getDataHoraInicio();
        LocalDateTime fim = inicio.plusMinutes(servicoCompleto.getDuracaoMinutos());

        novo.setValorFinal(servicoCompleto.getPreco());
        novo.setDataHoraFim(fim);
        novo.setStatus("AGENDADO");

        ZoneId fusoBR = ZoneId.of("America/Sao_Paulo");
        if (inicio.isBefore(LocalDateTime.now(fusoBR))) return false;

        int diaSemanaId = inicio.getDayOfWeek().getValue();
        HorarioFuncionamento regrasDoDia = horarioRepository.findById(diaSemanaId).orElse(null);
        if (regrasDoDia == null || regrasDoDia.isFechado()) return false;

        LocalTime aberturaDoDia = LocalTime.parse(regrasDoDia.getHoraAbertura());
        LocalTime fechamentoDoDia = LocalTime.parse(regrasDoDia.getHoraFechamento());

        if (inicio.toLocalTime().isBefore(aberturaDoDia) || fim.toLocalTime().isAfter(fechamentoDoDia)) return false;

        List<BloqueioAgenda> bloqueios = bloqueioAgendaRepository.findAll();
        for (BloqueioAgenda bloqueio : bloqueios) {
            if (bloqueio.getDataHoraInicio() == null || bloqueio.getDataHoraFim() == null) continue;
            if (inicio.isBefore(bloqueio.getDataHoraFim()) && fim.isAfter(bloqueio.getDataHoraInicio())) return false;
        }

        List<Agendamento> noBanco = agendamentoRepository.findByStatusNot("CANCELADO");
        for (Agendamento existente : noBanco) {
            // O robô agenda direto no WhatsApp, então a checagem é global pro salão inteiro (ou adapte para o dono)
            if (inicio.isBefore(existente.getDataHoraFim()) && fim.isAfter(existente.getDataHoraInicio())) return false;
        }

        agendamentoRepository.save(novo);
        return true;
    }

    public void forcarAgendamento(Agendamento novo) {
        Usuario logado = getUsuarioLogado();
        if(logado == null) return;

        Servico servicoCompleto = servicoRepository.findById(novo.getServicoEscolhido().getId()).orElseThrow();
        novo.setServicoEscolhido(servicoCompleto);

        // ✨ ATRELA O ENCAIXE AO BARBEIRO LOGADO
        novo.setDonoDoRegistro(logado);

        LocalDateTime inicio = novo.getDataHoraInicio();
        LocalDateTime fim = inicio.plusMinutes(servicoCompleto.getDuracaoMinutos());

        novo.setValorFinal(servicoCompleto.getPreco());
        novo.setDataHoraFim(fim);
        novo.setStatus("AGENDADO");
        novo.setFaturamentoBarbeiro(java.math.BigDecimal.ZERO);
        novo.setFormaPagamento("PENDENTE");

        agendamentoRepository.save(novo);
        System.out.println("⚠️ Encaixe realizado manualmente pelo barbeiro: " + novo.getNomeCliente());

        // ✨ AUDITORIA
        logService.registrarAcao("AGENDA", "ENCAIXE", "Realizou encaixe manual de " + servicoCompleto.getNome() + " para: " + novo.getNomeCliente());
    }

    public void confirmarPresenca(Long id) {
        Agendamento agendamento = agendamentoRepository.findById(id).orElseThrow();
        agendamento.setStatus("CONFIRMADO");
        agendamentoRepository.save(agendamento);
    }

    public List<LocalTime> buscarHorariosLivres(LocalDate dataBuscada, Long servicoId) {
        List<LocalTime> horariosDisponiveis = new ArrayList<>();
        int diaSemanaId = dataBuscada.getDayOfWeek().getValue();
        HorarioFuncionamento regrasDoDia = horarioRepository.findById(diaSemanaId).orElse(null);

        if (regrasDoDia == null || regrasDoDia.isFechado()) return horariosDisponiveis;

        LocalTime aberturaDoDia = LocalTime.parse(regrasDoDia.getHoraAbertura());
        LocalTime fechamentoDoDia = LocalTime.parse(regrasDoDia.getHoraFechamento());

        Servico servicoEscolhido = servicoRepository.findById(servicoId).orElseThrow();
        int duracao = servicoEscolhido.getDuracaoMinutos();

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

            for (Agendamento existente : todosNoBanco) {
                if (existente.getDataHoraInicio().toLocalDate().equals(dataBuscada)) {
                    if (inicioTentativa.isBefore(existente.getDataHoraFim()) && fimTentativa.isAfter(existente.getDataHoraInicio())) {
                        temConflito = true;
                        break;
                    }
                }
            }

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

    // ✨ LISTAGEM ISOLADA POR BARBEIRO
    public List<Agendamento> listarTodosOsAgendamentos() {
        Usuario logado = getUsuarioLogado();
        if (logado == null) return new ArrayList<>();

        if (logado.getPerfil().equals("ADMIN") || logado.getPerfil().equals("ROLE_ADMIN")) {
            return agendamentoRepository.findByStatusNot("CANCELADO");
        }

        return agendamentoRepository.findByStatusNotAndDonoDoRegistro("CANCELADO", logado);
    }

    public void cancelarAgendamento(Long id) {
        Agendamento agendamento = agendamentoRepository.findById(id).orElseThrow();
        agendamento.setStatus("CANCELADO");
        agendamentoRepository.save(agendamento);
        logService.registrarAcao("AGENDA", "CANCELAMENTO", "Cancelou o agendamento de: " + agendamento.getNomeCliente());
    }

    public Agendamento buscarAgendamentoAtivoPorTelefone(String telefone) {
        List<Agendamento> lista = agendamentoRepository.findByTelefoneClienteAndStatusNot(telefone, "CANCELADO");
        ZoneId fusoBR = ZoneId.of("America/Sao_Paulo");
        LocalDateTime agora = LocalDateTime.now(fusoBR);
        Agendamento proximoAgendamento = null;

        for (Agendamento ag : lista) {
            if (ag.getDataHoraInicio().isAfter(agora) && ("CONFIRMADO".equals(ag.getStatus()) || "AGENDADO".equals(ag.getStatus()))) {
                if (proximoAgendamento == null || ag.getDataHoraInicio().isBefore(proximoAgendamento.getDataHoraInicio())) {
                    proximoAgendamento = ag;
                }
            }
        }
        return proximoAgendamento;
    }

    public BloqueioAgenda adicionarBloqueio(BloqueioAgenda novoBloqueio) {
        Usuario logado = getUsuarioLogado();
        if(logado != null) novoBloqueio.setDonoDoRegistro(logado);
        logService.registrarAcao("AGENDA", "BLOQUEIO", "Bloqueou agenda. Motivo: " + novoBloqueio.getMotivo());
        return bloqueioAgendaRepository.save(novoBloqueio);
    }

    public void concluirAgendamento(Long id) {
        Agendamento agendamento = agendamentoRepository.findById(id).orElseThrow();
        agendamento.setStatus("CONCLUIDO");
        agendamentoRepository.save(agendamento);
    }

    public void atualizarValor(Long id, java.math.BigDecimal novoValor) {
        Agendamento agendamento = agendamentoRepository.findById(id).orElseThrow();
        agendamento.setValorFinal(novoValor);
        agendamentoRepository.save(agendamento);
    }

    public List<BloqueioAgenda> listarBloqueios() { return bloqueioAgendaRepository.findAll(); }
}