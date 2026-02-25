package com.barber.agendamento_bot.api.repository;

import com.barber.agendamento_bot.api.entity.SessaoBot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessaoBotRepository extends JpaRepository<SessaoBot, String> {
}