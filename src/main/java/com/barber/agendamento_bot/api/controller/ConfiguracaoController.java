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

    @GetMapping("/geral")
    public Map<String, Object> getConfigs() {
        String credito = repository.findById("TAXA_CREDITO").map(Configuracao::getValor).orElse("5.0");
        String debito = repository.findById("TAXA_DEBITO").map(Configuracao::getValor).orElse("2.0");

        String botAtivo = repository.findById("BOT_CONFIRMACAO_ATIVO").map(Configuracao::getValor).orElse("false");
        String minutos = repository.findById("BOT_MINUTOS_CONFIRMACAO").map(Configuracao::getValor).orElse("35");

        Map<String, Object> configs = new HashMap<>();
        configs.put("taxaCredito", Double.parseDouble(credito));
        configs.put("taxaDebito", Double.parseDouble(debito));
        configs.put("botAtivo", Boolean.parseBoolean(botAtivo));
        configs.put("minutosConfirmacao", Integer.parseInt(minutos));

        return configs;
    }

    @PostMapping("/geral")
    public void setConfigs(@RequestBody Map<String, Object> configs) {
        if (configs.containsKey("taxaCredito"))
            repository.save(new Configuracao("TAXA_CREDITO", String.valueOf(configs.get("taxaCredito"))));
        if (configs.containsKey("taxaDebito"))
            repository.save(new Configuracao("TAXA_DEBITO", String.valueOf(configs.get("taxaDebito"))));
        if (configs.containsKey("botAtivo"))
            repository.save(new Configuracao("BOT_CONFIRMACAO_ATIVO", String.valueOf(configs.get("botAtivo"))));
        if (configs.containsKey("minutosConfirmacao"))
            repository.save(new Configuracao("BOT_MINUTOS_CONFIRMACAO", String.valueOf(configs.get("minutosConfirmacao"))));
    }

    // Mantém a rota antiga "/taxas" funcionando para o financeiro.html não quebrar
    @GetMapping("/taxas")
    public Map<String, Double> getTaxas() {
        Map<String, Object> geral = getConfigs();
        Map<String, Double> taxas = new HashMap<>();
        taxas.put("credito", (Double) geral.get("taxaCredito"));
        taxas.put("debito", (Double) geral.get("taxaDebito"));
        return taxas;
    }

    @PostMapping("/taxas")
    public void setTaxas(@RequestBody Map<String, Double> taxas) {
        Map<String, Object> formatado = new HashMap<>();
        if (taxas.containsKey("credito")) formatado.put("taxaCredito", taxas.get("credito"));
        if (taxas.containsKey("debito")) formatado.put("taxaDebito", taxas.get("debito"));
        setConfigs(formatado);
    }
}