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
import java.util.Optional;

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

    // ✨ MUDANÇA: Inteligência que soma a quantidade se o produto já existir
    @PostMapping("/produtos")
    public ResponseEntity<Produto> salvarProduto(@RequestBody Produto novoProduto) {
        // Remove espaços extras no início e no fim do nome digitado
        String nomeLimpo = novoProduto.getNome().trim();

        // Verifica se já existe um produto com esse nome no banco (ignorando maiúsculas)
        Optional<Produto> existente = produtoRepository.findByNomeIgnoreCase(nomeLimpo);

        if (existente.isPresent()) {
            // Se existe, apenas SOMA o estoque novo com o antigo e atualiza o preço
            Produto p = existente.get();
            p.setQuantidadeEstoque(p.getQuantidadeEstoque() + novoProduto.getQuantidadeEstoque());
            p.setPreco(novoProduto.getPreco());

            return ResponseEntity.ok(produtoRepository.save(p));
        } else {
            // Se não existe, cria um novo normalmente
            novoProduto.setNome(nomeLimpo);
            return ResponseEntity.ok(produtoRepository.save(novoProduto));
        }
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

        // Registra o dinheiro da venda
        Venda venda = new Venda();
        venda.setProduto(produto);
        venda.setQuantidade(quantidade);
        venda.setFormaPagamento(pagamento);
        venda.setValorTotal(produto.getPreco().multiply(new BigDecimal(quantidade)));
        venda.setDataHoraVenda(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));

        vendaRepository.save(venda);
        return ResponseEntity.ok(venda);
    }

    @GetMapping("/vendas")
    public List<Venda> listarVendas() {
        return vendaRepository.findAll();
    }
}