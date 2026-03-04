package com.barber.agendamento_bot.api.controller;

import com.barber.agendamento_bot.api.entity.Despesa;
import com.barber.agendamento_bot.api.repository.DespesaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/despesas")
public class DespesaController {

    private final DespesaRepository repository;

    public DespesaController(DespesaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Despesa> listarTodas() {
        return repository.findAll();
    }

    @PostMapping
    public Despesa adicionarDespesa(@RequestBody Despesa despesa) {
        // Grava a data e hora exata em que o barbeiro lançou o gasto
        despesa.setDataHora(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));
        return repository.save(despesa);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> apagarDespesa(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
