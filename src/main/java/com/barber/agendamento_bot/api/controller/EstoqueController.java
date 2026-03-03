package com.barber.agendamento_bot.api.controller;

import com.barber.agendamento_bot.api.entity.Produto;
import com.barber.agendamento_bot.api.entity.Venda;
import com.barber.agendamento_bot.api.repository.ConfiguracaoRepository;
import com.barber.agendamento_bot.api.repository.ProdutoRepository;
import com.barber.agendamento_bot.api.repository.VendaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/estoque")
public class EstoqueController {

    private final ProdutoRepository produtoRepository;
    private final VendaRepository vendaRepository;
    private final ConfiguracaoRepository configuracaoRepository; // ✨ NOVO

    public EstoqueController(ProdutoRepository produtoRepository, VendaRepository vendaRepository, ConfiguracaoRepository configuracaoRepository) {
        this.produtoRepository = produtoRepository;
        this.vendaRepository = vendaRepository;
        this.configuracaoRepository = configuracaoRepository;
    }

    @GetMapping("/produtos")
    public List<Produto> listarProdutos() {
        return produtoRepository.findAll().stream()
                .filter(p -> p.getAtivo() == null || p.getAtivo())
                .collect(Collectors.toList());
    }

    @PostMapping("/produtos")
    public Produto salvarProduto(@RequestBody Produto produto) {
        produto.setNome(produto.getNome().trim());
        produto.setAtivo(true);
        return produtoRepository.save(produto);
    }

    @PutMapping("/produtos/{id}")
    public ResponseEntity<?> editarProduto(@PathVariable Long id, @RequestBody Produto dadosAtualizados) {
        Produto produto = produtoRepository.findById(id).orElseThrow();
        produto.setNome(dadosAtualizados.getNome().trim());
        produto.setPreco(dadosAtualizados.getPreco());
        produto.setQuantidadeEstoque(dadosAtualizados.getQuantidadeEstoque());
        produtoRepository.save(produto);
        return ResponseEntity.ok(produto);
    }

    @PutMapping("/produtos/{id}/repor")
    public ResponseEntity<?> reporEstoque(@PathVariable Long id, @RequestParam Integer quantidade) {
        Produto produto = produtoRepository.findById(id).orElseThrow();
        produto.setQuantidadeEstoque(produto.getQuantidadeEstoque() + quantidade);
        produtoRepository.save(produto);
        return ResponseEntity.ok(produto);
    }

    @DeleteMapping("/produtos/{id}")
    public ResponseEntity<?> excluirProduto(@PathVariable Long id) {
        Produto produto = produtoRepository.findById(id).orElseThrow();
        produto.setAtivo(false);
        produtoRepository.save(produto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/vender")
    public ResponseEntity<?> registrarVenda(@RequestParam Long produtoId, @RequestParam Integer quantidade, @RequestParam String pagamento) {
        Produto produto = produtoRepository.findById(produtoId).orElseThrow();

        if (produto.getQuantidadeEstoque() < quantidade) {
            return ResponseEntity.badRequest().body("Estoque insuficiente!");
        }

        produto.setQuantidadeEstoque(produto.getQuantidadeEstoque() - quantidade);
        produtoRepository.save(produto);

        // ✨ PUXA AS TAXAS DINÂMICAS DO BANCO DE DADOS
        double taxaCredito = configuracaoRepository.findById("TAXA_CREDITO").map(c -> Double.parseDouble(c.getValor())).orElse(5.0);
        double taxaDebito = configuracaoRepository.findById("TAXA_DEBITO").map(c -> Double.parseDouble(c.getValor())).orElse(2.0);

        BigDecimal precoCheio = produto.getPreco().multiply(new BigDecimal(quantidade));
        BigDecimal valorLiquido = precoCheio;

        if ("CREDITO".equalsIgnoreCase(pagamento)) {
            BigDecimal multiplicador = BigDecimal.ONE.subtract(new BigDecimal(taxaCredito).divide(new BigDecimal("100")));
            valorLiquido = precoCheio.multiply(multiplicador);
        } else if ("DEBITO".equalsIgnoreCase(pagamento)) {
            BigDecimal multiplicador = BigDecimal.ONE.subtract(new BigDecimal(taxaDebito).divide(new BigDecimal("100")));
            valorLiquido = precoCheio.multiply(multiplicador);
        }

        Venda venda = new Venda();
        venda.setProduto(produto);
        venda.setQuantidade(quantidade);
        venda.setFormaPagamento(pagamento);
        venda.setValorTotal(valorLiquido.setScale(2, RoundingMode.HALF_UP));
        venda.setDataHoraVenda(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));

        vendaRepository.save(venda);
        return ResponseEntity.ok(venda);
    }

    @GetMapping("/vendas")
    public List<Venda> listarVendas() {
        return vendaRepository.findAll();
    }
}