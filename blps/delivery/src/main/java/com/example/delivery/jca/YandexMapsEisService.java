package com.example.delivery.jca;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionFactory;
import jakarta.resource.cci.Interaction;
import jakarta.resource.cci.MappedRecord;
import jakarta.resource.cci.Record;

@Service
public class YandexMapsEisService {

    private static final Logger log = LoggerFactory.getLogger(YandexMapsEisService.class);

    private final ConnectionFactory connectionFactory;

    public YandexMapsEisService(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public Double calculateDistanceKm(String fromAddress, String toAddress) throws LogisticsException {
        Connection connection = null;
        Interaction interaction = null;
        try {
            connection = connectionFactory.getConnection();
            interaction = connection.createInteraction();

            MappedRecord<String, Object> request =
                    connectionFactory.getRecordFactory().createMappedRecord("calculateDistance");
            request.put("fromAddress", fromAddress);
            request.put("toAddress", toAddress);

            Record result = interaction.execute(null, request);
            if (!(result instanceof MappedRecord<?, ?> response)) {
                throw new LogisticsException("Yandex Maps JCA adapter returned an invalid response");
            }

            Object distance = response.get("distanceKm");
            if (!(distance instanceof Number number)) {
                throw new LogisticsException("Yandex Maps JCA adapter did not return distanceKm");
            }
            return BigDecimal.valueOf(number.doubleValue())
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        } catch (ResourceException e) {
            throw new LogisticsException("Yandex Maps EIS connection failed", e);
        } catch (RuntimeException e) {
            throw new LogisticsException("Yandex Maps JCA connection factory is unavailable", e);
        } finally {
            closeInteraction(interaction);
            closeConnection(connection);
        }
    }

    private void closeInteraction(Interaction interaction) {
        if (interaction != null) {
            try {
                interaction.close();
            } catch (ResourceException e) {
                log.warn("Error closing Yandex Maps interaction: {}", e.getMessage());
            }
        }
    }

    private void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (ResourceException e) {
                log.warn("Error closing Yandex Maps connection: {}", e.getMessage());
            }
        }
    }
}
