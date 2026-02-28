package com.barber.agendamento_bot.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "agendamentos")
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String telefoneCliente;
    private String nomeCliente;
    private String formaPagamento;
    private java.math.BigDecimal faturamentoBarbeiro;

    @ManyToOne
    @JoinColumn(name = "servico_id")
    private Servico servicoEscolhido;

    private LocalDateTime dataHoraInicio;
    private LocalDateTime dataHoraFim;
    private String status;
    private java.math.BigDecimal valorFinal;

    // 1. O Construtor vazio é TUDO que o Spring e o Postman precisam!
    // Apague o outro construtor que fazia o cálculo.
    public Agendamento() {}

    // --- GETTERS E SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTelefoneCliente() { return telefoneCliente; }
    public void setTelefoneCliente(String telefoneCliente) { this.telefoneCliente = telefoneCliente; }

    public String getNomeCliente() { return nomeCliente; }
    public void setNomeCliente(String nomeCliente) { this.nomeCliente = nomeCliente; }

    public Servico getServicoEscolhido() { return servicoEscolhido; }
    public void setServicoEscolhido(Servico servicoEscolhido) { this.servicoEscolhido = servicoEscolhido; }

    public LocalDateTime getDataHoraInicio() { return dataHoraInicio; }
    public void setDataHoraInicio(LocalDateTime dataHoraInicio) { this.dataHoraInicio = dataHoraInicio; }

    public LocalDateTime getDataHoraFim() { return dataHoraFim; }
    public void setDataHoraFim(LocalDateTime dataHoraFim) { this.dataHoraFim = dataHoraFim; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public java.math.BigDecimal getValorFinal() { return valorFinal; }
    public void setValorFinal(java.math.BigDecimal valorFinal) { this.valorFinal = valorFinal; }

    public String getFormaPagamento() { return formaPagamento; }

    public void setFormaPagamento(String formaPagamento) { this.formaPagamento = formaPagamento; }

    public java.math.BigDecimal getFaturamentoBarbeiro() { return faturamentoBarbeiro;}

    public void setFaturamentoBarbeiro(java.math.BigDecimal faturamentoBarbeiro) { this.faturamentoBarbeiro = faturamentoBarbeiro; }
}