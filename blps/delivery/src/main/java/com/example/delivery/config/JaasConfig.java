package com.example.delivery.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
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
        provider.setLoginConfig(materializeLoginConfig());
        provider.setAuthorityGranters(new JaasAuthorityGranter[] { new JaasAuthorityGranter() });

        provider.setCallbackHandlers(new JaasAuthenticationCallbackHandler[] {
            new JaasNameCallbackHandler(),
            new JaasPasswordCallbackHandler()
        });

        return provider;
    }

    private Resource materializeLoginConfig() throws IOException {
        ClassPathResource classPathConfig = new ClassPathResource("jaas.config");
        Path temporaryConfig = Files.createTempFile("delivery-jaas-", ".config");
        try (InputStream input = classPathConfig.getInputStream()) {
            Files.copy(input, temporaryConfig, StandardCopyOption.REPLACE_EXISTING);
        }
        temporaryConfig.toFile().deleteOnExit();
        return new FileSystemResource(temporaryConfig);
    }
}
