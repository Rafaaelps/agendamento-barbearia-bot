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
    // ⚙️ CONFIGURAÇÕES DA SUA EVOLUTION API MANTIDAS
    // =========================================================================
    private final String EVOLUTION_URL = "http://187.77.224.241:47851/message/sendText/barbearia";
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
            boolean isMidia = false; // ✨ A CHAVE DA MÁGICA AQUI

            JsonNode message = data.get("message");

            if (message.has("conversation")) {
                textoMensagem = message.get("conversation").asText();
            } else if (message.has("extendedTextMessage") && message.get("extendedTextMessage").has("text")) {
                textoMensagem = message.get("extendedTextMessage").get("text").asText();
            }
            // ✨ SE NÃO TEM TEXTO, VERIFICA SE É ÁUDIO, FOTO OU FIGURINHA!
            else if (message.has("audioMessage") || message.has("imageMessage") ||
                    message.has("videoMessage") || message.has("stickerMessage") ||
                    message.has("documentMessage")) {
                isMidia = true;
            }

            // ✨ AGORA ELE DEIXA PASSAR SE TIVER TEXTO OU SE FOR MÍDIA
            if (!textoMensagem.isEmpty() || isMidia) {

                if (isMidia) {
                    System.out.println("🎤 Áudio/Mídia recebida de " + telefone + " -> Acionando interceptador!");
                } else {
                    System.out.println("📩 Texto de " + telefone + ": " + textoMensagem);
                }

                // O textoMensagem vai entrar vazio se for mídia, o que dispara o aviso "Opa, não escuto áudios" lá no ChatbotService
                String respostaDoRobo = chatbotService.processarMensagem(telefone, textoMensagem);

                // Só tenta responder se o robô tiver algo a dizer
                if (respostaDoRobo != null && !respostaDoRobo.isEmpty()) {
                    enviarMensagemParaEvolution(remoteJid, respostaDoRobo);
                }
            }

        } catch (Exception e) {
            System.err.println("❌ ERRO INTERNO AO LER A MENSAGEM: " + e.getMessage());
            e.printStackTrace();
        }

        return ResponseEntity.ok("OK");
    }

    private void enviarMensagemParaEvolution(String numeroDestino, String textoParaEnviar) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", EVOLUTION_API_KEY);

        Map<String, Object> body = new HashMap<>();
        body.put("number", numeroDestino);
        body.put("text", textoParaEnviar);
        body.put("delay", 1200);
        body.put("presence", "composing"); // Faz aparecer "Digitando..." no WhatsApp

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(EVOLUTION_URL, request, String.class);
        } catch (Exception e) {
            System.err.println("❌ Falha ao tentar enviar mensagem de volta: " + e.getMessage());
        }
    }
}