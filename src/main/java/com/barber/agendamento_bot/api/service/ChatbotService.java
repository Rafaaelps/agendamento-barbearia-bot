package com.barber.agendamento_bot.api.service;

import com.barber.agendamento_bot.api.entity.Agendamento;
import com.barber.agendamento_bot.api.entity.Servico;
import com.barber.agendamento_bot.api.entity.SessaoBot;
import com.barber.agendamento_bot.api.repository.ServicoRepository;
import com.barber.agendamento_bot.api.repository.SessaoBotRepository;
import org.springframework.stereotype.Service;

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

    public String processarMensagem(String telefone, String textoRecebido) {

        SessaoBot sessao = sessaoRepository.findById(telefone).orElse(new SessaoBot(telefone, "MENU_INICIAL"));
        String respostaDoRobo = "";

        // Transforma o texto em minúsculo para facilitar validações
        String textoLimpo = textoRecebido.toLowerCase().trim();

        // =======================================================
        // 🚨 A VÁLVULA DE ESCAPE (O RESET)
        // =======================================================
        if (textoLimpo.equals("cancelar") || textoLimpo.equals("sair")) {
            sessao.setPassoAtual("MENU_INICIAL");
            sessao.setNomeClienteTemporario(null);
            sessao.setIdServicoTemporario(null);
            sessao.setDataTemporaria(null);
            sessao.setIdAgendamentoTemporario(null);
            sessaoRepository.save(sessao);
            return "🛑 Operação cancelada. Quando quiser recomeçar, é só mandar um 'Oi'!";
        }
        // =======================================================

        switch (sessao.getPassoAtual()) {

            case "MENU_INICIAL":
                respostaDoRobo = "Olá! Sou o assistente da Barbearia. O que você deseja fazer?\n*1* - Novo Agendamento\n*2* - Cancelar Agendamento";
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
                        String dataBonita = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm").format(agendamentoEncontrado.getDataHoraInicio());
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
                    // ✨ NOVIDADE: Tenta simular a data antes de salvar na memória!
                    int anoAtual = java.time.LocalDate.now().getYear();
                    java.time.format.DateTimeFormatter formatadorData = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

                    // Se o cliente digitou "27:02", o Java vai estourar um erro AQUI e pular pro "catch"
                    java.time.LocalDate.parse(textoLimpo + "/" + anoAtual, formatadorData);

                    // Se não deu erro, a data é válida! Salvamos e avançamos.
                    sessao.setDataTemporaria(textoLimpo);
                    respostaDoRobo = "Certo! E qual o horário? (ex: 14:30):";
                    sessao.setPassoAtual("ESPERANDO_HORARIO");

                } catch (java.time.format.DateTimeParseException e) {
                    // O cliente errou o formato. O robô avisa e o passo CONTINUA sendo "ESPERANDO_DATA"
                    respostaDoRobo = "⚠️ Formato de data inválido! Por favor, digite o dia e o mês separados por barra (ex: 28/02):";
                }
                break;

            case "ESPERANDO_HORARIO":
                try {
                    java.time.LocalTime horaDigitada = java.time.LocalTime.parse(textoLimpo);
                    String diaMes = sessao.getDataTemporaria();
                    int anoAtual = java.time.LocalDate.now().getYear();

                    java.time.format.DateTimeFormatter formatadorData = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    java.time.LocalDate dataDigitada = java.time.LocalDate.parse(diaMes + "/" + anoAtual, formatadorData);

                    java.time.LocalDateTime dataHoraCompleta = java.time.LocalDateTime.of(dataDigitada, horaDigitada);

                    Agendamento novoAgendamento = new Agendamento();
                    novoAgendamento.setTelefoneCliente(sessao.getTelefone());
                    novoAgendamento.setNomeCliente(sessao.getNomeClienteTemporario());
                    novoAgendamento.setDataHoraInicio(dataHoraCompleta);

                    Servico servicoEscolhido = new Servico();
                    servicoEscolhido.setId(sessao.getIdServicoTemporario());
                    novoAgendamento.setServicoEscolhido(servicoEscolhido);

                    boolean sucesso = agendaService.tentarAgendar(novoAgendamento);

                    if (sucesso) {
                        respostaDoRobo = "✅ Tudo certo, " + sessao.getNomeClienteTemporario() + "! Seu agendamento para " + diaMes + " às " + textoLimpo + " está confirmado.";
                        sessao.setPassoAtual("MENU_INICIAL");
                        sessao.setNomeClienteTemporario(null);
                        sessao.setIdServicoTemporario(null);
                        sessao.setDataTemporaria(null);
                    } else {
                        respostaDoRobo = "❌ Esse horário já está ocupado no dia " + diaMes + ". Por favor, digite outro horário livre:";
                    }
                } catch (java.time.format.DateTimeParseException e) {
                    // Como a data já foi validada no passo anterior, se der erro aqui, a culpa é da HORA!
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