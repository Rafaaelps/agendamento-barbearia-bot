package com.barber.agendamento_bot.api.controller;

import com.barber.agendamento_bot.api.entity.Servico;
import com.barber.agendamento_bot.api.repository.ServicoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // 1. Avisa ao Spring: "Esta classe é um Garçom (API REST)!"
@RequestMapping("/api/servicos") // 2. Define o endereço (URL) para chamar este garçom
public class ServicoController {

    private final ServicoRepository servicoRepository;

    // Construtor para injetar a conexão com o banco
    public ServicoController(ServicoRepository servicoRepository) {
        this.servicoRepository = servicoRepository;
    }

    // 3. Quando alguém acessar essa URL pelo navegador, este método é acionado!
    @GetMapping
    public List<Servico> listarTodosOsServicos() {
        // Vai no banco de dados, pega todos os serviços e devolve para a internet
        return servicoRepository.findAll();
    }

    // ✨ NOVO: Garçom que recebe o pedido da tela para CRIAR um novo serviço
    @PostMapping
    public Servico criar(@RequestBody Servico servico) {
        return servicoRepository.save(servico);
    }

    // ✨ NOVO: Garçom que recebe o pedido da tela para ATUALIZAR um serviço (Preço, Nome, Duração)
    @PutMapping("/{id}")
    public ResponseEntity<Servico> atualizar(@PathVariable Long id, @RequestBody Servico dadosAtualizados) {
        return servicoRepository.findById(id).map(servico -> {
            servico.setNome(dadosAtualizados.getNome());
            servico.setPreco(dadosAtualizados.getPreco());
            servico.setDuracaoMinutos(dadosAtualizados.getDuracaoMinutos());
            return ResponseEntity.ok(servicoRepository.save(servico));
        }).orElse(ResponseEntity.notFound().build());
    }
}