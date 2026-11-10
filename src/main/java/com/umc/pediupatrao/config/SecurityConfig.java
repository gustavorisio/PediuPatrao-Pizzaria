package com.umc.pediupatrao.config;

import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.servlet.DispatcherType;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UsuarioDetailsService usuarioDetailsService;

    public SecurityConfig(UsuarioDetailsService usuarioDetailsService) {
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(usuarioDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // O CSRF fica ativo: formulários e chamadas da API usam a mesma sessão de login.
        http
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/login", "/css/**", "/dist/**", "/plugins/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/api/dashboard/**", "/dashboard").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers("/api/auditorias/**", "/auditorias", "/funcionarios", "/entregadores/**", "/configuracoes").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers("/usuarios/**", "/admin").hasRole("ADMIN")
                        // A API de usuários também permite editar a própria conta; o método valida o ID.
                        .requestMatchers("/api/usuarios/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/clientes/**", "/api/produtos/**", "/api/entregadores/**", "/api/pedidos/**")
                            .hasAnyRole("ADMIN", "GERENTE", "ATENDENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/pedidos/*/cancelar", "/api/pedidos/*/desconto").hasRole("GERENTE")
                        .requestMatchers(HttpMethod.DELETE, "/api/pedidos/**").hasRole("GERENTE")
                        .requestMatchers("/api/pedidos/**").hasAnyRole("GERENTE", "ATENDENTE")
                        .requestMatchers(HttpMethod.DELETE, "/api/clientes/**").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers("/api/clientes/**").hasAnyRole("ADMIN", "GERENTE", "ATENDENTE")
                        .requestMatchers("/api/produtos/**", "/api/entregadores/**", "/produtos/salvar", "/produtos/editar/**", "/produtos/deletar/**")
                            .hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers("/", "/pedidos", "/clientes/**", "/produtos", "/logout").hasAnyRole("ADMIN", "GERENTE", "ATENDENTE")
                        .anyRequest().denyAll()
                )
                .exceptionHandling(errors -> errors
                        .defaultAuthenticationEntryPointFor((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"message\":\"Sessão expirada. Entre novamente.\"}");
                        }, new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/api/**"))
                        .accessDeniedHandler((request, response, exception) -> {
                            if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
                                response.setStatus(403);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"message\":\"Acesso negado para este perfil.\"}");
                            } else {
                                response.sendError(403);
                            }
                        }))
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                        .defaultSuccessUrl("/", true)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

}
