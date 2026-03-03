package com.barber.agendamento_bot.api.repository;

import com.barber.agendamento_bot.api.entity.Configuracao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoRepository extends JpaRepository<Configuracao, String> {}