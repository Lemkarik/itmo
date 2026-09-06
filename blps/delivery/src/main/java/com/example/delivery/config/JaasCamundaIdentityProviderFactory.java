package com.example.delivery.config;

import org.camunda.bpm.engine.impl.interceptor.Session;
import org.camunda.bpm.engine.impl.interceptor.SessionFactory;
import org.springframework.security.authentication.AuthenticationManager;

public class JaasCamundaIdentityProviderFactory implements SessionFactory {

    private final AuthenticationManager authenticationManager;

    public JaasCamundaIdentityProviderFactory(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public Class<?> getSessionType() {
        return JaasCamundaIdentityProvider.class;
    }

    @Override
    public Session openSession() {
        return new JaasCamundaIdentityProvider(authenticationManager);
    }
}
