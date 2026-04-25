package com.barber.agendamento_bot.api.service;

import com.barber.agendamento_bot.api.entity.Agendamento;
import com.barber.agendamento_bot.api.entity.Usuario;
import com.barber.agendamento_bot.api.repository.AgendamentoRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
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

    private final String EVOLUTION_URL = "http://187.77.224.241:47851";
    private final String API_KEY = "EAlUBkxSKCsYF9mSWGZYxTfTF6qXGD4m";

    public LembreteService(AgendamentoRepository agendamentoRepository) {
        this.agendamentoRepository = agendamentoRepository;
    }

    // ⏰ CRON 1: ENVIA O LEMBRETE (Roda a cada 1 min)
    @Scheduled(cron = "0 * * * * *")
    public void verificarEEnviarLembretes() {
        ZoneId fusoBR = ZoneId.of("America/Sao_Paulo");
        LocalDateTime agora = LocalDateTime.now(fusoBR);

        List<Agendamento> agendamentos = agendamentoRepository.findByStatusNot("CANCELADO");

        for (Agendamento ag : agendamentos) {
            if (Boolean.TRUE.equals(ag.getLembreteEnviado()) || ag.getDataHoraInicio().isBefore(agora) || "CONCLUIDO".equals(ag.getStatus())) {
                continue;
            }

            Usuario barbeiro = ag.getDonoDoRegistro();
            if (barbeiro == null || Boolean.FALSE.equals(barbeiro.getBotAtivo()) || barbeiro.getInstanciaWhatsapp() == null || barbeiro.getInstanciaWhatsapp().isEmpty()) {
                continue;
            }

            int minutosAviso = barbeiro.getMinutosConfirmacao() != null ? barbeiro.getMinutosConfirmacao() : 30;
            LocalDateTime horarioParaAvisar = ag.getDataHoraInicio().minusMinutes(minutosAviso);

            if (agora.isAfter(horarioParaAvisar) || agora.isEqual(horarioParaAvisar)) {
                boolean sucesso = enviarMensagemEvolution(ag, false, barbeiro.getInstanciaWhatsapp());
                if (sucesso) {
                    ag.setLembreteEnviado(true);
                    agendamentoRepository.save(ag);
                }
            }
        }
    }

    // 🪓 CRON 2: O CARRASCO (Cancela após 20 minutos de espera)
    @Scheduled(cron = "0 * * * * *")
    public void verificarECancelarNaoConfirmados() {
        ZoneId fusoBR = ZoneId.of("America/Sao_Paulo");
        LocalDateTime agora = LocalDateTime.now(fusoBR);

        List<Agendamento> agendamentos = agendamentoRepository.findByStatusNot("CANCELADO");

        for (Agendamento ag : agendamentos) {
            if (!Boolean.TRUE.equals(ag.getLembreteEnviado()) || ag.getDataHoraInicio().isBefore(agora) || "CONCLUIDO".equals(ag.getStatus()) || "CONFIRMADO".equals(ag.getStatus())) {
                continue;
            }

            Usuario barbeiro = ag.getDonoDoRegistro();
            if (barbeiro == null || Boolean.FALSE.equals(barbeiro.getBotAtivo()) || barbeiro.getInstanciaWhatsapp() == null || barbeiro.getInstanciaWhatsapp().isEmpty()) {
                continue;
            }

            int minutosAviso = barbeiro.getMinutosConfirmacao() != null ? barbeiro.getMinutosConfirmacao() : 30;
            int minutosParaCancelar = Math.max(5, minutosAviso - 20);
            LocalDateTime limiteCancelamento = ag.getDataHoraInicio().minusMinutes(minutosParaCancelar);

            if (agora.isAfter(limiteCancelamento)) {
                try {
                    ag.setStatus("CANCELADO");
                    agendamentoRepository.save(ag);
                    enviarMensagemEvolution(ag, true, barbeiro.getInstanciaWhatsapp());
                    System.out.println("🪓 [CANCELADO] Cliente " + ag.getNomeCliente() + " não confirmou a tempo (Instância: " + barbeiro.getInstanciaWhatsapp() + ").");
                } catch (Exception e) {
                    System.err.println("❌ Erro ao cancelar falta de confirmação: " + e.getMessage());
                }
            }
        }
    }

    private boolean enviarMensagemEvolution(Agendamento agendamento, boolean isCancelamento, String instancia) {
        String horaFormatada = agendamento.getDataHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm"));
        String mensagem = "";

        if (isCancelamento) {
            mensagem = "❌ *AGENDAMENTO CANCELADO*\n\nOlá, *" + agendamento.getNomeCliente() + "*. Como não recebemos a sua confirmação, o seu horário às *" + horaFormatada + "* foi liberado automaticamente na agenda para outro cliente.\n\nSe quiser remarcar um novo horário, basta mandar um *Oi*!";
        } else {
            mensagem = "⏳ Olá, *" + agendamento.getNomeCliente() + "*! O seu horário às *" + horaFormatada + "* está se aproximando.\n\n⚠️ *ATENÇÃO:* Responda *SIM* nos próximos 20 minutos para confirmar sua presença, caso contrário o sistema cancelará o agendamento automaticamente para liberar a vaga!";
        }

        String numeroLimpo = agendamento.getTelefoneCliente().replaceAll("[^0-9]", "");
        if (!numeroLimpo.startsWith("55")) numeroLimpo = "55" + numeroLimpo;

        // ✨ URL dinâmica baseada no barbeiro dono do agendamento
        String urlDeDisparo = EVOLUTION_URL + "/message/sendText/" + instancia;

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
            restTemplate.postForEntity(urlDeDisparo, pacote, String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}