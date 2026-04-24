package com.barber.agendamento_bot.api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Produto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private BigDecimal preco;
    private Integer quantidadeEstoque;
    private Boolean ativo = true;

    // ✨ NOVA VARIÁVEL DO SISTEMA SAAS
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario donoDoRegistro;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }

    public Integer getQuantidadeEstoque() { return quantidadeEstoque; }
    public void setQuantidadeEstoque(Integer quantidadeEstoque) { this.quantidadeEstoque = quantidadeEstoque; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }

    // ✨ GETTERS E SETTERS NOVOS
    public Usuario getDonoDoRegistro() { return donoDoRegistro; }
    public void setDonoDoRegistro(Usuario donoDoRegistro) { this.donoDoRegistro = donoDoRegistro; }
}