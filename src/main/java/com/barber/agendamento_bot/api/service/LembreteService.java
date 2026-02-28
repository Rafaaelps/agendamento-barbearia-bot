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

    // O "carteiro" embutido do Java para enviar dados para a internet
    private final RestTemplate restTemplate = new RestTemplate();

    // =======================================================
    // ⚙️ CONFIGURAÇÕES DA SUA EVOLUTION API (Altere aqui)
    // =======================================================
    private final String EVOLUTION_URL = "http://187.77.224.241:8080";
    private final String INSTANCE_NAME = "barbearia";
    private final String API_KEY = "EAlUBkxSKCsYF9mSWGZYxTfTF6qXGD4m";

    public LembreteService(AgendamentoRepository agendamentoRepository) {
        this.agendamentoRepository = agendamentoRepository;
    }

    // Roda silenciosamente a cada 1 minuto
    @Scheduled(cron = "0 * * * * *")
    public void verificarEEnviarLembretes() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime daquiA30Minutos = agora.plusMinutes(30);

        // Busca no banco os clientes que cortam daqui a meia hora e ainda não foram avisados
        List<Agendamento> agendamentos = agendamentoRepository
                .findByStatusAndLembreteEnviadoFalseAndDataHoraInicioBetween("CONFIRMADO", agora, daquiA30Minutos);

        for (Agendamento agendamento : agendamentos) {
            try {
                enviarMensagemEvolution(agendamento);

                // Carimba que já foi enviado para não mandar duas vezes no próximo minuto!
                agendamento.setLembreteEnviado(true);
                agendamentoRepository.save(agendamento);

            } catch (Exception e) {
                System.err.println("❌ Erro ao enviar lembrete via Evolution: " + e.getMessage());
            }
        }
    }

    private void enviarMensagemEvolution(Agendamento agendamento) {
        String horaFormatada = agendamento.getDataHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm"));

        // O texto que o cliente vai receber
        String mensagem = "⏳ Olá, *" + agendamento.getNomeCliente() + "*! Passando para lembrar que o seu horário conosco é daqui a pouco, às *" + horaFormatada + "*. Já estamos te esperando! 💈";

        // 1. Prepara o número de telefone (A Evolution exige apenas números e com o 55 do Brasil)
        String numeroLimpo = agendamento.getTelefoneCliente().replaceAll("[^0-9]", "");
        if (!numeroLimpo.startsWith("55")) {
            numeroLimpo = "55" + numeroLimpo;
        }

        // 2. Monta o endereço exato do disparo de texto da Evolution API
        String urlDeDisparo = EVOLUTION_URL + "/message/sendText/" + INSTANCE_NAME;

        // 3. Monta o cabeçalho informando a sua senha (API KEY)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", API_KEY);

        // 4. Monta o corpo da requisição (JSON)
        Map<String, String> corpoRequisicao = new HashMap<>();
        corpoRequisicao.put("number", numeroLimpo);
        corpoRequisicao.put("text", mensagem);

        // 5. Empacota tudo e envia o POST
        HttpEntity<Map<String, String>> pacote = new HttpEntity<>(corpoRequisicao, headers);
        ResponseEntity<String> resposta = restTemplate.postForEntity(urlDeDisparo, pacote, String.class);

        System.out.println("🔔 [SUCESSO] Lembrete disparado para " + numeroLimpo + " | Status Evolution: " + resposta.getStatusCode());
    }
}