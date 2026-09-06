package com.example.delivery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.messaging.amqp")
public record AmqpProperties(
        String url,
        String username,
        String password
) {
}

