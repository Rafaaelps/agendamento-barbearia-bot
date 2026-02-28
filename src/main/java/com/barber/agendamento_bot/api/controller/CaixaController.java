package com.barber.agendamento_bot.api.controller;

import com.barber.agendamento_bot.api.entity.Agendamento;
import com.barber.agendamento_bot.api.repository.AgendamentoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@RestController
@RequestMapping("/api/caixa")
public class CaixaController {

    private final AgendamentoRepository agendamentoRepository;

    public CaixaController(AgendamentoRepository agendamentoRepository) {
        this.agendamentoRepository = agendamentoRepository;
    }

    @PostMapping("/{id}/pagar")
    public ResponseEntity<String> registrarPagamento(@PathVariable Long id, @RequestParam String metodo) {
        Optional<Agendamento> agendamentoOpt = agendamentoRepository.findById(id);

        if (agendamentoOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Agendamento não encontrado.");
        }

        Agendamento agendamento = agendamentoOpt.get();
        BigDecimal precoBase = agendamento.getServicoEscolhido().getPreco();
        BigDecimal faturamentoLiquido = precoBase;
        String formaPgtoOficial = "";

        // Aplica a matemática Sênior das taxas da maquininha
        if (metodo.equalsIgnoreCase("CREDITO")) {
            formaPgtoOficial = "Cartão de Crédito";
            faturamentoLiquido = precoBase.multiply(new BigDecimal("0.9517")); // Deduz 4,83%
        } else if (metodo.equalsIgnoreCase("DEBITO")) {
            formaPgtoOficial = "Cartão de Débito";
            faturamentoLiquido = precoBase.multiply(new BigDecimal("0.9811")); // Deduz 1,89%
        } else if (metodo.equalsIgnoreCase("PIX_DINHEIRO")) {
            formaPgtoOficial = "Dinheiro / PIX";
            faturamentoLiquido = precoBase; // Deduz 0%
        } else {
            return ResponseEntity.badRequest().body("Método de pagamento inválido.");
        }

        // Arredonda para 2 casas decimais (Moeda Real)
        faturamentoLiquido = faturamentoLiquido.setScale(2, RoundingMode.HALF_UP);

        // Atualiza o banco de dados
        agendamento.setFormaPagamento(formaPgtoOficial);
        agendamento.setFaturamentoBarbeiro(faturamentoLiquido);
        agendamentoRepository.save(agendamento);

        return ResponseEntity.ok("Pagamento registrado: Lucro líquido de R$ " + faturamentoLiquido);
    }
}