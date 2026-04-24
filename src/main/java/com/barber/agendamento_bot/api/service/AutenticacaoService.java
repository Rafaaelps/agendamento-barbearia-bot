package com.barber.agendamento_bot.api.service;

import com.barber.agendamento_bot.api.entity.Usuario;
import com.barber.agendamento_bot.api.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AutenticacaoService implements UserDetailsService {

    private final UsuarioRepository repository;

    public AutenticacaoService(UsuarioRepository repository) {
        this.repository = repository;

        // ✨ GERA OS USUÁRIOS DE TESTE SE O BANCO ESTIVER VAZIO
        if(repository.count() == 0) {
            Usuario dono = new Usuario();
            dono.setNome("Sócio Principal");
            dono.setLogin("dono");
            dono.setSenha("123");
            dono.setPerfil("ADMIN");
            dono.setTaxaComissao(0.0); // O dono fica com 100%

            Usuario funcionario = new Usuario();
            funcionario.setNome("Barbeiro João");
            funcionario.setLogin("joao");
            funcionario.setSenha("123");
            funcionario.setPerfil("BARBEIRO");
            funcionario.setTaxaComissao(40.0); // O Sócio fica com 40% dos cortes do João

            repository.save(dono);
            repository.save(funcionario);
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = repository.findByLogin(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));

        return User.builder()
                .username(usuario.getLogin())
                .password("{noop}" + usuario.getSenha()) // {noop} indica que a senha não está criptografada (apenas para este ERP interno)
                .roles(usuario.getPerfil())
                .build();
    }
}