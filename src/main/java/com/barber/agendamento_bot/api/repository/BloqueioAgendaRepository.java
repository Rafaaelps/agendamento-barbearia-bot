package com.barber.agendamento_bot.api.repository;

import com.barber.agendamento_bot.api.entity.BloqueioAgenda;
import com.barber.agendamento_bot.api.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BloqueioAgendaRepository extends JpaRepository<BloqueioAgenda, Long> {
    // ✨ Garante que o sistema busque apenas os bloqueios de um barbeiro específico
    List<BloqueioAgenda> findAllByDonoDoRegistro(Usuario dono);
}