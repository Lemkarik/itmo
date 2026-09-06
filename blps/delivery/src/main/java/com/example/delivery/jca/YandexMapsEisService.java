package com.example.delivery.jca;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class YandexMapsEisService {

    private static final Logger log = LoggerFactory.getLogger(YandexMapsEisService.class);

    private final LogisticsConnectionFactory connectionFactory;

    public YandexMapsEisService(LogisticsConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public Double calculateDistanceKm(String fromAddress, String toAddress) throws LogisticsException {
        LogisticsConnection connection = null;
        try {
            jakarta.resource.cci.Connection cciConnection = connectionFactory.getConnection();
            connection = (LogisticsConnection) cciConnection;

            Double km = connection.calculateDistanceKm(fromAddress, toAddress);
            if (km == null) {
                return null;
            }
            return BigDecimal.valueOf(km).setScale(2, RoundingMode.HALF_UP).doubleValue();
        } catch (LogisticsException e) {
            throw e;
        } catch (jakarta.resource.ResourceException e) {
            throw new LogisticsException("Yandex Maps EIS connection failed", e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (jakarta.resource.ResourceException e) {
                    log.warn("Error closing Yandex Maps connection: {}", e.getMessage());
                }
            }
        }
    }
}

