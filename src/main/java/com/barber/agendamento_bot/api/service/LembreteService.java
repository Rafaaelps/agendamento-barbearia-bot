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
        // ✨ TRAVA DE FUSO HORÁRIO INFALÍVEL
        ZoneId fusoBR = ZoneId.of("America/Sao_Paulo");
        LocalDateTime agora = LocalDateTime.now(fusoBR);
        LocalDateTime daquiA35Minutos = agora.plusMinutes(35);

        // LOG 1: Batimento cardíaco do robô (Para sabermos que ele acordou)
        System.out.println("⏱️ [CRON] Buscando cortes entre " + agora.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " e " + daquiA35Minutos.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));

        List<Agendamento> agendamentos = agendamentoRepository
                .buscarAgendamentosParaLembrar("CONFIRMADO", agora, daquiA35Minutos);

        // LOG 2: Quantos ele achou?
        if (agendamentos.isEmpty()) {
            System.out.println("🤷 Nenhum cliente encontrado para lembrar agora.");
            return;
        }

        System.out.println("🎯 Encontrados " + agendamentos.size() + " clientes para enviar lembrete!");

        for (Agendamento agendamento : agendamentos) {
            boolean sucesso = enviarMensagemEvolution(agendamento);

            // Só carimba que enviou se a Evolution API realmente aceitar a mensagem!
            if (sucesso) {
                agendamento.setLembreteEnviado(true);
                agendamentoRepository.save(agendamento);
            }
        }
    }

    private boolean enviarMensagemEvolution(Agendamento agendamento) {
        String horaFormatada = agendamento.getDataHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm"));
        String mensagem = "⏳ Olá, *" + agendamento.getNomeCliente() + "*! Passando para lembrar que o seu horário conosco é daqui a pouco, às *" + horaFormatada + "*. Já estamos te esperando! 💈";

        String numeroLimpo = agendamento.getTelefoneCliente().replaceAll("[^0-9]", "");
        if (!numeroLimpo.startsWith("55")) {
            numeroLimpo = "55" + numeroLimpo;
        }

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
            System.out.println("✅ [ENVIADO] Lembrete para " + numeroLimpo + " | Evolution respondeu: 200 OK");
            return true;

        } catch (HttpClientErrorException e) {
            // LOG 3: O DEDO DURO DA EVOLUTION API
            System.err.println("❌ [ERRO EVOLUTION] A API recusou o envio para " + numeroLimpo + "!");
            System.err.println("🚨 Código HTTP: " + e.getStatusCode());
            System.err.println("🚨 Motivo: " + e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            System.err.println("❌ [ERRO SISTEMA] Falha ao tentar conectar na Evolution: " + e.getMessage());
            return false;
        }
    }
}