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
    private final ObjectMapper objectMapper = new ObjectMapper(); // Ferramenta para ler JSON com segurança

    // =========================================================================
    // ⚠️ ATENÇÃO: COLOQUE AQUI OS DADOS DA SUA EVOLUTION API
    // =========================================================================
    private final String EVOLUTION_URL = "http://187.77.224.241:47851/message/sendText/barbearia";
    private final String EVOLUTION_API_KEY = "EAlUBkxSKCsYF9mSWGZYxTfTF6qXGD4m";
    // =========================================================================

    public WebhookController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    // Mudamos de JsonNode para String bruta para o Java NUNCA dar erro 500 na entrada
    @PostMapping
    public ResponseEntity<String> receberMensagemDaEvolution(@RequestBody(required = false) String payloadString) {

        try {
            // 1. Escudo anti-vazio
            if (payloadString == null || payloadString.isEmpty()) {
                return ResponseEntity.ok("OK"); // Devolve 200 para acalmar a Evolution
            }

            // Vai imprimir na tela preta do Render DE QUALQUER JEITO!
            System.out.println("🚨 BATEU NO WEBHOOK BRUTO: " + payloadString);

            // 2. Converte o texto em JSON manualmente e com segurança
            JsonNode payload = objectMapper.readTree(payloadString);

            // 3. A Evolution pode mandar o dado dentro de "data" ou direto na raiz. Vamos cobrir os dois!
            JsonNode data = payload;
            if (payload.has("data")) {
                data = payload.get("data");
            }

            // 4. Se não for uma mensagem de texto válida, a gente ignora e devolve 200
            if (!data.has("key") || !data.has("message")) {
                return ResponseEntity.ok("OK");
            }

            // 5. O Escudo Anti-Loop (Ignora mensagens enviadas pelo próprio robô)
            if (data.get("key").has("fromMe") && data.get("key").get("fromMe").asBoolean()) {
                return ResponseEntity.ok("OK");
            }

            // 6. Extrai quem mandou (E bloqueia Grupos e Status)
            String remoteJid = data.get("key").get("remoteJid").asText();
            if (remoteJid.contains("@g.us") || remoteJid.contains("@broadcast")) {
                return ResponseEntity.ok("OK");
            }

            // Limpa o número de telefone
            String telefone = remoteJid.replace("@s.whatsapp.net", "");

            // 7. Garimpa o Texto da mensagem
            String textoMensagem = "";
            JsonNode message = data.get("message");

            if (message.has("conversation")) {
                textoMensagem = message.get("conversation").asText();
            } else if (message.has("extendedTextMessage") && message.get("extendedTextMessage").has("text")) {
                textoMensagem = message.get("extendedTextMessage").get("text").asText();
            }

            // 8. Manda pro cérebro pensar e responde!
            if (!textoMensagem.isEmpty()) {
                System.out.println("📩 Mensagem extraída com sucesso de " + telefone + ": " + textoMensagem);
                String respostaDoRobo = chatbotService.processarMensagem(telefone, textoMensagem);
                enviarMensagemParaEvolution(remoteJid, respostaDoRobo);
            }

        } catch (Exception e) {
            // Se der qualquer erro no código, ele imprime o erro mas DEVOLVE 200 PRA EVOLUTION NÃO TRAVAR!
            System.err.println("❌ ERRO INTERNO AO LER A MENSAGEM: " + e.getMessage());
            e.printStackTrace();
        }

        // Devolve o HTTP 200 (Sucesso)
        return ResponseEntity.ok("OK");
    }

    private void enviarMensagemParaEvolution(String numeroDestino, String textoParaEnviar) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", EVOLUTION_API_KEY);

        Map<String, Object> body = new HashMap<>();
        body.put("number", numeroDestino);

        Map<String, Object> textMessage = new HashMap<>();
        textMessage.put("text", textoParaEnviar);
        body.put("textMessage", textMessage);

        Map<String, Object> options = new HashMap<>();
        options.put("delay", 1200);
        options.put("presence", "composing");
        body.put("options", options);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(EVOLUTION_URL, request, String.class);
            System.out.println("✅ Resposta disparada para a Evolution com sucesso!");
        } catch (Exception e) {
            System.err.println("❌ Falha ao tentar enviar mensagem de volta: " + e.getMessage());
        }
    }
}