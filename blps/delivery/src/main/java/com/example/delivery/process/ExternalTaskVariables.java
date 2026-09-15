package com.example.delivery.process;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Date;

import org.camunda.bpm.client.task.ExternalTask;

import com.example.delivery.exception.BadRequestException;

final class ExternalTaskVariables {

    private ExternalTaskVariables() {
    }

    static Long requiredLong(ExternalTask task, String name) {
        Object value = task.getVariable(name);
        if (value instanceof Number number) {
            try {
                return new BigDecimal(number.toString()).longValueExact();
            } catch (ArithmeticException | NumberFormatException exception) {
                throw new BadRequestException(name + " must be an integer");
            }
        }
        if (value instanceof String string && !string.isBlank()) {
            try {
                return Long.valueOf(string.trim());
            } catch (NumberFormatException ignored) {
                throw new BadRequestException(name + " must be an integer");
            }
        }
        throw new BadRequestException(name + " is required");
    }

    static Long requiredPositiveLong(ExternalTask task, String name) {
        long value = requiredLong(task, name);
        if (value <= 0) {
            throw new BadRequestException(name + " must be a positive integer");
        }
        return value;
    }

    static int requiredPositiveInteger(ExternalTask task, String name) {
        long value = requiredPositiveLong(task, name);
        if (value > Integer.MAX_VALUE) {
            throw new BadRequestException(name + " must be a positive integer");
        }
        return (int) value;
    }

    static String requiredString(ExternalTask task, String name) {
        Object value = task.getVariable(name);
        if (!(value instanceof String string) || string.isBlank()) {
            throw new BadRequestException(name + " is required");
        }
        return string.trim();
    }

    static OffsetDateTime dateTime(ExternalTask task, String name) {
        Object value = task.getVariable(name);
        if (value instanceof OffsetDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof Date date) {
            return date.toInstant().atOffset(ZoneOffset.UTC);
        }
        if (value instanceof String string && !string.isBlank()) {
            try {
                return OffsetDateTime.parse(string.trim());
            } catch (DateTimeParseException ignored) {
                try {
                    return LocalDateTime.parse(string.trim()).atOffset(ZoneOffset.UTC);
                } catch (DateTimeParseException ignoredAgain) {
                    return null;
                }
            }
        }
        return null;
    }
}
