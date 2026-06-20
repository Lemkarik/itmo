package com.example.delivery.config;

import jakarta.jms.ConnectionFactory;

import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Configuration
@EnableJms
public class ArtemisConfig {

    private static final Logger log = LoggerFactory.getLogger(ArtemisConfig.class);

    private EmbeddedActiveMQ embedded;

    /**
     * Starts the embedded Artemis broker manually, with both in-VM and TCP acceptors.
     */
    @PostConstruct
    public void startEmbeddedBroker() throws Exception {
        log.info("Starting embedded Artemis broker...");
        // Disable JMX audit logging to avoid Subject.getSubject() issues on Java 23+
        System.setProperty("org.apache.activemq.audit.enabled", "false");
        ConfigurationImpl config = new ConfigurationImpl();
        config.setPersistenceEnabled(false);
        config.setSecurityEnabled(false);
        config.setJournalDirectory("target/artemis/journal");
        config.setBindingsDirectory("target/artemis/bindings");
        config.setLargeMessagesDirectory("target/artemis/large-messages");
        config.setPagingDirectory("target/artemis/paging");
        config.setName("DeliveryArtemisEmbedded");
        // Add in-VM acceptor for JVM-internal connections
        // The "0" means any available node ID for this JVM
        // Disable JMX management to avoid Subject.getSubject() issues on Java 23+
        config.setJMXManagementEnabled(false);
        config.addAcceptorConfiguration("invm", "vm://0");

        embedded = new EmbeddedActiveMQ();
        embedded.setConfiguration(config);
        embedded.start();
        log.info("Embedded Artemis broker started successfully with in-VM and TCP acceptors");
    }

    /**
     * Stops the embedded broker on shutdown.
     */
    @PreDestroy
    public void stopEmbeddedBroker() throws Exception {
        if (embedded != null) {
            log.info("Stopping embedded Artemis broker...");
            embedded.stop();
            log.info("Embedded Artemis broker stopped");
        }
    }

    /**
     * Creates a ConnectionFactory that connects to the embedded broker via in-VM.
     * Uses non-XA connection factory to avoid JTA transaction complications.
     */
    @Bean
    public ConnectionFactory jmsConnectionFactory() {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("vm://0");
        cf.setRetryInterval(1000);
        cf.setRetryIntervalMultiplier(1.0);
        cf.setMaxRetryInterval(5000);
        cf.setReconnectAttempts(-1);
        return cf;
    }

    @Bean
    public JmsTemplate jmsTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {

        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setDeliveryPersistent(true);
        template.setMessageConverter(messageConverter);

        return template;
    }

    /**
     * Creates the JMS listener container factory with session transacted = false
     * to avoid XA/JTA conflicts. Each message is acknowledged individually.
     */
    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrency("1-5");
        factory.setSessionTransacted(false);
        factory.setSessionAcknowledgeMode(jakarta.jms.Session.AUTO_ACKNOWLEDGE);
        factory.setCacheLevelName("CACHE_NONE");
        return factory;
    }

    /**
     * Configures Jackson as the message converter for JMS.
     */
    @Bean
    public MessageConverter jacksonJmsMessageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }

    /**
     * Configures Jackson ObjectMapper with JSR-310 support.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
