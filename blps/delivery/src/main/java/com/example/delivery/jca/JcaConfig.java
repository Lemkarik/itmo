package com.example.delivery.jca;

import org.springframework.beans.factory.annotation.Value;
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
            LogisticsResourceAdapter resourceAdapter,
            @Value("${app.eis.yandex.geocoder-url:https://geocode-maps.yandex.ru/v1/}") String geocoderUrl,
            @Value("${app.eis.yandex.api-key:}") String apiKey) {
        LogisticsManagedConnectionFactory factory = new LogisticsManagedConnectionFactory();
        try {
            factory.setResourceAdapter(resourceAdapter);
        } catch (jakarta.resource.ResourceException e) {
            throw new RuntimeException("Failed to set resource adapter", e);
        }
        factory.setLogisticsEndpointUrl(geocoderUrl);
        factory.setApiKey(apiKey);
        return factory;
    }

    
    @Bean
    public LogisticsConnectionFactory logisticsConnectionFactory(
            LogisticsManagedConnectionFactory managedConnectionFactory) {
        return new LogisticsConnectionFactory(managedConnectionFactory, null);
    }
}
