package com.example.delivery.config;

import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

import com.example.delivery.scheduler.CourierTimeoutJob;


@Configuration
public class QuartzConfig {

    private static final long ONE_MINUTE_MS = 60_000L;
    
    @Bean
    public JobDetailFactoryBean courierTimeoutJobDetail() {
        JobDetailFactoryBean factory = new JobDetailFactoryBean();
        factory.setJobClass(CourierTimeoutJob.class);
        factory.setDurability(true);
        factory.setName("CourierTimeoutJob");
        factory.setGroup("DeliveryJobs");
        factory.setDescription("Periodically requeues pending courier-search messages");
        return factory;
    }

    
    @Bean
    public SimpleTriggerFactoryBean courierTimeoutTrigger(JobDetail courierTimeoutJobDetail) {
        SimpleTriggerFactoryBean factory = new SimpleTriggerFactoryBean();
        factory.setJobDetail(courierTimeoutJobDetail);
        factory.setStartDelay(0L);
        factory.setRepeatInterval(ONE_MINUTE_MS);
        factory.setName("CourierTimeoutTrigger");
        factory.setGroup("DeliveryTriggers");
        factory.setDescription("Trigger for periodic courier-search message retry");
        return factory;
    }
}
