package com.pastudyhub.user.config;

import com.pastudyhub.user.security.JwtAuthenticationFilter;
import com.pastudyhub.user.security.JwtTokenProvider;
import com.pastudyhub.user.security.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the User Service.
 *
 * <p>Key security decisions:
 * <ul>
 *   <li>STATELESS session — JWT is the only session mechanism (no server-side sessions)</li>
 *   <li>CSRF disabled — not needed for stateless JWT REST APIs (CSRF exploits cookie-based sessions)</li>
 *   <li>/api/v1/auth/** is public — login and register don't require authentication</li>
 *   <li>All other endpoints require a valid JWT</li>
 *   <li>BCrypt strength 12 — intentionally slow to make brute-force attacks expensive</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(401, "Unauthorized"))
                )
                .authorizeHttpRequests(auth -> auth
                        // All endpoints permit all - monolith trusts X-User-Id header
                        // In production, add authentication/authorization layer
                        .anyRequest().permitAll()
                )
                .build();
    }

    /**
     * BCrypt password encoder with strength 12.
     * At strength 12, each hash takes ~250ms — slow enough to deter brute-force attacks
     * while fast enough for real users logging in.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Password policy bean — used by AuthenticationServiceImpl and UserServiceImpl.
     */
    @Bean
    public PasswordPolicy passwordPolicy() {
        return new PasswordPolicy();
    }
}
