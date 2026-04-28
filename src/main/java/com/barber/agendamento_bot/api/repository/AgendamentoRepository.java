package com.barber.agendamento_bot.api.repository;

import com.barber.agendamento_bot.api.entity.Agendamento;
import com.barber.agendamento_bot.api.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    List<Agendamento> findByStatusNot(String status);
    List<Agendamento> findByTelefoneClienteAndStatusNot(String telefoneCliente, String status);

    @Query("SELECT a FROM Agendamento a WHERE a.status = :status AND (a.lembreteEnviado = false OR a.lembreteEnviado IS NULL) AND a.dataHoraInicio BETWEEN :agora AND :daquiA30Min")
    List<Agendamento> buscarAgendamentosParaLembrar(@Param("status") String status, @Param("agora") LocalDateTime agora, @Param("daquiA30Min") LocalDateTime daquiA30Min);

    @Query("SELECT a FROM Agendamento a WHERE a.status = :status AND a.lembreteEnviado = true AND a.dataHoraInicio BETWEEN :agora AND :limite")
    List<Agendamento> buscarNaoConfirmados(@Param("status") String status, @Param("agora") LocalDateTime agora, @Param("limite") LocalDateTime limite);

    // ✨ NOVOS MÉTODOS PARA O SISTEMA DE MULTI-USUÁRIOS
    List<Agendamento> findByStatusNotAndDonoDoRegistro(String status, Usuario dono);
    List<Agendamento> findByTelefoneClienteAndStatusNotAndDonoDoRegistro(String telefoneCliente, String status, Usuario dono);

    long countByTelefoneClienteAndDataHoraInicioBetweenAndStatusNot(
            String telefone,
            LocalDateTime inicio,
            LocalDateTime fim,
            String status
    );

    // Busca todos os agendamentos ativos de um cliente (Para o Menu de Cancelamento)
    List<Agendamento> findByTelefoneClienteAndStatusNotOrderByDataHoraInicioAsc(String telefoneCliente, String status);

    // Busca os agendamentos ativos em um dia específico (Para o Menu de Limite Diário)
    List<Agendamento> findByTelefoneClienteAndDataHoraInicioBetweenAndStatusNotOrderByDataHoraInicioAsc(String telefoneCliente, LocalDateTime inicio, LocalDateTime fim, String status);
}