package com.barber.agendamento_bot.api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "horarios")
public class HorarioFuncionamento {

    // ✨ MUDANÇA CRÍTICA: Agora tem um ID próprio e o dono do registro
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer diaDaSemana; // 1 = Segunda, 7 = Domingo
    private String nomeDia;
    private String horaAbertura;
    private String horaFechamento;
    private boolean fechado;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario donoDoRegistro;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public Usuario getDonoDoRegistro() { return donoDoRegistro; }
    public void setDonoDoRegistro(Usuario donoDoRegistro) { this.donoDoRegistro = donoDoRegistro; }
}