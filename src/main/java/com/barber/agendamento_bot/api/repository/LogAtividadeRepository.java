package com.barber.agendamento_bot.api.repository;

import com.barber.agendamento_bot.api.entity.LogAtividade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogAtividadeRepository extends JpaRepository<LogAtividade, Long> {
    // O Spring JPA já nos fornece o findAll(Sort sort) automaticamente por padrão!
}