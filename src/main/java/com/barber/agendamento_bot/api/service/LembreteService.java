package com.barber.agendamento_bot.api.service;

import com.barber.agendamento_bot.api.entity.Agendamento;
import com.barber.agendamento_bot.api.repository.AgendamentoRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LembreteService {

    private final AgendamentoRepository agendamentoRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private final String EVOLUTION_URL = "http://187.77.224.241:47851";
    private final String INSTANCE_NAME = "barbearia";
    private final String API_KEY = "EAlUBkxSKCsYF9mSWGZYxTfTF6qXGD4m";

    public LembreteService(AgendamentoRepository agendamentoRepository) {
        this.agendamentoRepository = agendamentoRepository;
    }

    // ⏰ CRON 1: ENVIA O LEMBRETE (Faltando 35 min)
    @Scheduled(cron = "0 * * * * *")
    public void verificarEEnviarLembretes() {
        ZoneId fusoBR = ZoneId.of("America/Sao_Paulo");
        LocalDateTime agora = LocalDateTime.now(fusoBR);
        LocalDateTime daquiA35Minutos = agora.plusMinutes(35);

        List<Agendamento> agendamentos = agendamentoRepository
                .buscarAgendamentosParaLembrar("CONFIRMADO", agora, daquiA35Minutos);

        for (Agendamento agendamento : agendamentos) {
            boolean sucesso = enviarMensagemEvolution(agendamento, false);
            if (sucesso) {
                agendamento.setLembreteEnviado(true);
                agendamentoRepository.save(agendamento);
            }
        }
    }

    // 🪓 CRON 2: O CARRASCO (Cancela quem não respondeu até faltar 15 min)
    @Scheduled(cron = "0 * * * * *")
    public void verificarECancelarNaoConfirmados() {
        ZoneId fusoBR = ZoneId.of("America/Sao_Paulo");
        LocalDateTime agora = LocalDateTime.now(fusoBR);

        // Se falta 15 minutos e não confirmou, significa que passaram 20 minutos desde o lembrete!
        LocalDateTime daquiA15Minutos = agora.plusMinutes(15);

        List<Agendamento> agendamentos = agendamentoRepository
                .buscarNaoConfirmados("CONFIRMADO", agora, daquiA15Minutos);

        for (Agendamento agendamento : agendamentos) {
            try {
                // 1. Cancela no banco para liberar a vaga
                agendamento.setStatus("CANCELADO");
                agendamentoRepository.save(agendamento);

                // 2. Avisa o cliente do ocorrido
                enviarMensagemEvolution(agendamento, true);
                System.out.println("🪓 [CANCELADO] Cliente " + agendamento.getNomeCliente() + " não confirmou a tempo.");
            } catch (Exception e) {
                System.err.println("❌ Erro ao cancelar falta de confirmação: " + e.getMessage());
            }
        }
    }

    private boolean enviarMensagemEvolution(Agendamento agendamento, boolean isCancelamento) {
        String horaFormatada = agendamento.getDataHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm"));
        String mensagem = "";

        if (isCancelamento) {
            mensagem = "❌ *AGENDAMENTO CANCELADO*\n\nOlá, *" + agendamento.getNomeCliente() + "*. Como não recebemos a sua confirmação, o seu horário às *" + horaFormatada + "* foi liberado automaticamente na agenda para outro cliente.\n\nSe quiser remarcar um novo horário, basta mandar um *Oi*!";
        } else {
            mensagem = "⏳ Olá, *" + agendamento.getNomeCliente() + "*! O seu horário às *" + horaFormatada + "* está se aproximando.\n\n⚠️ *ATENÇÃO:* Responda *SIM* nos próximos 20 minutos para confirmar sua presença, caso contrário o sistema cancelará o agendamento automaticamente para liberar a vaga!";
        }

        String numeroLimpo = agendamento.getTelefoneCliente().replaceAll("[^0-9]", "");
        if (!numeroLimpo.startsWith("55")) numeroLimpo = "55" + numeroLimpo;

        String urlDeDisparo = EVOLUTION_URL + "/message/sendText/" + INSTANCE_NAME;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", API_KEY);

        Map<String, Object> corpoRequisicao = new HashMap<>();
        corpoRequisicao.put("number", numeroLimpo);
        corpoRequisicao.put("text", mensagem);

        Map<String, String> textMessage = new HashMap<>();
        textMessage.put("text", mensagem);
        corpoRequisicao.put("textMessage", textMessage);

        HttpEntity<Map<String, Object>> pacote = new HttpEntity<>(corpoRequisicao, headers);

        try {
            ResponseEntity<String> resposta = restTemplate.postForEntity(urlDeDisparo, pacote, String.class);
            return true;
        } catch (HttpClientErrorException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}