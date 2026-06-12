package com.example.delivery;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@EnableScheduling
public class DeliveryApplication {

	public static void main(String[] args) {
		
		
		SpringApplication app = new SpringApplication(DeliveryApplication.class);
		app.setWebApplicationType(org.springframework.boot.WebApplicationType.SERVLET);
		app.run(args);
	}

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}

