package com.example.delivery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.jaas.JaasAuthenticationCallbackHandler;
import org.springframework.security.authentication.jaas.JaasAuthenticationProvider;
import org.springframework.security.authentication.jaas.JaasNameCallbackHandler;
import org.springframework.security.authentication.jaas.JaasPasswordCallbackHandler;

@Configuration
public class JaasConfig {

    @Bean
    public JaasAuthenticationProvider springJaasAuthenticationProvider() throws Exception {
        JaasAuthenticationProvider provider = new JaasAuthenticationProvider();
        provider.setLoginContextName("DeliveryLogin");
        provider.setLoginConfig(new ClassPathResource("jaas.config"));
        provider.setAuthorityGranters(new JaasAuthorityGranter[] { new JaasAuthorityGranter() });

        provider.setCallbackHandlers(new JaasAuthenticationCallbackHandler[] {
            new JaasNameCallbackHandler(),
            new JaasPasswordCallbackHandler()
        });

        provider.afterPropertiesSet();
        return provider;
    }
}
