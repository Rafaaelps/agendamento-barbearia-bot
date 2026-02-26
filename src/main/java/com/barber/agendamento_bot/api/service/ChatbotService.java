package com.barber.agendamento_bot.api.service;

import com.barber.agendamento_bot.api.entity.Agendamento;
import com.barber.agendamento_bot.api.entity.Servico;
import com.barber.agendamento_bot.api.entity.SessaoBot;
import com.barber.agendamento_bot.api.repository.SessaoBotRepository;
import org.springframework.stereotype.Service;


@Service
public class ChatbotService {

    private final SessaoBotRepository sessaoRepository;
    private final AgendaService agendaService;

    public ChatbotService(SessaoBotRepository sessaoRepository, AgendaService agendaService) {
        this.sessaoRepository = sessaoRepository;
        this.agendaService = agendaService;
    }

    public String processarMensagem(String telefone, String textoRecebido) {

        SessaoBot sessao = sessaoRepository.findById(telefone).orElse(new SessaoBot(telefone, "MENU_INICIAL"));
        String respostaDoRobo = "";

        // Transforma o texto em minúsculo para facilitar se o cliente digitar "Sim" ou "SIM"
        String textoLimpo = textoRecebido.toLowerCase().trim();

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
                    // O cliente quer cancelar. O Java vai no banco procurar pelo telefone dele!
                    Agendamento agendamentoEncontrado = agendaService.buscarAgendamentoAtivoPorTelefone(telefone);

                    if (agendamentoEncontrado != null) {
                        // Achou! Guarda o ID na memória e pede confirmação
                        sessao.setIdAgendamentoTemporario(agendamentoEncontrado.getId());

                        // Formata a data para ficar legível
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
                    // Chama a mesma função que o dono da barbearia usa no botão vermelho do site!
                    agendaService.cancelarAgendamento(sessao.getIdAgendamentoTemporario());
                    respostaDoRobo = "✅ Seu agendamento foi cancelado com sucesso. O horário voltou a ficar livre na agenda!";
                } else {
                    respostaDoRobo = "Tudo bem, não cancelamos nada. Digite *Oi* para recomeçar.";
                }
                // Limpa a memória
                sessao.setPassoAtual("MENU_INICIAL");
                sessao.setIdAgendamentoTemporario(null);
                break;

            // ... (A PARTIR DAQUI SÃO OS PASSOS QUE VOCÊ JÁ TINHA, SEM MUDANÇAS) ...

            case "ESPERANDO_NOME":
                sessao.setNomeClienteTemporario(textoRecebido);
                respostaDoRobo = "Prazer, " + textoRecebido + "! O que deseja agendar para hoje?\n1 - Corte (R$ 50)\n2 - Barba (R$ 30)";
                sessao.setPassoAtual("ESPERANDO_SERVICO");
                break;

            case "ESPERANDO_SERVICO":
                if (textoLimpo.equals("1") || textoLimpo.equals("2")) {
                    sessao.setIdServicoTemporario(Long.parseLong(textoLimpo));

                    // Em vez de pedir a hora, agora pedimos o dia!
                    respostaDoRobo = "Perfeito. Para qual dia você deseja agendar? (Digite no formato DD/MM, ex: 28/02):";
                    sessao.setPassoAtual("ESPERANDO_DATA");
                } else {
                    respostaDoRobo = "Não entendi. Por favor, digite 1 para Corte ou 2 para Barba.";
                }
                break;


            case "ESPERANDO_DATA":
                // Vamos guardar o texto que o cliente digitou (ex: "28/02")
                sessao.setDataTemporaria(textoLimpo);

                respostaDoRobo = "Certo! E qual o horário? (ex: 14:30):";
                sessao.setPassoAtual("ESPERANDO_HORARIO");
                break;

            case "ESPERANDO_HORARIO":
                try {
                    // 1. Pegamos a hora que o cliente acabou de digitar
                    java.time.LocalTime horaDigitada = java.time.LocalTime.parse(textoLimpo);

                    // 2. Pegamos aquele dia que estava guardado na memória (ex: "28/02")
                    String diaMes = sessao.getDataTemporaria();
                    // Adicionamos o ano atual para o Java conseguir entender a data completa
                    int anoAtual = java.time.LocalDate.now().getYear();

                    // O formato fica tipo "28/02/2026"
                    java.time.format.DateTimeFormatter formatadorData = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    java.time.LocalDate dataDigitada = java.time.LocalDate.parse(diaMes + "/" + anoAtual, formatadorData);

                    // 3. Juntamos o dia e a hora perfeitos!
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
                        respostaDoRobo = "✅ Tudo certo, " + sessao.getNomeClienteTemporario() + "! Seu agendamento para " + diaMes + " às " + textoLimpo + " está confirmadíssimo.";
                        sessao.setPassoAtual("MENU_INICIAL");
                        // Limpa a memória
                        sessao.setNomeClienteTemporario(null);
                        sessao.setIdServicoTemporario(null);
                        sessao.setDataTemporaria(null);
                    } else {
                        respostaDoRobo = "❌ Poxa, esse horário já está ocupado no dia " + diaMes + ". Por favor, digite outro horário:";
                    }
                } catch (java.time.format.DateTimeParseException e) {
                    respostaDoRobo = "⚠️ Ops, não entendi o formato. Certifique-se de que o dia foi digitado como DD/MM (ex: 28/02) no passo anterior, e a hora com dois pontos (ex: 14:30). Vamos tentar o horário de novo:";
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