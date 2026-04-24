package com.barber.agendamento_bot.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class BloqueioAgenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime dataHoraInicio;
    private LocalDateTime dataHoraFim;
    private String motivo;

    // ✨ NOVA VARIÁVEL DO SISTEMA SAAS
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario donoDoRegistro;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getDataHoraInicio() { return dataHoraInicio; }
    public void setDataHoraInicio(LocalDateTime dataHoraInicio) { this.dataHoraInicio = dataHoraInicio; }

    public LocalDateTime getDataHoraFim() { return dataHoraFim; }
    public void setDataHoraFim(LocalDateTime dataHoraFim) { this.dataHoraFim = dataHoraFim; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    // ✨ GETTERS E SETTERS NOVOS
    public Usuario getDonoDoRegistro() { return donoDoRegistro; }
    public void setDonoDoRegistro(Usuario donoDoRegistro) { this.donoDoRegistro = donoDoRegistro; }
}