package com.example.delivery.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
    private static final String LEGACY_SHOP_TIMEOUT_JOB = "ShopTimeoutJob";
    private static final String LEGACY_SHOP_TIMEOUT_JOB_GROUP = "DeliveryJobs";
    private static final String LEGACY_SHOP_TIMEOUT_TRIGGER = "ShopTimeoutTrigger";
    private static final String LEGACY_SHOP_TIMEOUT_TRIGGER_GROUP = "DeliveryTriggers";

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
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                deleteLegacyFiredTriggers(connection);
                deleteLegacyTriggerDetails(connection);
                deleteLegacyTriggers(connection);
                deleteLegacyJob(connection);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to remove obsolete shop-timeout Quartz state", exception);
        }
    }

    private void deleteLegacyFiredTriggers(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                DELETE FROM qrtz_fired_triggers
                WHERE (job_name = ? AND job_group = ?)
                   OR (trigger_name = ? AND trigger_group = ?)
                """)) {
            setLegacyJobAndTriggerParameters(statement);
            statement.executeUpdate();
        }
    }

    private void deleteLegacyTriggers(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                DELETE FROM qrtz_triggers
                WHERE (job_name = ? AND job_group = ?)
                   OR (trigger_name = ? AND trigger_group = ?)
                """)) {
            setLegacyJobAndTriggerParameters(statement);
            statement.executeUpdate();
        }
    }

    private void deleteLegacyTriggerDetails(Connection connection) throws SQLException {
        deleteLegacyTriggerDetails(connection, "qrtz_simple_triggers");
        deleteLegacyTriggerDetails(connection, "qrtz_simprop_triggers");
        deleteLegacyTriggerDetails(connection, "qrtz_cron_triggers");
        deleteLegacyTriggerDetails(connection, "qrtz_blob_triggers");
    }

    private void deleteLegacyTriggerDetails(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                DELETE FROM %s
                WHERE trigger_name = ? AND trigger_group = ?
                """.formatted(tableName))) {
            statement.setString(1, LEGACY_SHOP_TIMEOUT_TRIGGER);
            statement.setString(2, LEGACY_SHOP_TIMEOUT_TRIGGER_GROUP);
            statement.executeUpdate();
        }
    }

    private void deleteLegacyJob(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                DELETE FROM qrtz_job_details
                WHERE job_name = ? AND job_group = ?
                """)) {
            statement.setString(1, LEGACY_SHOP_TIMEOUT_JOB);
            statement.setString(2, LEGACY_SHOP_TIMEOUT_JOB_GROUP);
            statement.executeUpdate();
        }
    }

    private void setLegacyJobAndTriggerParameters(PreparedStatement statement) throws SQLException {
        statement.setString(1, LEGACY_SHOP_TIMEOUT_JOB);
        statement.setString(2, LEGACY_SHOP_TIMEOUT_JOB_GROUP);
        statement.setString(3, LEGACY_SHOP_TIMEOUT_TRIGGER);
        statement.setString(4, LEGACY_SHOP_TIMEOUT_TRIGGER_GROUP);
    }
}
