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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
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

        // ✨ CORREÇÃO 1: Busca o dono da instância ignorando espaços e letras maiúsculas/minúsculas!
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

        // ✨ TRAVA DE SEGURANÇA: Se não achar o barbeiro, avisa o dono que algo está errado no cadastro.
        if (barbeiroResponsavel == null) {
            return "⚠️ *Aviso do Sistema:*\nEste número de WhatsApp ainda não foi vinculado corretamente a um barbeiro no painel administrativo.\n\nPor favor, peça ao gerente para verificar se o nome da instância *[" + instanciaWhatsapp + "]* está digitado corretamente na aba 'Equipe'.";
        }

        SessaoBot sessao = sessaoRepository.findById(telefone).orElse(new SessaoBot(telefone, "MENU_INICIAL"));
        String respostaDoRobo = "";
        String textoLimpo = textoRecebido.toLowerCase().trim();

        ZoneId fusoBR = ZoneId.of("America/Sao_Paulo");
        LocalDateTime agora = LocalDateTime.now(fusoBR);

        if (sessao.getUltimaInteracao() != null) {
            long minutosInativos = ChronoUnit.MINUTES.between(sessao.getUltimaInteracao(), agora);
            if (minutosInativos >= 10 && !sessao.getPassoAtual().equals("MENU_INICIAL")) {
                sessao.setPassoAtual("MENU_INICIAL");
                limparDadosTemporariosDaSessao(sessao);
                respostaDoRobo = "⏳ Vi que você demorou um pouquinho, então eu reiniciei nosso atendimento para organizar a agenda, tá bom?\n\n";
            }
        }
        sessao.setUltimaInteracao(agora);

        if (textoLimpo.matches("^(oi|olá|ola|bom dia|boa tarde|boa noite|menu|recomeçar|cancelar|sair).*")) {
            sessao.setPassoAtual("MENU_INICIAL");
            limparDadosTemporariosDaSessao(sessao);

            if (textoLimpo.equals("cancelar") || textoLimpo.equals("sair")) {
                sessaoRepository.save(sessao);
                return "🛑 Operação cancelada. Quando quiser recomeçar, é só mandar um 'Oi'!";
            }
        }

        if (textoLimpo.matches("^(sim|confirmar|confirmo|com certeza).*") && !sessao.getPassoAtual().equals("CONFIRMANDO_CANCELAMENTO")) {
            Agendamento ag = agendaService.buscarAgendamentoAtivoPorTelefone(telefone);

            if (ag != null && Boolean.TRUE.equals(ag.getLembreteEnviado()) && "AGENDADO".equals(ag.getStatus())) {
                agendaService.confirmarPresenca(ag.getId());
                sessao.setPassoAtual("MENU_INICIAL");
                limparDadosTemporariosDaSessao(sessao);
                sessaoRepository.save(sessao);
                return "✅ *Presença confirmada!* Muito obrigado, " + ag.getNomeCliente() + ". Estamos te esperando no horário marcado! 💈";
            }
        }

        if (textoLimpo.equals("voltar")) {
            switch (sessao.getPassoAtual()) {
                case "ESPERANDO_HORARIO":
                    sessao.setPassoAtual("ESPERANDO_DATA");
                    respostaDoRobo = "🗓️ Vamos escolher outra data! Para qual dia você deseja agendar? (Digite no formato DD/MM, ex: " + agora.format(DateTimeFormatter.ofPattern("dd/MM")) + "):";
                    break;
                case "ESPERANDO_DATA":
                    sessao.setPassoAtual("ESPERANDO_SERVICO");
                    List<Servico> listaServicosVoltar = servicoRepository.findAllByDonoDoRegistro(barbeiroResponsavel)
                            .stream().filter(s -> s.getAtivo() == null || s.getAtivo()).collect(Collectors.toList());

                    StringBuilder menuServicos = new StringBuilder("✂️ Vamos escolher outro serviço! O que deseja agendar?\n\n");
                    for (Servico s : listaServicosVoltar) {
                        menuServicos.append(s.getId()).append(" - ").append(s.getNome()).append(" (R$ ").append(s.getPreco()).append(")\n");
                    }
                    respostaDoRobo = menuServicos.toString();
                    break;
                default:
                    sessao.setPassoAtual("MENU_INICIAL");
                    limparDadosTemporariosDaSessao(sessao);
                    break;
            }

            if (!sessao.getPassoAtual().equals("MENU_INICIAL")) {
                sessaoRepository.save(sessao);
                return respostaDoRobo + "\n\n*Digite 'voltar' para a etapa anterior ou 'cancelar' para sair.*";
            }
        }

        DateTimeFormatter formatadorData = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter formatadorHora = DateTimeFormatter.ofPattern("HH:mm");

        switch (sessao.getPassoAtual()) {

            case "MENU_INICIAL":
                respostaDoRobo += "Olá! Sou o assistente virtual. O que você deseja fazer?\n*1* - Novo Agendamento\n*2* - Cancelar Agendamento";
                sessao.setPassoAtual("ESPERANDO_OPCAO_INICIAL");
                break;

            case "ESPERANDO_OPCAO_INICIAL":
                if (textoLimpo.equals("1")) {
                    respostaDoRobo = "Legal! Para começarmos, qual é o seu nome?";
                    sessao.setPassoAtual("ESPERANDO_NOME");
                }
                else if (textoLimpo.equals("2")) {
                    Agendamento agendamentoEncontrado = agendaService.buscarAgendamentoAtivoPorTelefone(telefone);

                    if (agendamentoEncontrado != null) {
                        sessao.setIdAgendamentoTemporario(agendamentoEncontrado.getId());
                        String dataBonita = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm").format(agendamentoEncontrado.getDataHoraInicio());
                        respostaDoRobo = "Encontrei um agendamento de *" + agendamentoEncontrado.getServicoEscolhido().getNome() + "* para o dia *" + dataBonita + "*.\n\nVocê tem certeza que deseja cancelar? Digite *SIM* para confirmar ou *NAO* para voltar ao menu.";
                        sessao.setPassoAtual("CONFIRMANDO_CANCELAMENTO");
                    } else {
                        respostaDoRobo = "Você não tem nenhum agendamento ativo no momento. Digite *Oi* para voltar.";
                        sessao.setPassoAtual("MENU_INICIAL");
                    }
                }
                else {
                    respostaDoRobo = "Opção inválida. Digite 1 para Agendar ou 2 para Cancelar.";
                }
                break;

            case "CONFIRMANDO_CANCELAMENTO":
                if (textoLimpo.contains("sim")) {
                    agendaService.cancelarAgendamento(sessao.getIdAgendamentoTemporario());
                    respostaDoRobo = "✅ Seu agendamento foi cancelado com sucesso. O horário voltou a ficar livre na agenda!";
                } else {
                    respostaDoRobo = "Tudo bem, não cancelamos nada. Digite *Oi* para recomeçar.";
                }
                sessao.setPassoAtual("MENU_INICIAL");
                sessao.setIdAgendamentoTemporario(null);
                break;

            case "ESPERANDO_NOME":
                sessao.setNomeClienteTemporario(textoRecebido);

                // ✨ CORREÇÃO 2: Busca APENAS os serviços deste barbeiro e que NÃO estão na lixeira.
                List<Servico> listaServicos = servicoRepository.findAllByDonoDoRegistro(barbeiroResponsavel)
                        .stream().filter(s -> s.getAtivo() == null || s.getAtivo()).collect(Collectors.toList());

                if (listaServicos.isEmpty()) {
                    respostaDoRobo = "Ops, eu ainda não tenho nenhum serviço cadastrado no meu sistema. Volte mais tarde!";
                    sessao.setPassoAtual("MENU_INICIAL");
                    break;
                }

                StringBuilder menuServicos = new StringBuilder("Prazer, " + textoRecebido + "! O que deseja agendar para hoje?\n\n");
                for (Servico s : listaServicos) {
                    menuServicos.append(s.getId()).append(" - ").append(s.getNome())
                            .append(" (R$ ").append(s.getPreco()).append(")\n");
                }
                respostaDoRobo = menuServicos.toString();
                sessao.setPassoAtual("ESPERANDO_SERVICO");
                break;

            case "ESPERANDO_SERVICO":
                try {
                    Long idEscolhido = Long.parseLong(textoLimpo);
                    Optional<Servico> servicoEncontrado = servicoRepository.findById(idEscolhido);

                    // Garante que o cliente não escolheu um serviço de outro barbeiro de sacanagem
                    if (servicoEncontrado.isPresent() && servicoEncontrado.get().getDonoDoRegistro().getId().equals(barbeiroResponsavel.getId())) {
                        sessao.setIdServicoTemporario(idEscolhido);
                        respostaDoRobo = "Perfeito. Você escolheu *" + servicoEncontrado.get().getNome() + "*. Para qual dia você deseja agendar? (Digite no formato DD/MM, ex: " + agora.format(DateTimeFormatter.ofPattern("dd/MM")) + "):";
                        sessao.setPassoAtual("ESPERANDO_DATA");
                    } else {
                        respostaDoRobo = "❌ Número inválido. Por favor, olhe o menu acima e digite o número correto do serviço.";
                    }
                } catch (NumberFormatException e) {
                    respostaDoRobo = "⚠️ Não entendi. Por favor, digite apenas o NÚMERO correspondente ao serviço desejado.";
                }
                break;

            case "ESPERANDO_DATA":
                try {
                    int anoAtual = agora.getYear();
                    LocalDate dataDigitada = LocalDate.parse(textoLimpo + "/" + anoAtual, formatadorData);
                    LocalDate dataDeHoje = agora.toLocalDate();

                    if (dataDigitada.isBefore(dataDeHoje)) {
                        respostaDoRobo = "⚠️ Ops, o dia *" + textoLimpo + "* não é possível agendar.\n\nPor favor, digite uma data de hoje em diante (ex: " + dataDeHoje.format(DateTimeFormatter.ofPattern("dd/MM")) + "):";
                    } else {
                        List<LocalTime> horariosLivres = agendaService.buscarHorariosLivres(dataDigitada, sessao.getIdServicoTemporario(), barbeiroResponsavel);

                        if (horariosLivres.isEmpty()) {
                            respostaDoRobo = "😔 Não temos mais horários disponíveis para o dia *" + textoLimpo + "*. Estamos lotados ou fechados.\n\nPor favor, digite outra data (ex: " + dataDeHoje.format(DateTimeFormatter.ofPattern("dd/MM")) + "):";
                        } else {
                            sessao.setDataTemporaria(textoLimpo);

                            StringBuilder mensagemHorarios = new StringBuilder("Certo! Para o dia *" + textoLimpo + "*, temos estes horários livres:\n\n");
                            for (LocalTime h : horariosLivres) {
                                mensagemHorarios.append("⏰ *").append(h.format(formatadorHora)).append("*\n");
                            }
                            mensagemHorarios.append("\nQual horário você prefere? (Digite no formato HH:mm, ex: ").append(horariosLivres.get(0).format(formatadorHora)).append("):");

                            respostaDoRobo = mensagemHorarios.toString();
                            sessao.setPassoAtual("ESPERANDO_HORARIO");
                        }
                    }

                } catch (DateTimeParseException e) {
                    respostaDoRobo = "⚠️ Formato de data inválido! Por favor, digite o dia e o mês separados por barra (ex: " + agora.format(DateTimeFormatter.ofPattern("dd/MM")) + "):";
                }
                break;

            case "ESPERANDO_HORARIO":
                try {
                    LocalTime horaDigitada = LocalTime.parse(textoLimpo);
                    String diaMes = sessao.getDataTemporaria();
                    int anoAtual = agora.getYear();

                    LocalDate dataDigitada = LocalDate.parse(diaMes + "/" + anoAtual, formatadorData);
                    LocalDateTime dataHoraCompleta = LocalDateTime.of(dataDigitada, horaDigitada);

                    Agendamento novoAgendamento = new Agendamento();
                    novoAgendamento.setTelefoneCliente(sessao.getTelefone());
                    novoAgendamento.setNomeCliente(sessao.getNomeClienteTemporario());
                    novoAgendamento.setDataHoraInicio(dataHoraCompleta);
                    novoAgendamento.setDonoDoRegistro(barbeiroResponsavel);

                    Servico servicoEscolhido = servicoRepository.findById(sessao.getIdServicoTemporario()).orElse(null);
                    novoAgendamento.setServicoEscolhido(servicoEscolhido);
                    novoAgendamento.setFormaPagamento("PENDENTE");
                    novoAgendamento.setFaturamentoBarbeiro(BigDecimal.ZERO);

                    boolean sucesso = agendaService.tentarAgendar(novoAgendamento);

                    if (sucesso) {
                        respostaDoRobo = "✅ Tudo certo, " + sessao.getNomeClienteTemporario() + "! Seu agendamento para o dia " + diaMes + " às " + textoLimpo + " está confirmado.";
                        sessao.setPassoAtual("MENU_INICIAL");
                        limparDadosTemporariosDaSessao(sessao);
                    } else {
                        List<LocalTime> horariosLivres = agendaService.buscarHorariosLivres(dataDigitada, sessao.getIdServicoTemporario(), barbeiroResponsavel);

                        if (horariosLivres.isEmpty()) {
                            respostaDoRobo = "❌ Esse horário já está ocupado e não temos mais vagas neste dia. Por favor, mande um *Oi* para recomeçar e escolher outra data.";
                            sessao.setPassoAtual("MENU_INICIAL");
                            limparDadosTemporariosDaSessao(sessao);
                        } else {
                            StringBuilder mensagemHorarios = new StringBuilder("❌ Esse horário já está ocupado ou é inválido. Por favor, escolha um dos horários livres abaixo:\n\n");
                            for (LocalTime h : horariosLivres) {
                                mensagemHorarios.append("⏰ *").append(h.format(formatadorHora)).append("*\n");
                            }
                            respostaDoRobo = mensagemHorarios.toString();
                        }
                    }
                } catch (DateTimeParseException e) {
                    respostaDoRobo = "⚠️ Formato de horário inválido! Por favor, digite a hora com dois pontos (ex: 14:30):";
                }
                break;

            default:
                respostaDoRobo = "Ops, me perdi. Vamos recomeçar? Diga Oi!";
                sessao.setPassoAtual("MENU_INICIAL");
        }

        sessaoRepository.save(sessao);

        List<String> passosComRetorno = List.of("ESPERANDO_NOME", "ESPERANDO_SERVICO", "ESPERANDO_DATA", "ESPERANDO_HORARIO", "CONFIRMANDO_CANCELAMENTO");

        if (passosComRetorno.contains(sessao.getPassoAtual())) {
            respostaDoRobo += "\n\n*Digite 'voltar' para a etapa anterior ou 'cancelar' para sair.*";
        }

        return respostaDoRobo;
    }
}