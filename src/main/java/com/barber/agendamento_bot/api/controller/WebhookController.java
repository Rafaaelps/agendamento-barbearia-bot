package com.barber.agendamento_bot.api.controller;

import com.barber.agendamento_bot.api.service.ChatbotService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/webhook/whatsapp")
public class WebhookController {

    private final ChatbotService chatbotService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // =========================================================================
    // ⚙️ BASE URL DA SUA EVOLUTION API (SEM O NOME DA INSTÂNCIA NO FINAL)
    // =========================================================================
    private final String EVOLUTION_BASE_URL = "http://187.77.224.241:47851/message/sendText/";
    private final String EVOLUTION_API_KEY = "EAlUBkxSKCsYF9mSWGZYxTfTF6qXGD4m";
    // =========================================================================

    public WebhookController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping
    public ResponseEntity<String> receberMensagemDaEvolution(@RequestBody(required = false) String payloadString) {

        try {
            if (payloadString == null || payloadString.isEmpty()) {
                return ResponseEntity.ok("OK");
            }

            JsonNode payload = objectMapper.readTree(payloadString);

            // ✨ Pega o nome da instância diretamente do payload da Evolution
            String instanciaName = "";
            if (payload.has("instance")) {
                instanciaName = payload.get("instance").asText();
            }

            JsonNode data = payload;
            if (payload.has("data")) {
                data = payload.get("data");
            }

            if (!data.has("key") || !data.has("message")) {
                return ResponseEntity.ok("OK");
            }

            if (data.get("key").has("fromMe") && data.get("key").get("fromMe").asBoolean()) {
                return ResponseEntity.ok("OK");
            }

            String remoteJid = data.get("key").get("remoteJid").asText();
            if (remoteJid.contains("@g.us") || remoteJid.contains("@broadcast")) {
                return ResponseEntity.ok("OK");
            }

            String telefone = remoteJid.replace("@s.whatsapp.net", "");

            String textoMensagem = "";
            boolean isMidia = false;

            JsonNode message = data.get("message");

            if (message.has("conversation")) {
                textoMensagem = message.get("conversation").asText();
            } else if (message.has("extendedTextMessage") && message.get("extendedTextMessage").has("text")) {
                textoMensagem = message.get("extendedTextMessage").get("text").asText();
            }
            else if (message.has("audioMessage") || message.has("imageMessage") ||
                    message.has("videoMessage") || message.has("stickerMessage") ||
                    message.has("documentMessage")) {
                isMidia = true;
            }

            if (!textoMensagem.isEmpty() || isMidia) {

                if (isMidia) {
                    System.out.println("🎤 Áudio/Mídia recebida de " + telefone + " na instância [" + instanciaName + "] -> Acionando interceptador!");
                } else {
                    System.out.println("📩 Texto de " + telefone + " na instância [" + instanciaName + "]: " + textoMensagem);
                }

                // ✨ Passa a instância para o ChatbotService saber quem está atendendo
                String respostaDoRobo = chatbotService.processarMensagem(telefone, textoMensagem, instanciaName);

                if (respostaDoRobo != null && !respostaDoRobo.isEmpty()) {
                    // Responde pela mesma instância
                    enviarMensagemParaEvolution(remoteJid, respostaDoRobo, instanciaName);
                }
            }

        } catch (Exception e) {
            System.err.println("❌ ERRO INTERNO AO LER A MENSAGEM: " + e.getMessage());
            e.printStackTrace();
        }

        return ResponseEntity.ok("OK");
    }

    private void enviarMensagemParaEvolution(String numeroDestino, String textoParaEnviar, String instancia) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", EVOLUTION_API_KEY);

        Map<String, Object> body = new HashMap<>();
        body.put("number", numeroDestino);
        body.put("text", textoParaEnviar);
        body.put("delay", 1200);
        body.put("presence", "composing");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        // ✨ Constrói a URL final dinamicamente com base em quem atendeu
        String urlFinal = EVOLUTION_BASE_URL + instancia;

        try {
            restTemplate.postForEntity(urlFinal, request, String.class);
        } catch (Exception e) {
            System.err.println("❌ Falha ao tentar enviar mensagem de volta pela instância [" + instancia + "]: " + e.getMessage());
        }
    }
}