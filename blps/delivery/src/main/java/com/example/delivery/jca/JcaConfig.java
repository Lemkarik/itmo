package com.example.delivery.jca;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jndi.JndiObjectFactoryBean;

import jakarta.resource.cci.ConnectionFactory;

@Configuration
public class JcaConfig {

    @Bean
    public JndiObjectFactoryBean yandexMapsConnectionFactory(
            @Value("${app.eis.yandex.connection-factory-jndi-name:java:/eis/YandexMapsConnectionFactory}")
            String jndiName) {
        JndiObjectFactoryBean factoryBean = new JndiObjectFactoryBean();
        factoryBean.setJndiName(jndiName);
        factoryBean.setProxyInterface(ConnectionFactory.class);
        factoryBean.setLookupOnStartup(true);
        factoryBean.setCache(true);
        return factoryBean;
    }
}
