package com.barber.agendamento_bot.api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "servicos")
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private BigDecimal preco;

    @Column(name = "duracao_minutos")
    private Integer duracaoMinutos;

    // ✨ NOVA VARIÁVEL DO SISTEMA SAAS
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario donoDoRegistro;

    public Servico() {}

    public Servico(String nome, BigDecimal preco, Integer duracaoMinutos) {
        this.nome = nome;
        this.preco = preco;
        this.duracaoMinutos = duracaoMinutos;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public Integer getDuracaoMinutos() { return duracaoMinutos; }
    public void setDuracaoMinutos(Integer duracaoMinutos) { this.duracaoMinutos = duracaoMinutos; }

    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }

    // ✨ GETTERS E SETTERS NOVOS
    public Usuario getDonoDoRegistro() { return donoDoRegistro; }
    public void setDonoDoRegistro(Usuario donoDoRegistro) { this.donoDoRegistro = donoDoRegistro; }
}