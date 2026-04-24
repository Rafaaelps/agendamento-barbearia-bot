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

    private Usuario getUsuarioLogado() {
        String login = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByLogin(login).orElse(null);
    }

    public boolean tentarAgendar(Agendamento novo) {
        Servico servicoCompleto = servicoRepository.findById(novo.getServicoEscolhido().getId()).orElseThrow();
        novo.setServicoEscolhido(servicoCompleto);

        Usuario barbeiro = novo.getDonoDoRegistro();
        if (barbeiro == null) return false;

        LocalDateTime inicio = novo.getDataHoraInicio();
        LocalDateTime fim = inicio.plusMinutes(servicoCompleto.getDuracaoMinutos());

        novo.setValorFinal(servicoCompleto.getPreco());
        novo.setDataHoraFim(fim);
        novo.setStatus("AGENDADO");

        ZoneId fusoBR = ZoneId.of("America/Sao_Paulo");
        if (inicio.isBefore(LocalDateTime.now(fusoBR))) return false;

        int diaSemanaId = inicio.getDayOfWeek().getValue();
        HorarioFuncionamento regrasDoDia = horarioRepository.findByDiaDaSemanaAndDonoDoRegistro(diaSemanaId, barbeiro).orElse(null);
        if (regrasDoDia == null || regrasDoDia.isFechado()) return false;

        LocalTime aberturaDoDia = LocalTime.parse(regrasDoDia.getHoraAbertura());
        LocalTime fechamentoDoDia = LocalTime.parse(regrasDoDia.getHoraFechamento());

        if (inicio.toLocalTime().isBefore(aberturaDoDia) || fim.toLocalTime().isAfter(fechamentoDoDia)) return false;

        List<BloqueioAgenda> bloqueios = bloqueioAgendaRepository.findAllByDonoDoRegistro(barbeiro);
        for (BloqueioAgenda bloqueio : bloqueios) {
            if (bloqueio.getDataHoraInicio() == null || bloqueio.getDataHoraFim() == null) continue;
            if (inicio.isBefore(bloqueio.getDataHoraFim()) && fim.isAfter(bloqueio.getDataHoraInicio())) return false;
        }

        List<Agendamento> noBanco = agendamentoRepository.findByStatusNotAndDonoDoRegistro("CANCELADO", barbeiro);
        for (Agendamento existente : noBanco) {
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
        novo.setDonoDoRegistro(logado);
        LocalDateTime inicio = novo.getDataHoraInicio();
        LocalDateTime fim = inicio.plusMinutes(servicoCompleto.getDuracaoMinutos());
        novo.setValorFinal(servicoCompleto.getPreco()); novo.setDataHoraFim(fim); novo.setStatus("AGENDADO");
        novo.setFaturamentoBarbeiro(java.math.BigDecimal.ZERO); novo.setFormaPagamento("PENDENTE");
        agendamentoRepository.save(novo);
        logService.registrarAcao("AGENDA", "ENCAIXE", "Realizou encaixe manual para: " + novo.getNomeCliente());
    }

    public List<LocalTime> buscarHorariosLivres(LocalDate dataBuscada, Long servicoId, Usuario barbeiro) {
        List<LocalTime> horariosDisponiveis = new ArrayList<>();
        if (barbeiro == null) return horariosDisponiveis;

        int diaSemanaId = dataBuscada.getDayOfWeek().getValue();
        HorarioFuncionamento regrasDoDia = horarioRepository.findByDiaDaSemanaAndDonoDoRegistro(diaSemanaId, barbeiro).orElse(null);

        if (regrasDoDia == null || regrasDoDia.isFechado()) return horariosDisponiveis;

        LocalTime aberturaDoDia = LocalTime.parse(regrasDoDia.getHoraAbertura());
        LocalTime fechamentoDoDia = LocalTime.parse(regrasDoDia.getHoraFechamento());

        Servico servicoEscolhido = servicoRepository.findById(servicoId).orElseThrow();
        int duracao = servicoEscolhido.getDuracaoMinutos();

        List<Agendamento> todosNoBanco = agendamentoRepository.findByStatusNotAndDonoDoRegistro("CANCELADO", barbeiro);
        List<BloqueioAgenda> todosBloqueios = bloqueioAgendaRepository.findAllByDonoDoRegistro(barbeiro);

        ZoneId fusoBR = ZoneId.of("America/Sao_Paulo");
        LocalDate dataDeHoje = LocalDate.now(fusoBR);
        LocalTime horaAtual = LocalTime.now(fusoBR);

        LocalTime horarioTeste = aberturaDoDia;

        while (horarioTeste.plusMinutes(duracao).compareTo(fechamentoDoDia) <= 0) {
            if (dataBuscada.equals(dataDeHoje) && horarioTeste.isBefore(horaAtual.plusMinutes(15))) {
                horarioTeste = horarioTeste.plusMinutes(30); continue;
            }

            LocalDateTime inicioTentativa = LocalDateTime.of(dataBuscada, horarioTeste);
            LocalDateTime fimTentativa = inicioTentativa.plusMinutes(duracao);
            boolean temConflito = false;

            for (Agendamento existente : todosNoBanco) {
                if (existente.getDataHoraInicio().toLocalDate().equals(dataBuscada)) {
                    if (inicioTentativa.isBefore(existente.getDataHoraFim()) && fimTentativa.isAfter(existente.getDataHoraInicio())) {
                        temConflito = true; break;
                    }
                }
            }

            if (!temConflito) {
                for (BloqueioAgenda bloqueio : todosBloqueios) {
                    if (bloqueio.getDataHoraInicio() == null || bloqueio.getDataHoraFim() == null) continue;
                    if (bloqueio.getDataHoraInicio().toLocalDate().equals(dataBuscada)) {
                        if (inicioTentativa.isBefore(bloqueio.getDataHoraFim()) && fimTentativa.isAfter(bloqueio.getDataHoraInicio())) {
                            temConflito = true; break;
                        }
                    }
                }
            }
            if (!temConflito) horariosDisponiveis.add(horarioTeste);
            horarioTeste = horarioTeste.plusMinutes(30);
        }
        return horariosDisponiveis;
    }

    public void confirmarPresenca(Long id) { Agendamento ag = agendamentoRepository.findById(id).orElseThrow(); ag.setStatus("CONFIRMADO"); agendamentoRepository.save(ag); }
    public void concluirAgendamento(Long id) { Agendamento ag = agendamentoRepository.findById(id).orElseThrow(); ag.setStatus("CONCLUIDO"); agendamentoRepository.save(ag); }
    public void cancelarAgendamento(Long id) { Agendamento ag = agendamentoRepository.findById(id).orElseThrow(); ag.setStatus("CANCELADO"); agendamentoRepository.save(ag); logService.registrarAcao("AGENDA", "CANCELAMENTO", "Cancelou o agendamento de: " + ag.getNomeCliente()); }
    public void atualizarValor(Long id, java.math.BigDecimal novoValor) { Agendamento ag = agendamentoRepository.findById(id).orElseThrow(); ag.setValorFinal(novoValor); agendamentoRepository.save(ag); }

    public Agendamento buscarAgendamentoAtivoPorTelefone(String telefone) {
        List<Agendamento> lista = agendamentoRepository.findByTelefoneClienteAndStatusNot(telefone, "CANCELADO");
        ZoneId fusoBR = ZoneId.of("America/Sao_Paulo");
        LocalDateTime agora = LocalDateTime.now(fusoBR);
        Agendamento proximoAgendamento = null;
        for (Agendamento ag : lista) {
            if (ag.getDataHoraInicio().isAfter(agora) && ("CONFIRMADO".equals(ag.getStatus()) || "AGENDADO".equals(ag.getStatus()))) {
                if (proximoAgendamento == null || ag.getDataHoraInicio().isBefore(proximoAgendamento.getDataHoraInicio())) proximoAgendamento = ag;
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

    public void removerBloqueio(Long id) {
        bloqueioAgendaRepository.deleteById(id);
    }

    // ✨ FUNÇÕES QUE ESTAVAM FALTANDO
    public List<Agendamento> listarTodosOsAgendamentos() {
        Usuario logado = getUsuarioLogado();
        if (logado == null) return new ArrayList<>();

        if (logado.getPerfil().equals("ADMIN") || logado.getPerfil().equals("ROLE_ADMIN")) {
            return agendamentoRepository.findByStatusNot("CANCELADO");
        }
        return agendamentoRepository.findByStatusNotAndDonoDoRegistro("CANCELADO", logado);
    }

    public List<BloqueioAgenda> listarBloqueios() {
        Usuario logado = getUsuarioLogado();
        if (logado == null) return new ArrayList<>();

        if (logado.getPerfil().equals("ADMIN") || logado.getPerfil().equals("ROLE_ADMIN")) {
            return bloqueioAgendaRepository.findAll();
        }
        return bloqueioAgendaRepository.findAllByDonoDoRegistro(logado);
    }
}