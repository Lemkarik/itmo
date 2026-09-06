package com.example.delivery.config;

import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Configuration
@ConditionalOnProperty(name = "app.messaging.embedded.enabled", havingValue = "true")
public class EmbeddedArtemisBrokerConfig {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedArtemisBrokerConfig.class);

    private EmbeddedActiveMQ embedded;

    @Value("${app.messaging.embedded.amqp-port:5672}")
    private int amqpPort;

    @PostConstruct
    public void startEmbeddedBroker() throws Exception {
        log.info("Starting embedded ActiveMQ Artemis broker (AMQP 1.0 on port {})...", amqpPort);

        System.setProperty("org.apache.activemq.audit.enabled", "false");

        ConfigurationImpl config = new ConfigurationImpl();
        config.setName("DeliveryArtemisEmbedded");
        config.setPersistenceEnabled(false);
        config.setSecurityEnabled(false);
        config.setJMXManagementEnabled(false);

        config.setJournalDirectory("target/artemis/journal");
        config.setBindingsDirectory("target/artemis/bindings");
        config.setLargeMessagesDirectory("target/artemis/large-messages");
        config.setPagingDirectory("target/artemis/paging");

        config.addAcceptorConfiguration("amqp", "tcp://0.0.0.0:" + amqpPort + "?protocols=AMQP");

        embedded = new EmbeddedActiveMQ();
        embedded.setConfiguration(config);
        embedded.start();

        log.info("Embedded Artemis broker started");
    }

    @PreDestroy
    public void stopEmbeddedBroker() throws Exception {
        if (embedded == null) {
            return;
        }
        log.info("Stopping embedded Artemis broker...");
        embedded.stop();
        log.info("Embedded Artemis broker stopped");
    }
}

