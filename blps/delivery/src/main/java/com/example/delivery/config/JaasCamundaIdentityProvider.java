package com.example.delivery.config;

import org.camunda.bpm.engine.impl.identity.db.DbIdentityServiceProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;

public class JaasCamundaIdentityProvider extends DbIdentityServiceProvider {

    private final AuthenticationManager authenticationManager;

    public JaasCamundaIdentityProvider(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public boolean checkPassword(String userId, String password) {
        try {
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userId, password)).isAuthenticated();
        } catch (AuthenticationException exception) {
            return false;
        }
    }
}
