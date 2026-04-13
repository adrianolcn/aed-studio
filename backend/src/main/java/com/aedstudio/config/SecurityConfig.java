package com.aedstudio.config;

import com.aedstudio.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuração de segurança com DUAS cadeias de filtros independentes.
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  Cadeia 1 — /api/**  (JWT stateless)                            │
 * │  • Sem sessão (STATELESS)                                       │
 * │  • CSRF desabilitado (tokens JWT são CSRF-safe por natureza)    │
 * │  • JwtAuthFilter valida Bearer token em cada requisição         │
 * │  • Rotas públicas: POST /api/auth/register, /api/auth/login     │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  Cadeia 2 — /** (sessão web)                                    │
 * │  • Sessão armazenada no PostgreSQL (Spring Session JDBC)        │
 * │  • CSRF habilitado (cookie SameSite=Strict)                     │
 * │  • Form login redireciona para /login.html                      │
 * │  • Rotas públicas: /login.html, /assets/**, /*.html             │
 * └─────────────────────────────────────────────────────────────────┘
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter           jwtAuthFilter;
    private final UserDetailsServiceImpl  userDetailsService;

    @Value("${cors.allowed-origins}")
    private String allowedOriginsRaw;

    // ── Cadeia 1: API REST (JWT) ─────────────────────────────────────

    /**
     * @Order(1) — tem precedência sobre a cadeia de sessão.
     * Só intercepta requisições que chegam em /api/**
     */
    @Bean
    @org.springframework.core.annotation.Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")              // só rotas de API
            .csrf(AbstractHttpConfigurer::disable)   // JWT é CSRF-safe
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Rotas públicas da API
                .requestMatchers(HttpMethod.POST,
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/refresh").permitAll()
                // Todo o resto da API exige autenticação
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ── Cadeia 2: Web (sessão) ───────────────────────────────────────

    @Bean
    @org.springframework.core.annotation.Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(Customizer.withDefaults())          // CSRF ativo para web
            .sessionManagement(sm -> sm
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(3)                   // máx 3 sessões por usuário
            )
            .authorizeHttpRequests(auth -> auth
                // Recursos públicos (tela de login, assets estáticos)
                .requestMatchers(
                    "/login.html",
                    "/register.html",
                    "/css/**",
                    "/js/**",
                    "/assets/**",
                    "/favicon.ico").permitAll()
                // Todo o resto exige login via sessão
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/auth/login-web")
                .defaultSuccessUrl("/aed-studio.html", true)
                .failureUrl("/login.html?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout-web")
                .logoutSuccessUrl("/login.html?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .securityContext(sc -> sc
                .securityContextRepository(securityContextRepository()));

        return http.build();
    }

    // ── Beans de segurança ───────────────────────────────────────────

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // custo 12 = bom equilíbrio segurança/performance
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    // ── CORS ─────────────────────────────────────────────────────────

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .toList();
        config.setAllowedOrigins(origins);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-CSRF-TOKEN"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);   // necessário para cookies de sessão
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
