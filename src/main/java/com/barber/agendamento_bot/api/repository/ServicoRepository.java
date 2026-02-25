package com.barber.agendamento_bot.api.repository;


import com.barber.agendamento_bot.api.entity.Servico;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicoRepository extends JpaRepository<Servico, Long> {
}
