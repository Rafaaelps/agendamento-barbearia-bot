package com.barber.agendamento_bot.api.dto;

// Esta classe não tem @Entity porque NÃO vai para o banco de dados.
// Ela só serve para ler o JSON que o WhatsApp mandou.
public class MensagemWhatsAppDTO {

    private String numeroCliente;
    private String textoMensagem;

    // Construtor vazio para o Spring conseguir preencher
    public MensagemWhatsAppDTO() {}

    // Getters e Setters
    public String getNumeroCliente() { return numeroCliente; }
    public void setNumeroCliente(String numeroCliente) { this.numeroCliente = numeroCliente; }

    public String getTextoMensagem() { return textoMensagem; }
    public void setTextoMensagem(String textoMensagem) { this.textoMensagem = textoMensagem; }
}