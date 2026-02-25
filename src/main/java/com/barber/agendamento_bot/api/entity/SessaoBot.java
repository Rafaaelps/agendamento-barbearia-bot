package com.barber.agendamento_bot.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class SessaoBot {

    // O ID dessa tabela vai ser o próprio número de telefone!
    @Id
    private String telefone;

    private Long idAgendamentoTemporario;

    // Guarda onde o cliente parou (ex: "MENU", "ESPERANDO_NOME", "ESPERANDO_SERVICO")
    private String passoAtual;

    // Campos temporários para guardar as respostas do cliente
    private String nomeClienteTemporario;
    private Long idServicoTemporario;

    public SessaoBot() {}

    public SessaoBot(String telefone, String passoAtual) {
        this.telefone = telefone;
        this.passoAtual = passoAtual;
    }

    // --- GETTERS E SETTERS ---
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getPassoAtual() { return passoAtual; }
    public void setPassoAtual(String passoAtual) { this.passoAtual = passoAtual; }

    public String getNomeClienteTemporario() { return nomeClienteTemporario; }
    public void setNomeClienteTemporario(String nomeClienteTemporario) { this.nomeClienteTemporario = nomeClienteTemporario; }

    public Long getIdServicoTemporario() { return idServicoTemporario; }
    public void setIdServicoTemporario(Long idServicoTemporario) { this.idServicoTemporario = idServicoTemporario; }

    public Long getIdAgendamentoTemporario() { return idAgendamentoTemporario; }
    public void setIdAgendamentoTemporario(Long idAgendamentoTemporario) { this.idAgendamentoTemporario = idAgendamentoTemporario; }

}