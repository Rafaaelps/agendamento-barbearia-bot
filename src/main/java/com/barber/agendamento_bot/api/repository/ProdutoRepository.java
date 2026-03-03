package com.barber.agendamento_bot.api.repository;

import com.barber.agendamento_bot.api.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {}