package com.erp.inventory.infrastructure.config;

import com.erp.inventory.infrastructure.security.ForbiddenAccessDeniedHandler;
import com.erp.inventory.infrastructure.security.JwtAuthenticationFilter;
import com.erp.inventory.infrastructure.security.UnauthorizedEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Perimeter security configuration for the inventory service.
 *
 * <p>Business endpoints are NOT public: they require a verified access token
 * from which the tenant context is derived. Previously, the wildcard
 * {@code /api/v1/**} was configured with permitAll, allowing unauthenticated requests
 * to register movements, reserve stock across tenants, and query balances.
 *
 * <p>CORS negotiation is delegated to the Kong API Gateway to prevent duplicate
 * Access-Control-Allow-* headers from reaching web clients.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   UnauthorizedEntryPoint unauthorizedEntryPoint,
                                                   ForbiddenAccessDeniedHandler forbiddenAccessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(unauthorizedEntryPoint)
                        .accessDeniedHandler(forbiddenAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health",
                                "/actuator/health/**"
                        ).permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
