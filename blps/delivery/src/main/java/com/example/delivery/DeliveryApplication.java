package com.example.delivery;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JndiConnectionFactoryAutoConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = {
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
