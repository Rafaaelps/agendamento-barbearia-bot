package com.barber.agendamento_bot.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // ⚠️ Libera login, webhook do WhatsApp e a rota de checagem de perfil
                        .requestMatchers("/login.html", "/fazer-login", "/api/webhook/**", "/webhook/**", "/api/usuario/me").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login.html")
                        .loginProcessingUrl("/fazer-login")
                        .defaultSuccessUrl("/index.html", true)
                        .failureUrl("/login.html?erro=true")
                )
                .rememberMe(remember -> remember
                        .key("chave-super-secreta-barbearia-2026")
                        .rememberMeParameter("lembrar_de_mim")
                        .tokenValiditySeconds(2592000) // 30 dias logado
                )
                .logout(logout -> logout
                        .logoutUrl("/sair")
                        .logoutSuccessUrl("/login.html?saiu=true")
                );

        return http.build();
    }
}