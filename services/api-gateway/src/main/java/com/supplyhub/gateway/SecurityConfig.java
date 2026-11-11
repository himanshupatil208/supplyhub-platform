
package com.supplyhub.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/health").permitAll()

                        // Public product and inventory browsing
                        .pathMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/catalog/products",
                                "/api/catalog/products/**",
                                "/api/inventory/stock"
                        ).permitAll()

                        // All other API requests require authentication
                        .pathMatchers("/api/**").hasRole("ADMIN")

                        .anyExchange().denyAll()
                )
                .httpBasic(Customizer.withDefaults())
                .build();
    }


    @Bean
    public MapReactiveUserDetailsService userDetailsService(
            @org.springframework.beans.factory.annotation.Value(
                    "${SUPPLYHUB_ADMIN_PASSWORD}") String password) {

        UserDetails admin = User.withUsername("admin")
                .passwordEncoder(
                        PasswordEncoderFactories
                                .createDelegatingPasswordEncoder()::encode)
                .password(password)
                .roles("ADMIN")
                .build();

        return new MapReactiveUserDetailsService(admin);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return new CorsConfigurationSource() {
            @Override
            public CorsConfiguration getCorsConfiguration(ServerWebExchange exchange) {
                CorsConfiguration config = new CorsConfiguration();

                config.setAllowedOrigins(List.of(
                        "http://localhost:5173",
                        "http://localhost:5174",
                        "http://localhost:8088",
                        "https://supplyhub.duckdns.org"
                ));

                config.setAllowedMethods(List.of(
                        "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
                ));

                config.setAllowedHeaders(List.of(
                        "Authorization",
                        "Content-Type"
                ));

                config.setAllowCredentials(true);
                config.setMaxAge(3600L);

                return config;
            }
        };
    }

}

