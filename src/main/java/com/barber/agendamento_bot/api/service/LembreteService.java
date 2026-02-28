package com.barber.agendamento_bot.api.service;

import com.barber.agendamento_bot.api.entity.Agendamento;
import com.barber.agendamento_bot.api.repository.AgendamentoRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LembreteService {

    private final AgendamentoRepository agendamentoRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    // =======================================================
    // ⚙️ CONFIGURAÇÕES DA SUA EVOLUTION API
    // =======================================================
    private final String EVOLUTION_URL = "http://SEU_IP_DA_HOSTINGER:8080";
    private final String INSTANCE_NAME = "NOME_DA_SUA_INSTANCIA";
    private final String API_KEY = "SUA_GLOBAL_API_KEY_AQUI";

    public LembreteService(AgendamentoRepository agendamentoRepository) {
        this.agendamentoRepository = agendamentoRepository;
    }

    // Roda a cada 1 minuto
    @Scheduled(cron = "0 * * * * *")
    public void verificarEEnviarLembretes() {
        LocalDateTime agora = LocalDateTime.now();
        // ✨ Aumentamos a janela para 35 minutos de segurança (cobre o agendamento perfeitamente)
        LocalDateTime daquiA35Minutos = agora.plusMinutes(35);

        List<Agendamento> agendamentos = agendamentoRepository
                .buscarAgendamentosParaLembrar("CONFIRMADO", agora, daquiA35Minutos);

        for (Agendamento agendamento : agendamentos) {
            try {
                enviarMensagemEvolution(agendamento);

                // Carimba que já foi enviado
                agendamento.setLembreteEnviado(true);
                agendamentoRepository.save(agendamento);

            } catch (Exception e) {
                System.err.println("❌ Erro ao enviar lembrete via Evolution para " + agendamento.getNomeCliente() + ": " + e.getMessage());
            }
        }
    }

    private void enviarMensagemEvolution(Agendamento agendamento) {
        String horaFormatada = agendamento.getDataHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm"));
        String mensagem = "⏳ Olá, *" + agendamento.getNomeCliente() + "*! Passando para lembrar que o seu horário conosco é daqui a pouco, às *" + horaFormatada + "*. Já estamos te esperando! 💈";

        String numeroLimpo = agendamento.getTelefoneCliente().replaceAll("[^0-9]", "");
        if (!numeroLimpo.startsWith("55")) {
            numeroLimpo = "55" + numeroLimpo;
        }

        String urlDeDisparo = EVOLUTION_URL + "/message/sendText/" + INSTANCE_NAME;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", API_KEY); // Autenticação da Evolution

        // ✨ Pacote à prova de falhas para V1 e V2 da Evolution API
        Map<String, Object> corpoRequisicao = new HashMap<>();
        corpoRequisicao.put("number", numeroLimpo);
        corpoRequisicao.put("text", mensagem); // Lida com a Evolution V2

        Map<String, String> textMessage = new HashMap<>();
        textMessage.put("text", mensagem);
        corpoRequisicao.put("textMessage", textMessage); // Lida com a Evolution V1

        HttpEntity<Map<String, Object>> pacote = new HttpEntity<>(corpoRequisicao, headers);
        ResponseEntity<String> resposta = restTemplate.postForEntity(urlDeDisparo, pacote, String.class);

        System.out.println("🔔 [SUCESSO] Lembrete disparado para " + numeroLimpo + " | Status: " + resposta.getStatusCode());
    }
}