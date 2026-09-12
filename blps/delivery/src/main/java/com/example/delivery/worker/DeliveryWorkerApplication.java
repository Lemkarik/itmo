package com.example.delivery.worker;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JndiConnectionFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.example.delivery.config.AmqpMessagingConfig;
import com.example.delivery.config.JmsListenerConfig;
import com.example.delivery.config.JtaConfig;
import com.example.delivery.config.QuartzConfig;
import com.example.delivery.messaging.CourierAssignmentListener;
import com.example.delivery.messaging.MessageProducerService;
import com.example.delivery.messaging.NotificationDispatchListener;
import com.example.delivery.model.Order;
import com.example.delivery.repository.OrderRepository;
import com.example.delivery.scheduler.CourierTimeoutJob;
import com.example.delivery.service.CourierAssignmentService;
import com.example.delivery.service.NotificationService;

@SpringBootApplication(exclude = {
        JmsAutoConfiguration.class,
        JndiConnectionFactoryAutoConfiguration.class
})
@EntityScan(basePackageClasses = Order.class)
@EnableJpaRepositories(basePackageClasses = OrderRepository.class)
@Profile("worker")
@Import({
        AmqpMessagingConfig.class,
        JmsListenerConfig.class,
        JtaConfig.class,
        QuartzConfig.class,
        CourierAssignmentListener.class,
        NotificationDispatchListener.class,
        MessageProducerService.class,
        CourierAssignmentService.class,
        NotificationService.class,
        CourierTimeoutJob.class
})
public class DeliveryWorkerApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(DeliveryWorkerApplication.class);
        application.setAdditionalProfiles("worker");
        application.run(args);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
