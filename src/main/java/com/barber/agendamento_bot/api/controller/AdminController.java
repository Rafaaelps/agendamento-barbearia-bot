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

    // ✨ NOVA FUNÇÃO: Busca o usuário real do banco para ver se ele é SUPER ou ADMIN normal
    private Usuario getLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return usuarioRepository.findByLogin(auth.getName()).orElse(null);
    }

    @GetMapping("/funcionarios")
    public ResponseEntity<List<Usuario>> listarFuncionarios() {
        Usuario logado = getLogado();
        if (logado == null || "BARBEIRO".equals(logado.getPerfil())) return ResponseEntity.status(403).build();

        List<Usuario> ativos = usuarioRepository.findAll().stream()
                .filter(u -> u.getAtivo() == null || u.getAtivo())
                .filter(u -> {
                    // ✨ MÁGICA 1: O SUPER_ADMIN vê todo mundo
                    if ("SUPER_ADMIN".equals(logado.getPerfil())) return true;

                    // ✨ MÁGICA 2: O ADMIN normal só vê ele mesmo e os Barbeiros
                    return u.getId().equals(logado.getId()) || "BARBEIRO".equals(u.getPerfil());
                })
                .toList();
        return ResponseEntity.ok(ativos);
    }

    @GetMapping("/auditoria")
    public ResponseEntity<List<LogAtividade>> puxarAuditoria() {
        Usuario logado = getLogado();
        if (logado == null || "BARBEIRO".equals(logado.getPerfil())) return ResponseEntity.status(403).build();

        List<LogAtividade> logs = logRepository.findAll(Sort.by(Sort.Direction.DESC, "dataHora"));
        return ResponseEntity.ok(logs);
    }

    @PostMapping("/usuarios")
    public ResponseEntity<?> criarUsuario(@RequestBody Usuario novoUsuario) {
        Usuario logado = getLogado();
        if (logado == null || "BARBEIRO".equals(logado.getPerfil())) return ResponseEntity.status(403).build();

        // ✨ TRAVA: Um ADMIN normal não pode criar uma conta SUPER_ADMIN
        if ("SUPER_ADMIN".equals(novoUsuario.getPerfil()) && !"SUPER_ADMIN".equals(logado.getPerfil())) {
            return ResponseEntity.status(403).body("Apenas um desenvolvedor pode criar uma conta mestre.");
        }

        novoUsuario.setAtivo(true);
        if(novoUsuario.getTaxaCredito() == null) novoUsuario.setTaxaCredito(5.0);
        if(novoUsuario.getTaxaDebito() == null) novoUsuario.setTaxaDebito(2.0);
        if(novoUsuario.getTaxaComissaoProduto() == null) novoUsuario.setTaxaComissaoProduto(0.0);

        return ResponseEntity.ok(usuarioRepository.save(novoUsuario));
    }

    @PutMapping("/usuarios/{id}")
    public ResponseEntity<?> atualizarUsuario(@PathVariable Long id, @RequestBody Usuario dadosAtualizados) {
        Usuario logado = getLogado();
        if (logado == null || "BARBEIRO".equals(logado.getPerfil())) return ResponseEntity.status(403).build();

        // ✨ TRAVA: Impede promover alguém para Super Admin indevidamente
        if ("SUPER_ADMIN".equals(dadosAtualizados.getPerfil()) && !"SUPER_ADMIN".equals(logado.getPerfil())) {
            return ResponseEntity.status(403).body("Acesso Negado.");
        }

        return usuarioRepository.findById(id).map(usuario -> {
            usuario.setNome(dadosAtualizados.getNome());
            usuario.setLogin(dadosAtualizados.getLogin());
            if(dadosAtualizados.getSenha() != null && !dadosAtualizados.getSenha().isEmpty()){
                usuario.setSenha(dadosAtualizados.getSenha());
            }

            // ✨ TRAVA: Impede alterar a conta de um Super Admin se você não for um
            if ("SUPER_ADMIN".equals(usuario.getPerfil()) && !"SUPER_ADMIN".equals(logado.getPerfil())) {
                // Fica em silêncio e ignora a alteração do perfil
            } else {
                usuario.setPerfil(dadosAtualizados.getPerfil());
            }

            usuario.setInstanciaWhatsapp(dadosAtualizados.getInstanciaWhatsapp());
            usuario.setTaxaComissao(dadosAtualizados.getTaxaComissao());
            usuario.setTaxaComissaoProduto(dadosAtualizados.getTaxaComissaoProduto());

            return ResponseEntity.ok(usuarioRepository.save(usuario));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<?> excluirUsuario(@PathVariable Long id) {
        Usuario logado = getLogado();
        if (logado == null || "BARBEIRO".equals(logado.getPerfil())) return ResponseEntity.status(403).build();

        return usuarioRepository.findById(id).map(usuario -> {
            // ✨ TRAVA: Ninguém deleta o Desenvolvedor
            if ("SUPER_ADMIN".equals(usuario.getPerfil()) && !"SUPER_ADMIN".equals(logado.getPerfil())) {
                return ResponseEntity.status(403).build();
            }

            usuario.setAtivo(false);
            usuarioRepository.save(usuario);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}