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
		System.out.println(System.getProperty("java.version"));
		SpringApplication app = new SpringApplication(DeliveryApplication.class);
		app.setWebApplicationType(org.springframework.boot.WebApplicationType.SERVLET);
		app.run(args);
	}

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}

