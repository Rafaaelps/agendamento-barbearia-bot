package com.barber.agendamento_bot.api.repository;

import com.barber.agendamento_bot.api.entity.Servico;
import com.barber.agendamento_bot.api.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServicoRepository extends JpaRepository<Servico, Long> {
    // ✨ Busca apenas os serviços do usuário logado
    List<Servico> findAllByDonoDoRegistro(Usuario dono);
}