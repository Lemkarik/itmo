package com.example.delivery.service;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.delivery.jca.LogisticsException;
import com.example.delivery.jca.YandexMapsEisService;

@Service
public class DeliveryEstimationService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEstimationService.class);

    private final YandexMapsEisService yandexMapsEisService;
    private final Clock clock;
    private final String shopAddress;
    private final int baseMinutes;
    private final double speedKmh;

    public DeliveryEstimationService(
            YandexMapsEisService yandexMapsEisService,
            Clock clock,
            @Value("${app.shop.address:}") String shopAddress,
            @Value("${app.delivery.eta.base-minutes:20}") int baseMinutes,
            @Value("${app.delivery.eta.speed-kmh:25}") double speedKmh) {
        this.yandexMapsEisService = yandexMapsEisService;
        this.clock = clock;
        this.shopAddress = shopAddress;
        this.baseMinutes = baseMinutes;
        this.speedKmh = speedKmh;
    }

    public OffsetDateTime chooseDeliveryTime(OffsetDateTime requestedDeliveryTime, String destinationAddress) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        return chooseDeliveryTime(now, requestedDeliveryTime, destinationAddress);
    }

    public OffsetDateTime chooseDeliveryTime(
            OffsetDateTime now,
            OffsetDateTime requestedDeliveryTime,
            String destinationAddress) {
        return decideDeliveryTime(now, requestedDeliveryTime, destinationAddress).chosen();
    }

    public DeliveryTimeDecision decideDeliveryTime(
            OffsetDateTime now,
            OffsetDateTime requestedDeliveryTime,
            String destinationAddress) {
        OffsetDateTime estimated = estimateDeliveryTime(now, destinationAddress);
        if (estimated == null) {
            return new DeliveryTimeDecision(requestedDeliveryTime, null, false);
        }

        if (requestedDeliveryTime == null) {
            return new DeliveryTimeDecision(estimated, estimated, false);
        }

        boolean adjusted = estimated.isAfter(requestedDeliveryTime);
        OffsetDateTime chosen = adjusted ? estimated : requestedDeliveryTime;
        return new DeliveryTimeDecision(chosen, estimated, adjusted);
    }

    public OffsetDateTime estimateDeliveryTime(OffsetDateTime now, String destinationAddress) {
        if (destinationAddress == null || destinationAddress.isBlank()) {
            return null;
        }
        if (shopAddress == null || shopAddress.isBlank()) {
            log.debug("Shop address is not configured (app.shop.address). ETA estimation skipped.");
            return null;
        }
        if (speedKmh <= 0) {
            log.debug("Invalid speed (app.delivery.eta.speed-kmh={}). ETA estimation skipped.", speedKmh);
            return null;
        }

        try {
            Double km = yandexMapsEisService.calculateDistanceKm(shopAddress, destinationAddress);
            if (km == null) {
                return null;
            }
            long travelMinutes = Math.round((km / speedKmh) * 60.0);
            long totalMinutes = Math.max(0, baseMinutes) + Math.max(0, travelMinutes);
            return now.plusMinutes(totalMinutes);
        } catch (LogisticsException e) {
            log.warn("ETA estimation failed: {}", e.getMessage());
            return null;
        }
    }

    public record DeliveryTimeDecision(
            OffsetDateTime chosen,
            OffsetDateTime estimatedEarliest,
            boolean adjustedFromRequested
    ) {
    }
}
