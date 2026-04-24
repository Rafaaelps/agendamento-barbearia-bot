package com.barber.agendamento_bot.api.repository;

import com.barber.agendamento_bot.api.entity.HorarioFuncionamento;
import com.barber.agendamento_bot.api.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HorarioRepository extends JpaRepository<HorarioFuncionamento, Long> {
    // ✨ Busca os horários na ordem correta, SÓ do barbeiro solicitado
    List<HorarioFuncionamento> findAllByDonoDoRegistroOrderByDiaDaSemanaAsc(Usuario dono);
    Optional<HorarioFuncionamento> findByDiaDaSemanaAndDonoDoRegistro(Integer dia, Usuario dono);
}