package com.barber.agendamento_bot.api.service;

import com.barber.agendamento_bot.api.entity.Agendamento;
import com.barber.agendamento_bot.api.entity.Servico;
import com.barber.agendamento_bot.api.entity.SessaoBot;
import com.barber.agendamento_bot.api.entity.Usuario;
import com.barber.agendamento_bot.api.repository.ServicoRepository;
import com.barber.agendamento_bot.api.repository.SessaoBotRepository;
import com.barber.agendamento_bot.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private final SessaoBotRepository sessaoRepository;
    private final AgendaService agendaService;
    private final ServicoRepository servicoRepository;
    private final UsuarioRepository usuarioRepository;

    public ChatbotService(SessaoBotRepository sessaoRepository, AgendaService agendaService, ServicoRepository servicoRepository, UsuarioRepository usuarioRepository) {
        this.sessaoRepository = sessaoRepository;
        this.agendaService = agendaService;
        this.servicoRepository = servicoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    private void limparDadosTemporariosDaSessao(SessaoBot sessao) {
        sessao.setNomeClienteTemporario(null);
        sessao.setIdServicoTemporario(null);
        sessao.setDataTemporaria(null);
        sessao.setIdAgendamentoTemporario(null);
    }

    public String processarMensagem(String telefone, String textoRecebido, String instanciaWhatsapp) {

        if (textoRecebido == null || textoRecebido.trim().isEmpty()) {
            return "🎧 Opa! Eu sou um assistente virtual em treinamento e ainda não consigo ouvir áudios ou ver imagens.\n\nPor favor, digite a sua mensagem em texto para eu poder te ajudar!";
        }

        Usuario barbeiroResponsavel = null;
        if (instanciaWhatsapp != null && !instanciaWhatsapp.isEmpty()) {
            String instanciaLimpa = instanciaWhatsapp.trim().toLowerCase();
            for (Usuario u : usuarioRepository.findAll()) {
                if (u.getInstanciaWhatsapp() != null && u.getInstanciaWhatsapp().trim().toLowerCase().equals(instanciaLimpa)) {
                    barbeiroResponsavel = u;
                    break;
                }
            }
        }

        if (barbeiroResponsavel == null) {
            return "⚠️ *Aviso do Sistema:*\nEste número de WhatsApp ainda não foi vinculado corretamente a um barbeiro no painel administrativo.";
        }

        SessaoBot sessao = sessaoRepository.findById(telefone).orElse(new SessaoBot(telefone, "MENU_INICIAL"));
        String respostaDoRobo = "";

        String textoLimpo = textoRecebido.toLowerCase().trim();
        String textoSemAcento = Normalizer.normalize(textoLimpo, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");

        ZoneId fusoBR = ZoneId.of("America/Sao_Paulo");
        LocalDateTime agora = LocalDateTime.now(fusoBR);

        // Reset por inatividade (10 min)
        if (sessao.getUltimaInteracao() != null) {
            long minutosInativos = ChronoUnit.MINUTES.between(sessao.getUltimaInteracao(), agora);
            if (minutosInativos >= 10 && !sessao.getPassoAtual().equals("MENU_INICIAL")) {
                sessao.setPassoAtual("MENU_INICIAL");
                limparDadosTemporariosDaSessao(sessao);
                respostaDoRobo = "⏳ Vi que você demorou um pouquinho, então eu reiniciei nosso atendimento.\n\n";
            }
        }
        sessao.setUltimaInteracao(agora);

        // Comandos de interrupção
        if (textoSemAcento.matches("^(oi|ola|bom dia|boa tarde|boa noite|menu|recomecar|cancelar|sair).*")) {
            sessao.setPassoAtual("MENU_INICIAL");
            limparDadosTemporariosDaSessao(sessao);

            if (textoSemAcento.equals("cancelar") || textoSemAcento.equals("sair")) {
                sessaoRepository.save(sessao);
                return "🛑 Operação cancelada. Quando quiser recomeçar, é só mandar um 'Oi'!";
            }
        }

        // Lógica de Agradecimentos
        if (sessao.getPassoAtual().equals("MENU_INICIAL")) {
            String regrasDeObrigado = "^(obrigad[oa]s?|obrigadao|muito obrigad[oa]s?|muitissimo obrigad[oa]s?|obrigad[oa] por tudo|obrigad[oa] de montao|obrigad[oa] de coracao|brigad[oa]s?|brigadao|valeu|beleza|joia|falou|show|firmeza|tmj|tamo junto|gratidao).*";
            if (textoSemAcento.matches(regrasDeObrigado)) {
                sessaoRepository.save(sessao);
                return "Fico feliz em ajudar, nós agradecemos o contato! Se precisar de mais alguma coisa, é só chamar. 💈💙";
            }
        }

        // Voltar etapa
        if (textoLimpo.equals("voltar")) {
            switch (sessao.getPassoAtual()) {
                case "ESPERANDO_HORARIO":
                    sessao.setPassoAtual("ESPERANDO_DATA");
                    respostaDoRobo = "🗓️ Vamos escolher outra data! Para qual dia você deseja agendar? (DD/MM):";
                    break;
                case "ESPERANDO_DATA":
                    sessao.setPassoAtual("ESPERANDO_SERVICO");
                    List<Servico> servs = servicoRepository.findAllByDonoDoRegistro(barbeiroResponsavel)
                            .stream().filter(s -> s.getAtivo() == null || s.getAtivo())
                            .sorted(Comparator.comparing(Servico::getId)).toList();
                    StringBuilder menu = new StringBuilder("✂️ Escolha o serviço:\n\n");
                    for (int i = 0; i < servs.size(); i++) {
                        menu.append(i + 1).append(" - ").append(servs.get(i).getNome()).append("\n");
                    }
                    respostaDoRobo = menu.toString();
                    break;
                default:
                    sessao.setPassoAtual("MENU_INICIAL");
                    limparDadosTemporariosDaSessao(sessao);
                    break;
            }
            sessaoRepository.save(sessao);
            return respostaDoRobo + "\n\n*Digite 'voltar' ou 'cancelar'.*";
        }

        DateTimeFormatter formatadorData = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter formatadorHora = DateTimeFormatter.ofPattern("HH:mm");

        switch (sessao.getPassoAtual()) {

            case "MENU_INICIAL":
                respostaDoRobo += "Olá! Sou o assistente virtual. O que deseja?\n*1* - Novo Agendamento\n*2* - Cancelar Agendamento";
                sessao.setPassoAtual("ESPERANDO_OPCAO_INICIAL");
                break;

            case "ESPERANDO_OPCAO_INICIAL":
                if (textoLimpo.equals("1")) {
                    respostaDoRobo = "Qual é o seu nome?";
                    sessao.setPassoAtual("ESPERANDO_NOME");
                } else if (textoLimpo.equals("2")) {
                    Agendamento ag = agendaService.buscarAgendamentoAtivoPorTelefone(telefone);
                    if (ag != null) {
                        sessao.setIdAgendamentoTemporario(ag.getId());
                        respostaDoRobo = "Confirma o cancelamento? Digite *SIM* ou *NAO*.";
                        sessao.setPassoAtual("CONFIRMANDO_CANCELAMENTO");
                    } else {
                        respostaDoRobo = "Sem agendamentos ativos. Digite *Oi*.";
                        sessao.setPassoAtual("MENU_INICIAL");
                    }
                }
                break;

            case "ESPERANDO_NOME":
                sessao.setNomeClienteTemporario(textoRecebido);
                List<Servico> listaServicos = servicoRepository.findAllByDonoDoRegistro(barbeiroResponsavel)
                        .stream().filter(s -> s.getAtivo() == null || s.getAtivo())
                        .sorted(Comparator.comparing(Servico::getId)).toList();
                StringBuilder sb = new StringBuilder("Prazer, " + textoRecebido + "! O que deseja agendar?\n\n");
                for (int i = 0; i < listaServicos.size(); i++) {
                    sb.append(i + 1).append(" - ").append(listaServicos.get(i).getNome()).append(" (R$ ").append(listaServicos.get(i).getPreco()).append(")\n");
                }
                respostaDoRobo = sb.toString();
                sessao.setPassoAtual("ESPERANDO_SERVICO");
                break;

            case "ESPERANDO_SERVICO":
                try {
                    int indice = Integer.parseInt(textoLimpo);
                    List<Servico> servs = servicoRepository.findAllByDonoDoRegistro(barbeiroResponsavel)
                            .stream().filter(s -> s.getAtivo() == null || s.getAtivo())
                            .sorted(Comparator.comparing(Servico::getId)).toList();
                    if (indice >= 1 && indice <= servs.size()) {
                        Servico s = servs.get(indice - 1);
                        sessao.setIdServicoTemporario(s.getId());
                        respostaDoRobo = "Escolheu " + s.getNome() + ". Para qual dia? (DD/MM):";
                        sessao.setPassoAtual("ESPERANDO_DATA");
                    }
                } catch (Exception e) { respostaDoRobo = "Digite apenas o número."; }
                break;

            case "ESPERANDO_DATA":
                try {
                    LocalDate data = LocalDate.parse(textoLimpo + "/" + agora.getYear(), formatadorData);
                    if (data.isBefore(agora.toLocalDate())) {
                        respostaDoRobo = "Data inválida. Digite hoje ou depois:";
                    } else {
                        List<LocalTime> livres = agendaService.buscarHorariosLivres(data, sessao.getIdServicoTemporario(), barbeiroResponsavel);
                        if (livres.isEmpty()) {
                            respostaDoRobo = "Sem horários para este dia. Tente outro:";
                        } else {
                            sessao.setDataTemporaria(textoLimpo);
                            StringBuilder hl = new StringBuilder("Horários livres para " + textoLimpo + ":\n\n");
                            livres.forEach(h -> hl.append("⏰ *").append(h.format(formatadorHora)).append("*\n"));
                            respostaDoRobo = hl.append("\nQual prefere? (Ex: 14:30):").toString();
                            sessao.setPassoAtual("ESPERANDO_HORARIO");
                        }
                    }
                } catch (Exception e) { respostaDoRobo = "Use o formato DD/MM."; }
                break;

            case "ESPERANDO_HORARIO":
                try {
                    LocalTime hora = LocalTime.parse(textoLimpo);
                    LocalDate data = LocalDate.parse(sessao.getDataTemporaria() + "/" + agora.getYear(), formatadorData);

                    // ✨ TRAVA DE LIMITE: 2 agendamentos por dia via bot
                    if (agendaService.atingiuLimiteDiario(sessao.getTelefone(), data)) {
                        respostaDoRobo = "⚠️ *Limite atingido!*\n\nPermitimos apenas *2 agendamentos ativos* por dia via WhatsApp. Ligue para a barbearia para mais horários, ou cancele um horário agendado para reagendar.";
                        sessao.setPassoAtual("MENU_INICIAL");
                        limparDadosTemporariosDaSessao(sessao);
                        break;
                    }

                    Agendamento novo = new Agendamento();
                    novo.setTelefoneCliente(sessao.getTelefone());
                    novo.setNomeCliente(sessao.getNomeClienteTemporario());
                    novo.setDataHoraInicio(LocalDateTime.of(data, hora));
                    novo.setDonoDoRegistro(barbeiroResponsavel);
                    novo.setServicoEscolhido(servicoRepository.findById(sessao.getIdServicoTemporario()).orElse(null));
                    novo.setFormaPagamento("PENDENTE");
                    novo.setFaturamentoBarbeiro(BigDecimal.ZERO);

                    if (agendaService.tentarAgendar(novo)) {
                        respostaDoRobo = "✅ Confirmado! Te esperamos dia " + sessao.getDataTemporaria() + " às " + textoLimpo + ".";
                        sessao.setPassoAtual("MENU_INICIAL");
                        limparDadosTemporariosDaSessao(sessao);
                    } else {
                        respostaDoRobo = "Horário ocupado. Escolha outro:";
                    }
                } catch (Exception e) { respostaDoRobo = "Horário inválido. Use HH:mm."; }
                break;

            case "CONFIRMANDO_CANCELAMENTO":
                if (textoSemAcento.contains("sim")) {
                    agendaService.cancelarAgendamento(sessao.getIdAgendamentoTemporario());
                    respostaDoRobo = "✅ Cancelado com sucesso.";
                } else {
                    respostaDoRobo = "Ok, mantido.";
                }
                sessao.setPassoAtual("MENU_INICIAL");
                break;
        }

        sessaoRepository.save(sessao);
        if (List.of("ESPERANDO_NOME", "ESPERANDO_SERVICO", "ESPERANDO_DATA", "ESPERANDO_HORARIO").contains(sessao.getPassoAtual())) {
            respostaDoRobo += "\n\n*Digite 'voltar' ou 'cancelar'.*";
        }
        return respostaDoRobo;
    }
}