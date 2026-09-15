package com.example.delivery.camunda.identity;

import java.util.logging.Logger;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;

public class DeliveryJaasIdentityProviderPlugin extends AbstractProcessEnginePlugin {

    private static final Logger LOGGER = Logger.getLogger(DeliveryJaasIdentityProviderPlugin.class.getName());

    private XmlUserStore userStore;

    @Override
    public void preInit(ProcessEngineConfigurationImpl configuration) {
        userStore = new XmlUserStore();
        configuration.setIdentityProviderSessionFactory(new JaasIdentityProviderFactory(userStore));
        LOGGER.info(() -> "Delivery JAAS identity provider initialized with "
                + userStore.users().size() + " users from " + userStore.source());
    }

    @Override
    public void postProcessEngineBuild(ProcessEngine processEngine) {
        new CamundaIdentitySynchronizer(processEngine, userStore).synchronize();
    }
}
