package com.barber.agendamento_bot.api.controller;

import com.barber.agendamento_bot.api.entity.HorarioFuncionamento;
import com.barber.agendamento_bot.api.entity.Usuario;
import com.barber.agendamento_bot.api.repository.HorarioRepository;
import com.barber.agendamento_bot.api.repository.UsuarioRepository;
import com.barber.agendamento_bot.api.service.AgendaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ConfiguracaoController {

    private final UsuarioRepository usuarioRepository;
    private final HorarioRepository horarioRepository;
    private final AgendaService agendaService;

    public ConfiguracaoController(UsuarioRepository usuarioRepository, HorarioRepository horarioRepository, AgendaService agendaService) {
        this.usuarioRepository = usuarioRepository;
        this.horarioRepository = horarioRepository;
        this.agendaService = agendaService;
    }

    private Usuario getLogado() {
        String login = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByLogin(login).orElse(null);
    }

    // =========================================================
    // 1. CONFIGURAÇÕES DO ROBÔ E TAXAS (AGORA SÃO INDIVIDUAIS)
    // =========================================================
    @GetMapping("/configuracoes/geral")
    public ResponseEntity<Map<String, Object>> getConfigs() {
        Usuario u = getLogado();
        if (u == null) return ResponseEntity.status(401).build();

        Map<String, Object> configs = new HashMap<>();
        configs.put("botAtivo", u.getBotAtivo() != null ? u.getBotAtivo() : false);
        configs.put("minutosConfirmacao", u.getMinutosConfirmacao() != null ? u.getMinutosConfirmacao() : 30);
        configs.put("taxaCredito", u.getTaxaCredito() != null ? u.getTaxaCredito() : 5.0);
        configs.put("taxaDebito", u.getTaxaDebito() != null ? u.getTaxaDebito() : 2.0);

        // Retrocompatibilidade para o JS do financeiro não quebrar
        configs.put("credito", u.getTaxaCredito() != null ? u.getTaxaCredito() : 5.0);
        configs.put("debito", u.getTaxaDebito() != null ? u.getTaxaDebito() : 2.0);

        return ResponseEntity.ok(configs);
    }

    @GetMapping("/promover-mestre/{loginUsuario}")
    public ResponseEntity<String> promoverMestre(@PathVariable String loginUsuario) {
        Usuario u = usuarioRepository.findByLogin(loginUsuario).orElse(null);
        if (u != null) {
            u.setPerfil("SUPER_ADMIN");
            usuarioRepository.save(u);
            return ResponseEntity.ok("Sucesso! A conta '" + loginUsuario + "' agora é a dona de todo o sistema (SUPER_ADMIN). Pode recarregar a tela da barbearia!");
        }
        return ResponseEntity.badRequest().body("Usuário não encontrado. Verifique o login.");
    }

    @PostMapping("/configuracoes/geral")
    public ResponseEntity<?> setConfigs(@RequestBody Map<String, Object> payload) {
        Usuario u = getLogado();
        if (u == null) return ResponseEntity.status(401).build();

        if (payload.containsKey("botAtivo")) u.setBotAtivo((Boolean) payload.get("botAtivo"));
        if (payload.containsKey("minutosConfirmacao")) u.setMinutosConfirmacao(Integer.parseInt(payload.get("minutosConfirmacao").toString()));

        if (payload.containsKey("taxaCredito")) u.setTaxaCredito(Double.parseDouble(payload.get("taxaCredito").toString()));
        else if (payload.containsKey("credito")) u.setTaxaCredito(Double.parseDouble(payload.get("credito").toString()));

        if (payload.containsKey("taxaDebito")) u.setTaxaDebito(Double.parseDouble(payload.get("taxaDebito").toString()));
        else if (payload.containsKey("debito")) u.setTaxaDebito(Double.parseDouble(payload.get("debito").toString()));

        usuarioRepository.save(u);
        return ResponseEntity.ok().build();
    }

    // =========================================================
    // 2. HORÁRIOS DE FUNCIONAMENTO (INDIVIDUAIS POR BARBEIRO)
    // =========================================================
    @GetMapping("/horarios")
    public ResponseEntity<List<HorarioFuncionamento>> getHorarios(@RequestParam(required = false) Long barbeiroId) {
        Usuario logado = getLogado();
        if (logado == null) return ResponseEntity.status(401).build();

        // ✨ Se o Admin passar um ID, buscamos o horário daquele barbeiro específico.
        // Se não passar nada, buscamos o do próprio logado.
        Usuario alvo = logado;
        if (barbeiroId != null && (logado.getPerfil().contains("ADMIN") || logado.getPerfil().equals("SUPER_ADMIN"))) {
            alvo = usuarioRepository.findById(barbeiroId).orElse(logado);
        }

        List<HorarioFuncionamento> horarios = horarioRepository.findAllByDonoDoRegistroOrderByDiaDaSemanaAsc(alvo);

        if (horarios.isEmpty()) {
            String[] dias = {"", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"};
            for (int i = 1; i <= 7; i++) {
                HorarioFuncionamento h = new HorarioFuncionamento();
                h.setDiaDaSemana(i);
                h.setNomeDia(dias[i]);
                h.setHoraAbertura("09:00");
                h.setHoraFechamento("19:00");
                h.setFechado(i == 7);
                h.setDonoDoRegistro(alvo);
                horarioRepository.save(h);
                horarios.add(h);
            }
        }
        return ResponseEntity.ok(horarios);
    }

    @PutMapping("/horarios")
    public ResponseEntity<?> salvarHorarios(@RequestParam(required = false) Long barbeiroId, @RequestBody List<HorarioFuncionamento> alterados) {
        Usuario logado = getLogado();
        if (logado == null) return ResponseEntity.status(401).build();

        Usuario alvo = logado;
        if (barbeiroId != null && (logado.getPerfil().contains("ADMIN") || logado.getPerfil().equals("SUPER_ADMIN"))) {
            alvo = usuarioRepository.findById(barbeiroId).orElse(logado);
        }

        for (HorarioFuncionamento h : alterados) {
            HorarioFuncionamento db = horarioRepository.findById(h.getId()).orElse(null);
            // ✨ Segurança: Só permite salvar se o registro pertencer ao alvo correto
            if (db != null && db.getDonoDoRegistro().getId().equals(alvo.getId())) {
                db.setHoraAbertura(h.getHoraAbertura());
                db.setHoraFechamento(h.getHoraFechamento());
                db.setFechado(h.isFechado());
                horarioRepository.save(db);
            }
        }
        return ResponseEntity.ok().build();
    }

    // =========================================================
    // 3. EXCLUSÃO DE BLOQUEIOS DA AGENDA
    // =========================================================
    @DeleteMapping("/agendamentos/bloqueios/{id}")
    public ResponseEntity<?> removerBloqueio(@PathVariable Long id) {
        agendaService.removerBloqueio(id);
        return ResponseEntity.ok().build();
    }
}