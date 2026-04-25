package com.barber.agendamento_bot.api.controller;

import com.barber.agendamento_bot.api.entity.Usuario;
import com.barber.agendamento_bot.api.repository.UsuarioRepository;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/crm")
public class CrmController {

    private final String EVOLUTION_API_URL = "http://187.77.224.241:47851";
    private final String API_KEY = "EAlUBkxSKCsYF9mSWGZYxTfTF6qXGD4m";

    private final UsuarioRepository usuarioRepository;

    public CrmController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    private Usuario getLogado() {
        String login = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByLogin(login).orElse(null);
    }

    @PostMapping("/lembrete")
    public ResponseEntity<?> enviarLembrete(@RequestBody Map<String, String> dados) {
        String telefone = dados.get("telefone");
        String mensagem = dados.get("mensagem");

        // ✨ Busca a instância do usuário logado
        Usuario logado = getLogado();
        if (logado == null || logado.getInstanciaWhatsapp() == null || logado.getInstanciaWhatsapp().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erro: Seu usuário não possui uma instância do WhatsApp configurada.");
        }

        String instancia = logado.getInstanciaWhatsapp();

        RestTemplate restTemplate = new RestTemplate();
        String url = EVOLUTION_API_URL + "/message/sendText/" + instancia;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", API_KEY);

        Map<String, Object> body = new HashMap<>();
        body.put("number", telefone);
        body.put("text", mensagem);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao enviar mensagem para Evolution API na instância " + instancia + ": " + e.getMessage());
        }
    }
}