package com.barber.agendamento_bot.api.repository;

import com.barber.agendamento_bot.api.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Busca o usuário pelo Login (Usado na tela da Web)
    Optional<Usuario> findByLogin(String login);

    // ✨ NOVA LINHA: Busca o usuário pela Instância da Evolution API (Usado pelo Robô do WhatsApp)
    Optional<Usuario> findByInstanciaWhatsapp(String instanciaWhatsapp);
}