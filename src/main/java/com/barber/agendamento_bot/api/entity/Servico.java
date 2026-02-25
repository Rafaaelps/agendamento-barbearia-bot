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

    // Construtor vazio obrigatório para o Spring/Hibernate funcionar
    public Servico() {}

    public Servico(String nome, BigDecimal preco, Integer duracaoMinutos) {
        this.nome = nome;
        this.preco = preco;
        this.duracaoMinutos = duracaoMinutos;
    }

    // Gere os Getters e Setters aqui (id, nome, preco, duracaoMinutos)
    public Long getId() { return id; }
    public String getNome() { return nome; }
    public Integer getDuracaoMinutos() { return duracaoMinutos; }
    public void setId(Long id) { this.id = id; }
}