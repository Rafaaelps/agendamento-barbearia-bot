package com.barber.agendamento_bot.api.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/crm")
public class CrmController {

    // ⚠️ ATENÇÃO: PREENCHA COM OS DADOS DA SUA EVOLUTION API
    private final String EVOLUTION_API_URL = "http://187.77.224.241:47851";
    private final String INSTANCIA = "barbearia";
    private final String API_KEY = "EAlUBkxSKCsYF9mSWGZYxTfTF6qXGD4m";

    @PostMapping("/lembrete")
    public ResponseEntity<?> enviarLembrete(@RequestBody Map<String, String> dados) {
        String telefone = dados.get("telefone");
        String mensagem = dados.get("mensagem");

        RestTemplate restTemplate = new RestTemplate();
        // Rota oficial da Evolution API para envio de texto
        String url = EVOLUTION_API_URL + "/message/sendText/" + INSTANCIA;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", API_KEY); // Chave de segurança

        // Monta o corpo da requisição no formato que a Evolution API exige
        Map<String, Object> body = new HashMap<>();
        body.put("number", telefone);
        body.put("text", mensagem);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            // Dispara a mensagem via HTTP POST
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao enviar mensagem para Evolution API: " + e.getMessage());
        }
    }
}
