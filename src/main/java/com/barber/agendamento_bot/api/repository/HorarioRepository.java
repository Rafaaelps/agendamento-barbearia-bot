package com.barber.agendamento_bot.api.repository;

import com.barber.agendamento_bot.api.entity.HorarioFuncionamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HorarioRepository extends JpaRepository<HorarioFuncionamento, Integer> {
}