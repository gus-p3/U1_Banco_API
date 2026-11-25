package com.intrumentoev.demo.config;

import com.intrumentoev.demo.service.service.auth.BearerTokenValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Seguridad stateless con JWT y roles (CLIENT / ADMIN).
 * <ul>
 *   <li>Públicos: salud, Swagger, login, refresh, onboarding y catálogos de lectura.</li>
 *   <li>CLIENT: solo su propia información (/v1/clientes/me, sus cuentas y pagos).</li>
 *   <li>ADMIN: consultas globales de clientes, baja/actualización de clientes y administración.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    static final String ADMIN = "ADMIN";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            BearerTokenValidator tokenValidator,
                                            RestAuthenticationEntryPoint entryPoint,
                                            RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Salud y documentación
                        .requestMatchers(HttpMethod.GET, "/", "/health").permitAll()
                        .requestMatchers("/error", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/favicon.ico").permitAll()
                        // Autenticación
                        .requestMatchers(HttpMethod.POST, "/v1/auth/login", "/v1/auth/login-biometrico", "/v1/auth/refresh").permitAll()
                        // Onboarding público y catálogos necesarios para el formulario
                        .requestMatchers(HttpMethod.POST, "/v1/clientes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/catalogos/**").permitAll()
                        // Solo ADMIN
                        .requestMatchers(HttpMethod.POST, "/v1/catalogos/sincronizar").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.POST, "/v1/clientes/buscar").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/v1/clientes").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/v1/clientes").hasRole(ADMIN)
                        .requestMatchers("/personas", "/personasActualiza", "/personasElimina").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/v1/productos/**").authenticated()
                        .requestMatchers("/v1/productos/**").hasRole(ADMIN)
                        // Cualquier usuario autenticado (la propiedad del recurso se valida en el controlador)
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(tokenValidator), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
