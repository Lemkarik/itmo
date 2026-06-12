package com.example.delivery.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.annotation.PostConstruct;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final XmlUserDetailsService xmlUserDetailsService;
    private final JaasAuthProvider jaasAuthProvider;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          XmlUserDetailsService xmlUserDetailsService,
                          JaasAuthProvider jaasAuthProvider) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.xmlUserDetailsService = xmlUserDetailsService;
        this.jaasAuthProvider = jaasAuthProvider;
    }

    @PostConstruct
    public void init() {
        XmlUserDetailsServiceHolder.setService(xmlUserDetailsService);
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(List.of(jaasAuthProvider));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/v3/api-docs.yaml").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
