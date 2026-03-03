package com.barber.agendamento_bot.api.controller;

import com.barber.agendamento_bot.api.entity.Produto;
import com.barber.agendamento_bot.api.entity.Venda;
import com.barber.agendamento_bot.api.repository.ProdutoRepository;
import com.barber.agendamento_bot.api.repository.VendaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/estoque")
public class EstoqueController {

    private final ProdutoRepository produtoRepository;
    private final VendaRepository vendaRepository;

    public EstoqueController(ProdutoRepository produtoRepository, VendaRepository vendaRepository) {
        this.produtoRepository = produtoRepository;
        this.vendaRepository = vendaRepository;
    }

    @GetMapping("/produtos")
    public List<Produto> listarProdutos() {
        return produtoRepository.findAll();
    }

    // Apenas cadastra um novo (Gera um novo ID)
    @PostMapping("/produtos")
    public Produto salvarProduto(@RequestBody Produto produto) {
        produto.setNome(produto.getNome().trim());
        return produtoRepository.save(produto);
    }

    // ✨ NOVO: Repor Estoque baseado no ID exato
    @PutMapping("/produtos/{id}/repor")
    public ResponseEntity<?> reporEstoque(@PathVariable Long id, @RequestParam Integer quantidade) {
        Produto produto = produtoRepository.findById(id).orElseThrow();
        produto.setQuantidadeEstoque(produto.getQuantidadeEstoque() + quantidade);
        produtoRepository.save(produto);
        return ResponseEntity.ok(produto);
    }

    // ✨ NOVO: Botão de Lixeira para arrumar cagadas no cadastro
    @DeleteMapping("/produtos/{id}")
    public ResponseEntity<?> excluirProduto(@PathVariable Long id) {
        produtoRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/vender")
    public ResponseEntity<?> registrarVenda(@RequestParam Long produtoId, @RequestParam Integer quantidade, @RequestParam String pagamento) {
        Produto produto = produtoRepository.findById(produtoId).orElseThrow();

        if (produto.getQuantidadeEstoque() < quantidade) {
            return ResponseEntity.badRequest().body("Estoque insuficiente!");
        }

        // Baixa no estoque
        produto.setQuantidadeEstoque(produto.getQuantidadeEstoque() - quantidade);
        produtoRepository.save(produto);

        // ✨ MATEMÁTICA DAS TAXAS (Calcula o valor líquido real)
        BigDecimal precoCheio = produto.getPreco().multiply(new BigDecimal(quantidade));
        BigDecimal valorLiquido = precoCheio;

        if ("CREDITO".equalsIgnoreCase(pagamento)) {
            valorLiquido = precoCheio.multiply(new BigDecimal("0.95")); // Tira 5% de taxa
        } else if ("DEBITO".equalsIgnoreCase(pagamento)) {
            valorLiquido = precoCheio.multiply(new BigDecimal("0.98")); // Tira 2% de taxa
        }

        Venda venda = new Venda();
        venda.setProduto(produto);
        venda.setQuantidade(quantidade);
        venda.setFormaPagamento(pagamento);
        venda.setValorTotal(valorLiquido); // Salva o que realmente caiu na conta!
        venda.setDataHoraVenda(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));

        vendaRepository.save(venda);
        return ResponseEntity.ok(venda);
    }

    @GetMapping("/vendas")
    public List<Venda> listarVendas() {
        return vendaRepository.findAll();
    }
}