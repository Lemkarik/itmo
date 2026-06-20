package com.example.delivery.jca;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class JcaConfig {

    
    @Bean
    public LogisticsResourceAdapter logisticsResourceAdapter() {
        return new LogisticsResourceAdapter();
    }

    
    @Bean
    public LogisticsManagedConnectionFactory logisticsManagedConnectionFactory(
            LogisticsResourceAdapter resourceAdapter) {
        LogisticsManagedConnectionFactory factory = new LogisticsManagedConnectionFactory();
        try {
            factory.setResourceAdapter(resourceAdapter);
        } catch (jakarta.resource.ResourceException e) {
            throw new RuntimeException("Failed to set resource adapter", e);
        }
        factory.setLogisticsEndpointUrl("http://logistics.example.com/api/v2");
        factory.setApiKey("delivery-system-key-12345");
        return factory;
    }

    
    @Bean
    public LogisticsConnectionFactory logisticsConnectionFactory(
            LogisticsManagedConnectionFactory managedConnectionFactory) {
        return new LogisticsConnectionFactory(managedConnectionFactory, null);
    }
}
