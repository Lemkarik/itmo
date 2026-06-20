package com.example.delivery.config;

import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

import com.example.delivery.scheduler.CourierTimeoutJob;
import com.example.delivery.scheduler.ShopTimeoutJob;

/**
 * Configuration for Quartz Scheduler.
 * Defines jobs and triggers for periodic tasks:
 * 
 * 1. ShopTimeoutJob  - runs every 60 seconds, checks for shop decision timeouts
 * 2. CourierTimeoutJob - runs every 60 seconds (with initial delay),
 *    checks for courier assignment timeouts and retries assignments
 * 
 * The scheduler itself is auto-configured by Spring Boot's
 * QuartzAutoConfiguration using the JDBC-backed job store
 * for cluster-safe execution.
 */
@Configuration
public class QuartzConfig {

    private static final long ONE_MINUTE_MS = 60_000L;

    /**
     * JobDetail for ShopTimeoutJob.
     */
    @Bean
    public JobDetailFactoryBean shopTimeoutJobDetail() {
        JobDetailFactoryBean factory = new JobDetailFactoryBean();
        factory.setJobClass(ShopTimeoutJob.class);
        factory.setDurability(true);
        factory.setName("ShopTimeoutJob");
        factory.setGroup("DeliveryJobs");
        factory.setDescription("Checks for orders that timed out waiting for shop decision");
        return factory;
    }

    /**
     * Trigger for ShopTimeoutJob - runs every 60 seconds.
     */
    @Bean
    public SimpleTriggerFactoryBean shopTimeoutTrigger(JobDetail shopTimeoutJobDetail) {
        SimpleTriggerFactoryBean factory = new SimpleTriggerFactoryBean();
        factory.setJobDetail(shopTimeoutJobDetail);
        factory.setStartDelay(0L);
        factory.setRepeatInterval(ONE_MINUTE_MS);
        factory.setName("ShopTimeoutTrigger");
        factory.setGroup("DeliveryTriggers");
        factory.setDescription("Trigger for ShopTimeoutJob every 60 seconds");
        return factory;
    }

    /**
     * JobDetail for CourierTimeoutJob.
     */
    @Bean
    public JobDetailFactoryBean courierTimeoutJobDetail() {
        JobDetailFactoryBean factory = new JobDetailFactoryBean();
        factory.setJobClass(CourierTimeoutJob.class);
        factory.setDurability(true);
        factory.setName("CourierTimeoutJob");
        factory.setGroup("DeliveryJobs");
        factory.setDescription("Checks for courier assignment timeouts and retries assignments");
        return factory;
    }

    /**
     * Trigger for CourierTimeoutJob - runs every 60 seconds with 15 second delay.
     */
    @Bean
    public SimpleTriggerFactoryBean courierTimeoutTrigger(JobDetail courierTimeoutJobDetail) {
        SimpleTriggerFactoryBean factory = new SimpleTriggerFactoryBean();
        factory.setJobDetail(courierTimeoutJobDetail);
        factory.setStartDelay(15_000L);
        factory.setRepeatInterval(ONE_MINUTE_MS);
        factory.setName("CourierTimeoutTrigger");
        factory.setGroup("DeliveryTriggers");
        factory.setDescription("Trigger for CourierTimeoutJob every 60 seconds (with delay)");
        return factory;
    }
}
