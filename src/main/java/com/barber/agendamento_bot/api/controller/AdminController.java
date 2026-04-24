package com.barber.agendamento_bot.api.controller;

import com.barber.agendamento_bot.api.entity.LogAtividade;
import com.barber.agendamento_bot.api.entity.Usuario;
import com.barber.agendamento_bot.api.repository.LogAtividadeRepository;
import com.barber.agendamento_bot.api.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UsuarioRepository usuarioRepository;
    private final LogAtividadeRepository logRepository;

    public AdminController(UsuarioRepository usuarioRepository, LogAtividadeRepository logRepository) {
        this.usuarioRepository = usuarioRepository;
        this.logRepository = logRepository;
    }

    // Apenas usuários ATIVOS aparecem nos relatórios/menus
    @GetMapping("/funcionarios")
    public ResponseEntity<List<Usuario>> listarFuncionarios() {
        if (!isAdmin()) return ResponseEntity.status(403).build();
        List<Usuario> ativos = usuarioRepository.findAll().stream()
                .filter(u -> u.getAtivo() == null || u.getAtivo())
                .toList();
        return ResponseEntity.ok(ativos);
    }

    @GetMapping("/auditoria")
    public ResponseEntity<List<LogAtividade>> puxarAuditoria() {
        if (!isAdmin()) return ResponseEntity.status(403).build();
        List<LogAtividade> logs = logRepository.findAll(Sort.by(Sort.Direction.DESC, "dataHora"));
        return ResponseEntity.ok(logs);
    }

    // ✨ GERENCIAMENTO DE USUÁRIOS
    @PostMapping("/usuarios")
    public ResponseEntity<Usuario> criarUsuario(@RequestBody Usuario novoUsuario) {
        if (!isAdmin()) return ResponseEntity.status(403).build();
        novoUsuario.setAtivo(true);
        if(novoUsuario.getTaxaCredito() == null) novoUsuario.setTaxaCredito(5.0);
        if(novoUsuario.getTaxaDebito() == null) novoUsuario.setTaxaDebito(2.0);
        return ResponseEntity.ok(usuarioRepository.save(novoUsuario));
    }

    @PutMapping("/usuarios/{id}")
    public ResponseEntity<Usuario> atualizarUsuario(@PathVariable Long id, @RequestBody Usuario dadosAtualizados) {
        if (!isAdmin()) return ResponseEntity.status(403).build();
        return usuarioRepository.findById(id).map(usuario -> {
            usuario.setNome(dadosAtualizados.getNome());
            usuario.setLogin(dadosAtualizados.getLogin());
            if(dadosAtualizados.getSenha() != null && !dadosAtualizados.getSenha().isEmpty()){
                usuario.setSenha(dadosAtualizados.getSenha());
            }
            usuario.setPerfil(dadosAtualizados.getPerfil());
            usuario.setInstanciaWhatsapp(dadosAtualizados.getInstanciaWhatsapp());
            usuario.setTaxaComissao(dadosAtualizados.getTaxaComissao());
            return ResponseEntity.ok(usuarioRepository.save(usuario));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<?> excluirUsuario(@PathVariable Long id) {
        if (!isAdmin()) return ResponseEntity.status(403).build();
        return usuarioRepository.findById(id).map(usuario -> {
            usuario.setAtivo(false); // Lixeira Inteligente (Soft Delete)
            usuarioRepository.save(usuario);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));
    }
}