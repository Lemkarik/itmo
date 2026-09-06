package com.example.delivery.config;

import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Component;

@Component
public class CamundaJaasIdentityProviderPlugin extends AbstractProcessEnginePlugin {

    private final AuthenticationManager authenticationManager;

    public CamundaJaasIdentityProviderPlugin(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        processEngineConfiguration.setIdentityProviderSessionFactory(
                new JaasCamundaIdentityProviderFactory(authenticationManager));
    }
}
