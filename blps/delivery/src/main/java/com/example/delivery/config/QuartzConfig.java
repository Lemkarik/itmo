package com.example.delivery.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

import com.example.delivery.scheduler.CourierTimeoutJob;


@Configuration
public class QuartzConfig {

    private static final long ONE_MINUTE_MS = 60_000L;
    private static final String[] LEGACY_SHOP_TIMEOUT_DELETE_SQL = {
        "DELETE FROM qrtz_fired_triggers WHERE (job_name = 'ShopTimeoutJob' AND job_group = 'DeliveryJobs') OR (trigger_name = 'ShopTimeoutTrigger' AND trigger_group = 'DeliveryTriggers')",
        "DELETE FROM qrtz_simple_triggers WHERE trigger_name = 'ShopTimeoutTrigger' AND trigger_group = 'DeliveryTriggers'",
        "DELETE FROM qrtz_simprop_triggers WHERE trigger_name = 'ShopTimeoutTrigger' AND trigger_group = 'DeliveryTriggers'",
        "DELETE FROM qrtz_cron_triggers WHERE trigger_name = 'ShopTimeoutTrigger' AND trigger_group = 'DeliveryTriggers'",
        "DELETE FROM qrtz_blob_triggers WHERE trigger_name = 'ShopTimeoutTrigger' AND trigger_group = 'DeliveryTriggers'",
        "DELETE FROM qrtz_triggers WHERE (job_name = 'ShopTimeoutJob' AND job_group = 'DeliveryJobs') OR (trigger_name = 'ShopTimeoutTrigger' AND trigger_group = 'DeliveryTriggers')",
        "DELETE FROM qrtz_job_details WHERE job_name = 'ShopTimeoutJob' AND job_group = 'DeliveryJobs'"
    };

    @Bean
    public SchedulerFactoryBeanCustomizer removeLegacyShopTimeoutJob(DataSource dataSource) {
        return schedulerFactoryBean -> deleteLegacyShopTimeoutJob(dataSource);
    }
    
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

    private void deleteLegacyShopTimeoutJob(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                for (String sql : LEGACY_SHOP_TIMEOUT_DELETE_SQL) {
                    statement.executeUpdate(sql);
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to remove obsolete shop-timeout Quartz state", exception);
        }
    }
}
