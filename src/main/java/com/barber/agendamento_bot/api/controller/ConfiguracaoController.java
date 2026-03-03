package com.barber.agendamento_bot.api.controller;

import com.barber.agendamento_bot.api.entity.Configuracao;
import com.barber.agendamento_bot.api.repository.ConfiguracaoRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/configuracoes")
public class ConfiguracaoController {

    private final ConfiguracaoRepository repository;

    public ConfiguracaoController(ConfiguracaoRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/taxas")
    public Map<String, Double> getTaxas() {
        // Se não tiver nada no banco, assume 5% e 2% como padrão inicial
        double credito = repository.findById("TAXA_CREDITO").map(c -> Double.parseDouble(c.getValor())).orElse(5.0);
        double debito = repository.findById("TAXA_DEBITO").map(c -> Double.parseDouble(c.getValor())).orElse(2.0);

        Map<String, Double> taxas = new HashMap<>();
        taxas.put("credito", credito);
        taxas.put("debito", debito);
        return taxas;
    }

    @PostMapping("/taxas")
    public void setTaxas(@RequestBody Map<String, Double> taxas) {
        if (taxas.containsKey("credito")) {
            repository.save(new Configuracao("TAXA_CREDITO", String.valueOf(taxas.get("credito"))));
        }
        if (taxas.containsKey("debito")) {
            repository.save(new Configuracao("TAXA_DEBITO", String.valueOf(taxas.get("debito"))));
        }
    }
}