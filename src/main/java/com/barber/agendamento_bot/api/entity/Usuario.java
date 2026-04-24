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

    // "ADMIN" (Dono) ou "BARBEIRO" (Funcionário)
    private String perfil;

    // A instância exata da Evolution API deste barbeiro (Ex: "barbeiro_joao")
    private String instanciaWhatsapp;

    // Quanto o dono tira desse barbeiro. (Ex: 40.0 significa que o ADMIN ganha 40%)
    private Double taxaComissao;

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
}