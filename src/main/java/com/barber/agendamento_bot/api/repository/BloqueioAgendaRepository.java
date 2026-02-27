package com.barber.agendamento_bot.api.repository;

import com.barber.agendamento_bot.api.entity.BloqueioAgenda;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BloqueioAgendaRepository extends JpaRepository<BloqueioAgenda, Long> {
    // Apagamos a regra antiga. O Service vai fazer a checagem inteligente agora.
}