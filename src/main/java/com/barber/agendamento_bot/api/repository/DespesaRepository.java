package com.barber.agendamento_bot.api.repository;

import com.barber.agendamento_bot.api.entity.Despesa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DespesaRepository extends JpaRepository<Despesa, Long> {}