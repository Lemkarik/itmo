package com.example.delivery.jca;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for JCA (Jakarta Connectors) integration.
 * 
 * This configures the Logistics Resource Adapter and ConnectionFactory
 * that allow the delivery service to communicate with an external
 * Logistics Enterprise Information System (EIS).
 * 
 * In a production environment, the connection factory would typically
 * be looked up via JNDI. Here we wire it directly for simplicity.
 */
@Configuration
public class JcaConfig {

    /**
     * Creates the LogisticsResourceAdapter instance.
     * This is the main JCA resource adapter that manages
     * connections to the external logistics system.
     */
    @Bean
    public LogisticsResourceAdapter logisticsResourceAdapter() {
        return new LogisticsResourceAdapter();
    }

    /**
     * Creates the LogisticsManagedConnectionFactory.
     * Configures the connection properties for the external
     * logistics system endpoint.
     */
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

    /**
     * Creates the LogisticsConnectionFactory.
     * This is the client-facing factory used by application code
     * to obtain connections to the logistics EIS.
     */
    @Bean
    public LogisticsConnectionFactory logisticsConnectionFactory(
            LogisticsManagedConnectionFactory managedConnectionFactory) {
        return new LogisticsConnectionFactory(managedConnectionFactory, null);
    }
}
