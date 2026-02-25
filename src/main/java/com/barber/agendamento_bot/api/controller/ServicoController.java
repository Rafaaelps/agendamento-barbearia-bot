package com.barber.agendamento_bot.api.controller;


import com.barber.agendamento_bot.api.entity.Servico;
import com.barber.agendamento_bot.api.repository.ServicoRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController // 1. Avisa ao Spring: "Esta classe é um Garçom (API REST)!"
@RequestMapping("/api/servicos") // 2. Define o endereço (URL) para chamar este garçom
public class ServicoController {

    private final ServicoRepository servicoRepository;

    // Construtor para injetar a conexão com o banco
    public ServicoController(ServicoRepository servicoRepository) {
        this.servicoRepository = servicoRepository;
    }

    // 3. Quando alguém acessar essa URL pelo navegador, este métod é acionado!
    @GetMapping
    public List<Servico> listarTodosOsServicos() {
        // Vai no banco de dados, pega todos os serviços e devolve para a internet
        return servicoRepository.findAll();
    }
}
