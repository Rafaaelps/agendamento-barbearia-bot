package com.barber.agendamento_bot.api.repository;

import com.barber.agendamento_bot.api.entity.BloqueioAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;

public interface BloqueioAgendaRepository extends JpaRepository<BloqueioAgenda, Long> {

    // Essa é a pergunta mágica: "Existe algum bloqueio nesta data e hora exatas?"
    boolean existsByDataHoraBloqueada(LocalDateTime dataHora);
}
