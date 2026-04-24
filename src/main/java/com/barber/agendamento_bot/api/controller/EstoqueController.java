package com.barber.agendamento_bot.api.controller;

import com.barber.agendamento_bot.api.entity.Produto;
import com.barber.agendamento_bot.api.entity.Usuario;
import com.barber.agendamento_bot.api.entity.Venda;
import com.barber.agendamento_bot.api.repository.ConfiguracaoRepository;
import com.barber.agendamento_bot.api.repository.ProdutoRepository;
import com.barber.agendamento_bot.api.repository.UsuarioRepository;
import com.barber.agendamento_bot.api.repository.VendaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final ConfiguracaoRepository configuracaoRepository;
    private final UsuarioRepository usuarioRepository; // ✨ NOVO

    public EstoqueController(ProdutoRepository produtoRepository, VendaRepository vendaRepository, ConfiguracaoRepository configuracaoRepository, UsuarioRepository usuarioRepository) {
        this.produtoRepository = produtoRepository;
        this.vendaRepository = vendaRepository;
        this.configuracaoRepository = configuracaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // ✨ DESCOBRE QUEM ESTÁ LOGADO
    private Usuario getUsuarioLogado() {
        String login = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByLogin(login).orElse(null);
    }

    @GetMapping("/produtos")
    public List<Produto> listarProdutos() {
        Usuario logado = getUsuarioLogado();
        if (logado == null) return List.of();

        // Se for ADMIN, vê todos os produtos ativos do salão
        if (logado.getPerfil().equals("ADMIN") || logado.getPerfil().equals("ROLE_ADMIN")) {
            return produtoRepository.findAll().stream()
                    .filter(p -> p.getAtivo() == null || p.getAtivo())
                    .collect(Collectors.toList());
        }

        // ✨ Se for Barbeiro (João), vê SÓ os produtos ativos DELE
        return produtoRepository.findByAtivoTrueAndDonoDoRegistro(logado);
    }

    @PostMapping("/produtos")
    public Produto salvarProduto(@RequestBody Produto produto) {
        Usuario logado = getUsuarioLogado();
        if (logado != null) produto.setDonoDoRegistro(logado); // ✨ Carimba o dono no produto

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

        Usuario dono = produto.getDonoDoRegistro();
        double taxaCredito = (dono != null && dono.getTaxaCredito() != null) ? dono.getTaxaCredito() : 5.0;
        double taxaDebito = (dono != null && dono.getTaxaDebito() != null) ? dono.getTaxaDebito() : 2.0;

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
        Usuario logado = getUsuarioLogado();
        if (logado == null) return List.of();

        // Admin vê todas as vendas
        if (logado.getPerfil().equals("ADMIN") || logado.getPerfil().equals("ROLE_ADMIN")) {
            return vendaRepository.findAll();
        }

        // ✨ Barbeiro vê só as vendas dos produtos DELE
        return vendaRepository.findAll().stream()
                .filter(v -> v.getProduto() != null && v.getProduto().getDonoDoRegistro() != null && v.getProduto().getDonoDoRegistro().getId().equals(logado.getId()))
                .collect(Collectors.toList());
    }
}