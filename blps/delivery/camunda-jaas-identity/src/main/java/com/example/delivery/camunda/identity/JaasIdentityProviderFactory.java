package com.example.delivery.camunda.identity;

import org.camunda.bpm.engine.impl.interceptor.Session;
import org.camunda.bpm.engine.impl.interceptor.SessionFactory;

final class JaasIdentityProviderFactory implements SessionFactory {

    private final XmlUserStore userStore;

    JaasIdentityProviderFactory(XmlUserStore userStore) {
        this.userStore = userStore;
    }

    @Override
    public Class<?> getSessionType() {
        return JaasDbIdentityServiceProvider.class;
    }

    @Override
    public Session openSession() {
        return new JaasDbIdentityServiceProvider(userStore);
    }
}
