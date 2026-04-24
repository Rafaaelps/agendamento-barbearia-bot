package com.barber.agendamento_bot.api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Column(unique = true)
    private String login;

    private String senha;
    private String perfil;
    private String instanciaWhatsapp;
    private Double taxaComissao;

    // ✨ NOVAS VARIÁVEIS PARA O NOVO RECURSO
    private Boolean ativo = true; // Soft Delete (Lixeira Inteligente)
    private Double taxaCredito = 5.0; // Taxa padrão inicial
    private Double taxaDebito = 2.0;
    private Boolean botAtivo = false;
    private Integer minutosConfirmacao = 30;

    // --- GETTERS E SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getPerfil() { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }
    public String getInstanciaWhatsapp() { return instanciaWhatsapp; }
    public void setInstanciaWhatsapp(String instanciaWhatsapp) { this.instanciaWhatsapp = instanciaWhatsapp; }
    public Double getTaxaComissao() { return taxaComissao; }
    public void setTaxaComissao(Double taxaComissao) { this.taxaComissao = taxaComissao; }

    // ✨ NOVOS GETTERS E SETTERS
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    public Double getTaxaCredito() { return taxaCredito; }
    public void setTaxaCredito(Double taxaCredito) { this.taxaCredito = taxaCredito; }
    public Double getTaxaDebito() { return taxaDebito; }
    public void setTaxaDebito(Double taxaDebito) { this.taxaDebito = taxaDebito; }
    public Boolean getBotAtivo() { return botAtivo; }
    public void setBotAtivo(Boolean botAtivo) { this.botAtivo = botAtivo; }
    public Integer getMinutosConfirmacao() { return minutosConfirmacao; }
    public void setMinutosConfirmacao(Integer minutosConfirmacao) { this.minutosConfirmacao = minutosConfirmacao; }
}