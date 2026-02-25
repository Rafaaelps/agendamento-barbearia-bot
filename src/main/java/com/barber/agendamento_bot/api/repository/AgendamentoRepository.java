package com.barber.agendamento_bot.api.repository;

import com.barber.agendamento_bot.api.entity.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
    // O Spring cria o SQL sozinho só de ler o nome deste métod!
    // Ele vai buscar todos os agendamentos que não estejam CANCELADOS
    List<Agendamento> findByStatusNot(String status);

    //Buscar agendamento pelo numero de telefone
    List<Agendamento> findByTelefoneClienteAndStatusNot(String telefoneCliente, String status);
}