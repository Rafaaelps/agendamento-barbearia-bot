package com.barber.agendamento_bot.api.controller;

import com.barber.agendamento_bot.api.entity.Agendamento;
import com.barber.agendamento_bot.api.entity.Usuario;
import com.barber.agendamento_bot.api.repository.AgendamentoRepository;
import com.barber.agendamento_bot.api.repository.ConfiguracaoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping("/api/caixa")
public class CaixaController {

    private final AgendamentoRepository agendamentoRepository;
    private final ConfiguracaoRepository configuracaoRepository; // ✨ NOVO

    public CaixaController(AgendamentoRepository agendamentoRepository, ConfiguracaoRepository configuracaoRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.configuracaoRepository = configuracaoRepository;
    }

    @PostMapping("/{id}/pagar")
    public ResponseEntity<?> registrarPagamento(@PathVariable Long id, @RequestParam String metodo) {
        Agendamento agendamento = agendamentoRepository.findById(id).orElseThrow();
        agendamento.setFormaPagamento(metodo.toUpperCase());

        // ✨ PUXA AS TAXAS DINÂMICAS DO BANCO DE DADOS
        Usuario dono = agendamento.getDonoDoRegistro();
        double taxaCredito = (dono != null && dono.getTaxaCredito() != null) ? dono.getTaxaCredito() : 5.0;
        double taxaDebito = (dono != null && dono.getTaxaDebito() != null) ? dono.getTaxaDebito() : 2.0;

        BigDecimal precoCheio = agendamento.getServicoEscolhido().getPreco();
        BigDecimal valorLiquido = precoCheio;

        if ("CREDITO".equals(metodo.toUpperCase())) {
            BigDecimal multiplicador = BigDecimal.ONE.subtract(new BigDecimal(taxaCredito).divide(new BigDecimal("100")));
            valorLiquido = precoCheio.multiply(multiplicador);
        } else if ("DEBITO".equals(metodo.toUpperCase())) {
            BigDecimal multiplicador = BigDecimal.ONE.subtract(new BigDecimal(taxaDebito).divide(new BigDecimal("100")));
            valorLiquido = precoCheio.multiply(multiplicador);
        }

        agendamento.setFaturamentoBarbeiro(valorLiquido.setScale(2, RoundingMode.HALF_UP));
        agendamento.setValorFinal(precoCheio);

        agendamentoRepository.save(agendamento);
        return ResponseEntity.ok().build();
    }
}