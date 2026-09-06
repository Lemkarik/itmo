package com.example.delivery.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;

import jakarta.annotation.PostConstruct;

@Configuration
public class AuthenticationConfig {

    private final XmlUserDetailsService xmlUserDetailsService;
    private final JaasAuthProvider jaasAuthProvider;

    public AuthenticationConfig(XmlUserDetailsService xmlUserDetailsService,
                                JaasAuthProvider jaasAuthProvider) {
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
}
