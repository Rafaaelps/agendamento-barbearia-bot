package com.barber.agendamento_bot.api.repository;

import com.barber.agendamento_bot.api.entity.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
    // O Spring cria o SQL sozinho só de ler o nome deste métod!
    // Ele vai buscar todos os agendamentos que não estejam CANCELADOS
    List<Agendamento> findByStatusNot(String status);

    //Buscar agendamento pelo numero de telefone
    List<Agendamento> findByTelefoneClienteAndStatusNot(String telefoneCliente, String status);

    // lembrete para o cliente
    @Query("SELECT a FROM Agendamento a WHERE a.status = :status AND (a.lembreteEnviado = false OR a.lembreteEnviado IS NULL) AND a.dataHoraInicio BETWEEN :agora AND :daquiA30Min")
    List<Agendamento> buscarAgendamentosParaLembrar(
            @Param("status") String status,
            @Param("agora") LocalDateTime agora,
            @Param("daquiA30Min") LocalDateTime daquiA30Min);
}