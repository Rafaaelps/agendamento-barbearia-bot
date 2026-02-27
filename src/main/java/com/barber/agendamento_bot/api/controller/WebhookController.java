package com.barber.agendamento_bot.api.controller;

import com.barber.agendamento_bot.api.service.ChatbotService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/webhook/whatsapp")
public class WebhookController {

    private final ChatbotService chatbotService;

    // =========================================================================
    // ⚠️ ATENÇÃO: COLOQUE AQUI OS DADOS DA SUA EVOLUTION API
    // =========================================================================
    private final String EVOLUTION_URL = "http://187.77.224.241:47851/message/sendText/barbearia";
    private final String EVOLUTION_API_KEY = "EAlUBkxSKCsYF9mSWGZYxTfTF6qXGD4m";
    // =========================================================================

    public WebhookController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping
    public String receberMensagemDaEvolution(@RequestBody JsonNode payload) {

        System.out.println("🚨 BATEU NO WEBHOOK! O que chegou: " + payload.toString());

        try {
            // 1. Verifica se é um evento de mensagem recebida (ignora status de leitura, etc)
            if (payload.has("event") && payload.get("event").asText().equals("messages.upsert")) {

                JsonNode data = payload.get("data");

                // 2. Se a mensagem foi enviada pelo próprio bot (fromMe = true), ignoramos para não dar loop!
                if (data.get("key").get("fromMe").asBoolean()) {
                    return "OK";
                }

                // 3. Garimpa o ID de quem enviou
                String remoteJid = data.get("key").get("remoteJid").asText();

                // =======================================================
                // ✨ PASSO 2.5: O ESCUDO ANTI-GRUPOS E STATUS
                // Se a mensagem vier de um grupo (@g.us) ou status (@broadcast), o Java ignora na hora!
                // =======================================================
                if (remoteJid.contains("@g.us") || remoteJid.contains("@broadcast")) {
                    return "OK";
                }

                // Extrai apenas o número do telefone
                String telefone = remoteJid.replace("@s.whatsapp.net", "");

                // 4. Garimpa o Texto da mensagem (pode vir em 'conversation' ou 'extendedTextMessage')
                String textoMensagem = "";
                JsonNode message = data.get("message");

                if (message != null) {
                    if (message.has("conversation")) {
                        textoMensagem = message.get("conversation").asText();
                    } else if (message.has("extendedTextMessage")) {
                        textoMensagem = message.get("extendedTextMessage").get("text").asText();
                    }
                }

                // Se tiver texto, manda pro cérebro processar
                if (!textoMensagem.isEmpty()) {
                    System.out.println("📩 Mensagem recebida de " + telefone + ": " + textoMensagem);

                    String respostaDoRobo = chatbotService.processarMensagem(telefone, textoMensagem);

                    // 5. DISPARA A RESPOSTA DE VOLTA PARA O WHATSAPP DO CLIENTE!
                    enviarMensagemParaEvolution(remoteJid, respostaDoRobo);
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar webhook da Evolution: " + e.getMessage());
        }

        return "OK"; // Sempre devolvemos OK rápido para a Evolution não travar
    }

    // Método que faz a requisição HTTP (POST) para a Evolution enviar a mensagem
    private void enviarMensagemParaEvolution(String numeroDestino, String textoParaEnviar) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", EVOLUTION_API_KEY);

        // Monta o corpinho que a Evolution exige para enviar texto
        Map<String, Object> body = new HashMap<>();
        body.put("number", numeroDestino);

        Map<String, Object> textMessage = new HashMap<>();
        textMessage.put("text", textoParaEnviar);
        body.put("textMessage", textMessage);

        // Simula que o robô está "digitando..." por 1 segundo para ficar mais natural!
        Map<String, Object> options = new HashMap<>();
        options.put("delay", 1200);
        options.put("presence", "composing");
        body.put("options", options);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(EVOLUTION_URL, request, String.class);
            System.out.println("✅ Resposta enviada com sucesso!");
        } catch (Exception e) {
            System.err.println("❌ Falha ao enviar resposta via Evolution: " + e.getMessage());
        }
    }
}