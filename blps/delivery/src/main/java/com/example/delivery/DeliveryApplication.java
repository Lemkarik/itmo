package com.example.delivery;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jms.autoconfigure.JmsAutoConfiguration;
import org.springframework.boot.jms.autoconfigure.JndiConnectionFactoryAutoConfiguration;
import org.springframework.boot.artemis.autoconfigure.ArtemisAutoConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = {
    ArtemisAutoConfiguration.class,
    JmsAutoConfiguration.class,
    JndiConnectionFactoryAutoConfiguration.class
})
public class DeliveryApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeliveryApplication.class, args);
	}

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}
