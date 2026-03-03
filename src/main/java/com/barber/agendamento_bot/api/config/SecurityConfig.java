package com.barber.agendamento_bot.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Libera nosso JavaScript para salvar dados
                .authorizeHttpRequests(auth -> auth
                        // ⚠️ Libera a tela de login e o Webhook do WhatsApp (para o robô continuar respondendo)
                        .requestMatchers("/login.html", "/api/webhook/**", "/webhook/**").permitAll()
                        // Tranca o resto do sistema (index, financeiro, servicos)
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login.html") // Aponta para a nossa tela visual
                        .loginProcessingUrl("/fazer-login")
                        .defaultSuccessUrl("/index.html", true)
                        .failureUrl("/login.html?erro=true")
                )
                .rememberMe(remember -> remember
                        .key("chave-super-secreta-barbearia-2026") // Chave para criptografar o login
                        .rememberMeParameter("lembrar_de_mim") // O nome exato da checkbox no HTML
                        .tokenValiditySeconds(2592000) // ✨ Mantém logado por 30 DIAS (em segundos)
                )
                .logout(logout -> logout
                        .logoutUrl("/sair")
                        .logoutSuccessUrl("/login.html?saiu=true")
                );

        return http.build();
    }

    // ✨ CADASTRE AQUI O USUÁRIO E SENHA DO BARBEIRO
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
                .username("barbearia")
                .password("{noop}admin123") // {noop} significa senha em texto puro (só pra facilitar agora)
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }
}