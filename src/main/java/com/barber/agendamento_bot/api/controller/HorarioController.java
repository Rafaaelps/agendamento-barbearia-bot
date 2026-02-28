package com.barber.agendamento_bot.api.controller;

import com.barber.agendamento_bot.api.entity.HorarioFuncionamento;
import com.barber.agendamento_bot.api.repository.HorarioRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/horarios")
public class HorarioController {

    private final HorarioRepository horarioRepository;

    public HorarioController(HorarioRepository horarioRepository) {
        this.horarioRepository = horarioRepository;
    }

    @PostConstruct
    public void popularHorariosPadrao() {
        if (horarioRepository.count() == 0) {
            horarioRepository.save(new HorarioFuncionamento(1, "Segunda-feira", "08:00", "19:00", false));
            horarioRepository.save(new HorarioFuncionamento(2, "Terça-feira", "08:00", "19:00", false));
            horarioRepository.save(new HorarioFuncionamento(3, "Quarta-feira", "08:00", "19:00", false));
            horarioRepository.save(new HorarioFuncionamento(4, "Quinta-feira", "08:00", "19:00", false));
            horarioRepository.save(new HorarioFuncionamento(5, "Sexta-feira", "08:00", "19:00", false));
            horarioRepository.save(new HorarioFuncionamento(6, "Sábado", "08:00", "17:00", false));
            horarioRepository.save(new HorarioFuncionamento(7, "Domingo", "00:00", "00:00", true));
        }
    }

    @GetMapping
    public List<HorarioFuncionamento> listarHorarios() {
        return horarioRepository.findAll();
    }

    @PutMapping
    public void atualizarHorarios(@RequestBody List<HorarioFuncionamento> horariosAtualizados) {
        horarioRepository.saveAll(horariosAtualizados);
    }
}