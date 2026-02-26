package com.barber.agendamento_bot.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class BloqueioAgenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // O horário exato que o barbeiro não quer atender
    private LocalDateTime dataHoraBloqueada;

    // Para o barbeiro saber por que bloqueou (ex: "Almoço", "Médico")
    private String motivo;

    // --- GETTERS E SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getDataHoraBloqueada() { return dataHoraBloqueada; }
    public void setDataHoraBloqueada(LocalDateTime dataHoraBloqueada) { this.dataHoraBloqueada = dataHoraBloqueada; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}