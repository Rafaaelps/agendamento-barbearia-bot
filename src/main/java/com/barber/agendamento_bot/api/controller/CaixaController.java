package com.barber.agendamento_bot.api.controller;

import com.barber.agendamento_bot.api.entity.Agendamento;
import com.barber.agendamento_bot.api.entity.Usuario;
import com.barber.agendamento_bot.api.repository.AgendamentoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping("/api/caixa")
public class CaixaController {

    private final AgendamentoRepository agendamentoRepository;

    public CaixaController(AgendamentoRepository agendamentoRepository) {
        this.agendamentoRepository = agendamentoRepository;
    }

    @PostMapping("/{id}/pagar")
    public ResponseEntity<?> registrarPagamento(@PathVariable Long id, @RequestParam String metodo) {
        Agendamento agendamento = agendamentoRepository.findById(id).orElseThrow();
        agendamento.setFormaPagamento(metodo.toUpperCase());

        Usuario dono = agendamento.getDonoDoRegistro();

        // 1. Puxa as taxas individuais da maquininha
        double taxaCredito = (dono != null && dono.getTaxaCredito() != null) ? dono.getTaxaCredito() : 5.0;
        double taxaDebito = (dono != null && dono.getTaxaDebito() != null) ? dono.getTaxaDebito() : 2.0;

        // 2. Puxa a taxa de COMISSÃO DO SALÃO (A % que fica para o Dono)
        double taxaSalao = (dono != null && dono.getTaxaComissao() != null) ? dono.getTaxaComissao() : 0.0;

        BigDecimal precoCheio = agendamento.getValorFinal() != null ? agendamento.getValorFinal() : agendamento.getServicoEscolhido().getPreco();
        BigDecimal valorAposCartao = precoCheio;

        // 3. Desconta a taxa da maquininha primeiro
        if ("CREDITO".equals(metodo.toUpperCase())) {
            BigDecimal multiplicadorCartao = BigDecimal.ONE.subtract(new BigDecimal(taxaCredito).divide(new BigDecimal("100")));
            valorAposCartao = precoCheio.multiply(multiplicadorCartao);
        } else if ("DEBITO".equals(metodo.toUpperCase())) {
            BigDecimal multiplicadorCartao = BigDecimal.ONE.subtract(new BigDecimal(taxaDebito).divide(new BigDecimal("100")));
            valorAposCartao = precoCheio.multiply(multiplicadorCartao);
        }

        // 4. Divide o dinheiro restante (O que vai pro Barbeiro vs O que vai pro Dono)
        // Se a taxa do salão for 20%, o barbeiro vai multiplicar por 80% (0.80)
        BigDecimal multiplicadorBarbeiro = BigDecimal.ONE.subtract(new BigDecimal(taxaSalao).divide(new BigDecimal("100")));

        BigDecimal faturamentoDoBarbeiro = valorAposCartao.multiply(multiplicadorBarbeiro).setScale(2, RoundingMode.HALF_UP);
        BigDecimal comissaoDoSalao = valorAposCartao.subtract(faturamentoDoBarbeiro).setScale(2, RoundingMode.HALF_UP);

        // 5. Salva a divisão no banco de dados!
        agendamento.setFaturamentoBarbeiro(faturamentoDoBarbeiro);
        agendamento.setComissaoDoAdmin(comissaoDoSalao); // ✨ A parte do salão agora é registrada!
        agendamento.setValorFinal(precoCheio);

        agendamentoRepository.save(agendamento);
        return ResponseEntity.ok().build();
    }
}