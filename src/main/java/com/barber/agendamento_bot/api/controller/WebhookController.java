package com.barber.agendamento_bot.api.controller;

import com.barber.agendamento_bot.api.dto.MensagemWhatsAppDTO;
import com.barber.agendamento_bot.api.service.ChatbotService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook/whatsapp")
public class WebhookController {

    private final ChatbotService chatbotService;

    public WebhookController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping
    public String receberMensagemDoZap(@RequestBody MensagemWhatsAppDTO mensagem) {

        // Passa o telefone e o texto para o Cérebro decidir o que fazer
        String respostaParaEnviar = chatbotService.processarMensagem(
                mensagem.getNumeroCliente(),
                mensagem.getTextoMensagem()
        );

        System.out.println("\n--- SIMULAÇÃO DE ENVIO ---");
        System.out.println("Enviando para o WhatsApp de " + mensagem.getNumeroCliente() + ":");
        System.out.println(respostaParaEnviar);
        System.out.println("--------------------------");

        return "OK"; // Sempre devolvemos OK para o WhatsApp não travar
    }
}