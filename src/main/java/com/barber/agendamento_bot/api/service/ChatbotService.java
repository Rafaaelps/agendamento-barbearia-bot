package com.barber.agendamento_bot.api.service;

import com.barber.agendamento_bot.api.entity.Agendamento;
import com.barber.agendamento_bot.api.entity.Servico;
import com.barber.agendamento_bot.api.entity.SessaoBot;
import com.barber.agendamento_bot.api.repository.ServicoRepository;
import com.barber.agendamento_bot.api.repository.SessaoBotRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class ChatbotService {

    private final SessaoBotRepository sessaoRepository;
    private final AgendaService agendaService;
    private final ServicoRepository servicoRepository;

    public ChatbotService(SessaoBotRepository sessaoRepository, AgendaService agendaService, ServicoRepository servicoRepository) {
        this.sessaoRepository = sessaoRepository;
        this.agendaService = agendaService;
        this.servicoRepository = servicoRepository;
    }

    @PostConstruct
    public void popularBancoSeEstiverVazio() {
        if (servicoRepository.count() == 0) {
            Servico s1 = new Servico();
            s1.setNome("Corte de Cabelo");
            s1.setPreco(new BigDecimal("35.00"));
            s1.setDuracaoMinutos(30);

            Servico s2 = new Servico();
            s2.setNome("Barba");
            s2.setPreco(new BigDecimal("25.00"));
            s2.setDuracaoMinutos(20);

            Servico s3 = new Servico();
            s3.setNome("Corte + Barba");
            s3.setPreco(new BigDecimal("55.00"));
            s3.setDuracaoMinutos(50);

            servicoRepository.saveAll(List.of(s1, s2, s3));
        }
    }

    private void limparDadosTemporariosDaSessao(SessaoBot sessao) {
        sessao.setNomeClienteTemporario(null);
        sessao.setIdServicoTemporario(null);
        sessao.setDataTemporaria(null);
        sessao.setIdAgendamentoTemporario(null);
    }

    public String processarMensagem(String telefone, String textoRecebido) {

        // ✨ INTERCEPTADOR DE ÁUDIO E IMAGENS
        // A Evolution envia o texto vazio quando recebe mídias sem legenda.
        if (textoRecebido == null || textoRecebido.trim().isEmpty()) {
            return "🎧 Opa! Eu sou um assistente virtual em treinamento e ainda não consigo ouvir áudios ou ver imagens.\n\nPor favor, digite a sua mensagem em texto para eu poder te ajudar!";
        }

        SessaoBot sessao = sessaoRepository.findById(telefone).orElse(new SessaoBot(telefone, "MENU_INICIAL"));
        String respostaDoRobo = "";
        String textoLimpo = textoRecebido.toLowerCase().trim();
        LocalDateTime agora = LocalDateTime.now();

        if (sessao.getUltimaInteracao() != null) {
            long minutosInativos = ChronoUnit.MINUTES.between(sessao.getUltimaInteracao(), agora);
            if (minutosInativos >= 10 && !sessao.getPassoAtual().equals("MENU_INICIAL")) {
                sessao.setPassoAtual("MENU_INICIAL");
                limparDadosTemporariosDaSessao(sessao);
                respostaDoRobo = "⏳ Vi que você demorou um pouquinho, então eu reiniciei nosso atendimento para organizar a agenda, tá bom?\n\n";
            }
        }
        sessao.setUltimaInteracao(agora);

        if (textoLimpo.matches("^(oi|olá|ola|bom dia|boa tarde|boa noite|menu|recomeçar|voltar|cancelar|sair).*")) {
            sessao.setPassoAtual("MENU_INICIAL");
            limparDadosTemporariosDaSessao(sessao);

            if (textoLimpo.equals("cancelar") || textoLimpo.equals("sair")) {
                sessaoRepository.save(sessao);
                return "🛑 Operação cancelada. Quando quiser recomeçar, é só mandar um 'Oi'!";
            }
        }

        DateTimeFormatter formatadorData = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter formatadorHora = DateTimeFormatter.ofPattern("HH:mm");

        switch (sessao.getPassoAtual()) {

            case "MENU_INICIAL":
                respostaDoRobo += "Olá! Sou o assistente da Barbearia. O que você deseja fazer?\n*1* - Novo Agendamento\n*2* - Cancelar Agendamento";
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
                List<Servico> listaServicos = servicoRepository.findAll();

                if (listaServicos.isEmpty()) {
                    respostaDoRobo = "Ops, não encontrei nenhum serviço cadastrado no sistema. Volte mais tarde!";
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

                    if (servicoEncontrado.isPresent()) {
                        sessao.setIdServicoTemporario(idEscolhido);
                        respostaDoRobo = "Perfeito. Você escolheu *" + servicoEncontrado.get().getNome() + "*. Para qual dia você deseja agendar? (Digite no formato DD/MM, ex: 28/02):";
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
                    int anoAtual = LocalDate.now().getYear();
                    LocalDate dataDigitada = LocalDate.parse(textoLimpo + "/" + anoAtual, formatadorData);

                    List<LocalTime> horariosLivres = agendaService.buscarHorariosLivres(dataDigitada, sessao.getIdServicoTemporario());

                    if (horariosLivres.isEmpty()) {
                        respostaDoRobo = "😔 Poxa, não temos mais horários disponíveis para o dia *" + textoLimpo + "*. Estamos lotados ou fechados.\n\nPor favor, digite outra data (ex: 01/03):";
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

                } catch (DateTimeParseException e) {
                    respostaDoRobo = "⚠️ Formato de data inválido! Por favor, digite o dia e o mês separados por barra (ex: 28/02):";
                }
                break;

            case "ESPERANDO_HORARIO":
                try {
                    LocalTime horaDigitada = LocalTime.parse(textoLimpo);
                    String diaMes = sessao.getDataTemporaria();
                    int anoAtual = LocalDate.now().getYear();

                    LocalDate dataDigitada = LocalDate.parse(diaMes + "/" + anoAtual, formatadorData);
                    LocalDateTime dataHoraCompleta = LocalDateTime.of(dataDigitada, horaDigitada);

                    Agendamento novoAgendamento = new Agendamento();
                    novoAgendamento.setTelefoneCliente(sessao.getTelefone());
                    novoAgendamento.setNomeCliente(sessao.getNomeClienteTemporario());
                    novoAgendamento.setDataHoraInicio(dataHoraCompleta);

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
                        List<LocalTime> horariosLivres = agendaService.buscarHorariosLivres(dataDigitada, sessao.getIdServicoTemporario());

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
        return respostaDoRobo;
    }
}