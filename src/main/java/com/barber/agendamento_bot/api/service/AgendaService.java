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

    public AgendaService(AgendamentoRepository agendamentoRepository, ServicoRepository servicoRepository, BloqueioAgendaRepository bloqueioAgendaRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.servicoRepository = servicoRepository;
        this.bloqueioAgendaRepository = bloqueioAgendaRepository;
    }

    public boolean tentarAgendar(Agendamento novo) {
        // 1. Completa os dados: busca o serviço no banco para saber a duração real
        Servico servicoCompleto = servicoRepository.findById(novo.getServicoEscolhido().getId()).orElseThrow();
        novo.setServicoEscolhido(servicoCompleto);

        // 2. A MÁGICA ACONTECE AQUI: Calcula a hora do fim antes de qualquer coisa!
        LocalDateTime inicio = novo.getDataHoraInicio();
        LocalDateTime fim = inicio.plusMinutes(servicoCompleto.getDuracaoMinutos()); // Soma os minutos

        // Guarda a hora calculada dentro do agendamento
        novo.setDataHoraFim(fim);
        novo.setStatus("CONFIRMADO");

        // Novas regras de agendamento

        // REGRA 1: Dias de folga fixos (Não atende de Domingo)
        if (inicio.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            System.out.println("❌ Lamento, domingo não tem atendimento.");
            return false;
        }

        // REGRA 2: Bloqueios manuais (Verifica se o barbeiro travou esse horário)
        if (bloqueioAgendaRepository.existsByDataHoraBloqueada(inicio)) {
            System.out.println("❌ Horário já ocupado por outro cliente.");
            return false;
        }
        // =========================================================

        // 3. Busca os agendamentos já existentes para checar conflito
        List<Agendamento> noBanco = agendamentoRepository.findByStatusNot("CANCELADO");

        for (Agendamento existente : noBanco) {
            // Agora sim! Ele compara usando as variáveis que acabamos de calcular, e não um valor nulo.
            if (inicio.isBefore(existente.getDataHoraFim()) &&
                    fim.isAfter(existente.getDataHoraInicio())) {
                System.out.println("❌ Horário já ocupado por outro cliente.");
                return false;
            }
        }

        // 4. Se passou pelo For e por todas as regras sem conflito, salva no PostgreSQL!
        agendamentoRepository.save(novo);
        return true;
    }

    public List<LocalTime> buscarHorariosLivres(LocalDate dataBuscada, Long servicoId) {

        List<LocalTime> horariosDisponiveis = new ArrayList<>();

        // 1. Descobre a duração do serviço que o cliente quer
        Servico servicoEscolhido = servicoRepository.findById(servicoId).orElseThrow();
        int duracao = servicoEscolhido.getDuracaoMinutos();

        // 2. Busca todos os agendamentos já marcados no banco
        List<Agendamento> todosNoBanco = agendamentoRepository.findByStatusNot("CANCELADO");

        // Regras do expediente
        LocalTime horarioTeste = LocalTime.of(8, 0); // Começa às 08:00
        LocalTime fimExpediente = LocalTime.of(18, 0); // Vai até as 18:00

        // 3. O Loop que varre o dia pulando de 30 em 30 min
        while (horarioTeste.plusMinutes(duracao).compareTo(fimExpediente) <= 0) {

            LocalDateTime inicioTentativa = LocalDateTime.of(dataBuscada, horarioTeste);
            LocalDateTime fimTentativa = inicioTentativa.plusMinutes(duracao);
            boolean temConflito = false;

            for (Agendamento existente : todosNoBanco) {
                // Só verifica se for no mesmo dia!
                if (existente.getDataHoraInicio().toLocalDate().equals(dataBuscada)) {

                    // A regra de sobreposição que você já domina
                    if (inicioTentativa.isBefore(existente.getDataHoraFim()) &&
                            fimTentativa.isAfter(existente.getDataHoraInicio())) {
                        temConflito = true;
                        break; // Bateu horário? Pula pro próximo teste!
                    }
                }
            }

            // Se sobreviveu ao teste sem conflitos, é uma vaga livre!
            if (!temConflito) {
                horariosDisponiveis.add(horarioTeste);
            }

            // Avança o relógio em 30 minutos
            horarioTeste = horarioTeste.plusMinutes(30);
        }

        return horariosDisponiveis;
    }

    public List<Agendamento> listarTodosOsAgendamentos() {
        // Retorna todos, do mais antigo para o mais novo
        return agendamentoRepository.findAll();
    }


    public void cancelarAgendamento(Long id) {
        // Busca o agendamento no banco. Se não achar, dá erro.
        Agendamento agendamento = agendamentoRepository.findById(id).orElseThrow();

        // Muda o status e salva novamente!
        agendamento.setStatus("CANCELADO");
        agendamentoRepository.save(agendamento);
    }


    public Agendamento buscarAgendamentoAtivoPorTelefone(String telefone) {
        // Busca se o cliente tem agendamentos que NÃO estão cancelados
        List<Agendamento> lista = agendamentoRepository.findByTelefoneClienteAndStatusNot(telefone, "CANCELADO");

        if (!lista.isEmpty()) {
            return lista.get(0); // Devolve o primeiro que encontrar
        }
        return null; // Se a lista estiver vazia, retorna nulo
    }


    // MÉTOD PARA O BARBEIRO BLOQUEAR HORÁRIOS

    public BloqueioAgenda adicionarBloqueio(BloqueioAgenda novoBloqueio) {
        System.out.println("🔒 Bloqueando agenda para: " + novoBloqueio.getDataHoraBloqueada() + " | Motivo: " + novoBloqueio.getMotivo());
        return bloqueioAgendaRepository.save(novoBloqueio);
    }

    public void concluirAgendamento(Long id) {
        // 1. Busca o agendamento no banco de dados
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado!"));

        // 2. Muda o status para concluído
        agendamento.setStatus("CONCLUIDO");

        // 3. Salva a atualização
        agendamentoRepository.save(agendamento);
        System.out.println("💰 Serviço concluído com sucesso! ID: " + id);
    }
}