package com.example.delivery.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Cleans up stale Quartz data from previous runs.
 * This is needed because old serialized job data may be incompatible
 * with the current Quartz configuration.
 */
@Component
@DependsOnDatabaseInitialization
public class QuartzSchemaCleaner {

    private static final Logger log = LoggerFactory.getLogger(QuartzSchemaCleaner.class);

    private final JdbcTemplate jdbcTemplate;

    public QuartzSchemaCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void cleanQuartzTables() {
        log.info("Cleaning Quartz scheduler tables...");
        String[] tables = {
            "qrtz_blob_triggers",
            "qrtz_calendars",
            "qrtz_cron_triggers",
            "qrtz_fired_triggers",
            "qrtz_job_details",
            "qrtz_locks",
            "qrtz_paused_trigger_grps",
            "qrtz_scheduler_state",
            "qrtz_simple_triggers",
            "qrtz_simprop_triggers",
            "qrtz_triggers"
        };
        for (String table : tables) {
            try {
                jdbcTemplate.update("DELETE FROM " + table);
                log.debug("Cleared table: {}", table);
            } catch (Exception e) {
                // Table might not exist yet, which is fine
                log.debug("Could not clear table {}: {}", table, e.getMessage());
            }
        }
        log.info("Quartz scheduler tables cleaned.");
    }
}
