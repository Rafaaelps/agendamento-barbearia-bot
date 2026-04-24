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
    private java.math.BigDecimal valorTotalHistorico;
    private Boolean lembreteEnviado = false;
    private Boolean confirmadoPeloCliente = false;

    @ManyToOne
    @JoinColumn(name = "servico_id")
    private Servico servicoEscolhido;

    private LocalDateTime dataHoraInicio;
    private LocalDateTime dataHoraFim;
    private String status;
    private java.math.BigDecimal valorFinal;

    // ✨ NOVAS VARIÁVEIS DO SISTEMA SAAS
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario donoDoRegistro;

    private java.math.BigDecimal comissaoDoAdmin;

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

    public java.math.BigDecimal getValorTotalHistorico() { return valorTotalHistorico; }
    public void setValorTotalHistorico(java.math.BigDecimal valorTotalHistorico) { this.valorTotalHistorico = valorTotalHistorico; }

    public Boolean getLembreteEnviado() { return lembreteEnviado; }
    public void setLembreteEnviado(Boolean lembreteEnviado) { this.lembreteEnviado = lembreteEnviado; }

    public Boolean getConfirmadoPeloCliente() { return confirmadoPeloCliente; }
    public void setConfirmadoPeloCliente(Boolean confirmadoPeloCliente) { this.confirmadoPeloCliente = confirmadoPeloCliente; }

    // ✨ GETTERS E SETTERS NOVOS
    public Usuario getDonoDoRegistro() { return donoDoRegistro; }
    public void setDonoDoRegistro(Usuario donoDoRegistro) { this.donoDoRegistro = donoDoRegistro; }

    public java.math.BigDecimal getComissaoDoAdmin() { return comissaoDoAdmin; }
    public void setComissaoDoAdmin(java.math.BigDecimal comissaoDoAdmin) { this.comissaoDoAdmin = comissaoDoAdmin; }
}