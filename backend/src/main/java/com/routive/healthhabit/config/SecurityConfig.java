package com.routive.healthhabit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationErrorHandler errors) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/v1/health",
                                "/verify-email",
                                "/reset-password",
                                "/auth-pages/**",
                                "/actuator/health/**",
                                "/api/v1/drugs/**",
                                "/api/v1/auth/signup",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/social",
                                "/api/v1/auth/email-verifications/**",
                                "/api/v1/auth/password-resets/**"
                        ).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errorsConfig -> errorsConfig.authenticationEntryPoint(errors))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> { }).authenticationEntryPoint(errors))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
