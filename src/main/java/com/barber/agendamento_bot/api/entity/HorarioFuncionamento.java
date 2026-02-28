package com.barber.agendamento_bot.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "horarios_funcionamento")
public class HorarioFuncionamento {

    @Id
    private Integer diaDaSemana; // 1 = Segunda, 2 = Terça ... 7 = Domingo

    private String nomeDia;
    private String horaAbertura;
    private String horaFechamento;
    private boolean fechado; // true se for dia de folga

    public HorarioFuncionamento() {}

    public HorarioFuncionamento(Integer diaDaSemana, String nomeDia, String horaAbertura, String horaFechamento, boolean fechado) {
        this.diaDaSemana = diaDaSemana;
        this.nomeDia = nomeDia;
        this.horaAbertura = horaAbertura;
        this.horaFechamento = horaFechamento;
        this.fechado = fechado;
    }

    public Integer getDiaDaSemana() { return diaDaSemana; }
    public void setDiaDaSemana(Integer diaDaSemana) { this.diaDaSemana = diaDaSemana; }
    public String getNomeDia() { return nomeDia; }
    public void setNomeDia(String nomeDia) { this.nomeDia = nomeDia; }
    public String getHoraAbertura() { return horaAbertura; }
    public void setHoraAbertura(String horaAbertura) { this.horaAbertura = horaAbertura; }
    public String getHoraFechamento() { return horaFechamento; }
    public void setHoraFechamento(String horaFechamento) { this.horaFechamento = horaFechamento; }
    public boolean isFechado() { return fechado; }
    public void setFechado(boolean fechado) { this.fechado = fechado; }
}