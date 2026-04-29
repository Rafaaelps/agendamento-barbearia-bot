package com.barber.agendamento_bot.api.repository;

import com.barber.agendamento_bot.api.entity.Agendamento;
import com.barber.agendamento_bot.api.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    // =========================================================
    // 1. MÉTODOS DE CONSULTA PARA O PAINEL E LEMBRETES
    // =========================================================

    List<Agendamento> findByStatusNot(String status);

    @Query("SELECT a FROM Agendamento a WHERE a.status = :status AND (a.lembreteEnviado = false OR a.lembreteEnviado IS NULL) AND a.dataHoraInicio BETWEEN :agora AND :daquiA30Min")
    List<Agendamento> buscarAgendamentosParaLembrar(@Param("status") String status, @Param("agora") LocalDateTime agora, @Param("daquiA30Min") LocalDateTime daquiA30Min);

    @Query("SELECT a FROM Agendamento a WHERE a.status = :status AND a.lembreteEnviado = true AND a.dataHoraInicio BETWEEN :agora AND :limite")
    List<Agendamento> buscarNaoConfirmados(@Param("status") String status, @Param("agora") LocalDateTime agora, @Param("limite") LocalDateTime limite);

    // =========================================================
    // 2. MÉTODOS PARA SISTEMA MULTI-USUÁRIO (SÓCIOS/BARBEIROS)
    // =========================================================

    List<Agendamento> findByStatusNotAndDonoDoRegistro(String status, Usuario dono);

    // ✨ Busca a agenda do barbeiro filtrando apenas os status ocupados (AGENDADO/CONFIRMADO)
    List<Agendamento> findByDonoDoRegistroAndStatusIn(Usuario dono, Collection<String> statuses);

    // =========================================================
    // 3. MÉTODOS PARA O CHATBOT (INTELIGÊNCIA E REGRAS)
    // =========================================================

    // ✨ Reconhecimento do cliente: Busca o último registro para saudar pelo nome
    Agendamento findFirstByTelefoneClienteOrderByDataHoraInicioDesc(String telefoneCliente);

    // ✨ Limite Diário: Conta quantos agendamentos ativos o cliente tem no dia
    long countByTelefoneClienteAndDataHoraInicioBetweenAndStatusIn(String telefone, LocalDateTime inicio, LocalDateTime fim, Collection<String> statuses);

    // ✨ Cancelamento Geral: Lista todos os horários pendentes do cliente
    List<Agendamento> findByTelefoneClienteAndStatusInOrderByDataHoraInicioAsc(String telefoneCliente, Collection<String> statuses);

    // ✨ Cancelamento por Limite: Lista os horários específicos de um dia para liberar vaga
    List<Agendamento> findByTelefoneClienteAndDataHoraInicioBetweenAndStatusInOrderByDataHoraInicioAsc(String telefoneCliente, LocalDateTime inicio, LocalDateTime fim, Collection<String> statuses);
}