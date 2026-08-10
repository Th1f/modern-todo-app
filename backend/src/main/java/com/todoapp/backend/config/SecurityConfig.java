package com.todoapp.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Development configuration: every endpoint is open.
 *
 * spring-boot-starter-security locks down the whole application by default, so
 * without this bean every request returns 401. When you add real authentication,
 * this is the file to change.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF tokens protect browser form posts; a token API called by
                // fetch() does not use them, and leaving it on blocks POST/PUT/DELETE.
                .csrf(csrf -> csrf.disable())
                // Makes the filter chain honour the rules in CorsConfig.
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // The H2 console renders in a frame, which is denied by default.
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .build();
    }
}
