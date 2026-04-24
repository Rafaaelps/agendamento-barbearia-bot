package com.barber.agendamento_bot.api.repository;

import com.barber.agendamento_bot.api.entity.Produto;
import com.barber.agendamento_bot.api.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    // ✨ Busca apenas produtos ativos E que pertençam ao usuário logado
    List<Produto> findByAtivoTrueAndDonoDoRegistro(Usuario dono);
}