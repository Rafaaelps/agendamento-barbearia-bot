package com.barber.agendamento_bot.api.repository;

import com.barber.agendamento_bot.api.entity.Venda;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendaRepository extends JpaRepository<Venda, Long> {}