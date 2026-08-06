package com.example.delivery.jca;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LogisticsConnectionImpl implements LogisticsConnection {

    private static final Logger log = LoggerFactory.getLogger(LogisticsConnectionImpl.class);
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final LogisticsManagedConnection managedConnection;
    private final String endpointUrl;
    private final String apiKey;
    private boolean closed = false;

    public LogisticsConnectionImpl(
            LogisticsManagedConnection managedConnection,
            String endpointUrl,
            String apiKey) {
        this.managedConnection = managedConnection;
        this.endpointUrl = endpointUrl;
        this.apiKey = apiKey;
        log.debug("Logistics connection opened");
    }

    @Override
    public String submitOrder(String orderData) throws LogisticsException {
        checkClosed();
        log.info("Submitting order to logistics EIS via JCA: {}", orderData);

        try {
            
            Thread.sleep(150);

            
            String trackingId = "TRK-" + System.currentTimeMillis() + "-" + orderData.hashCode();
            log.info("Order submitted to logistics EIS. Tracking ID: {}", trackingId);
            return trackingId;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LogisticsException("Interrupted while submitting order to logistics EIS", e);
        } catch (Exception e) {
            throw new LogisticsException("Failed to submit order to logistics EIS: " + e.getMessage(), e);
        }
    }

    @Override
    public String checkDeliveryStatus(String trackingId) throws LogisticsException {
        checkClosed();
        log.info("Checking delivery status for tracking ID: {}", trackingId);

        
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return "IN_TRANSIT";
    }

    @Override
    public boolean cancelDelivery(String trackingId) throws LogisticsException {
        checkClosed();
        log.info("Cancelling delivery for tracking ID: {}", trackingId);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Delivery {} cancelled in logistics EIS", trackingId);
        return true;
    }

    @Override
    public Double calculateDistanceKm(String fromAddress, String toAddress) throws LogisticsException {
        checkClosed();

        if (fromAddress == null || fromAddress.isBlank() || toAddress == null || toAddress.isBlank()) {
            return null;
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Yandex Maps API key is not configured (app.eis.yandex.api-key). Distance calculation skipped.");
            return null;
        }

        GeoPoint from = geocode(fromAddress);
        GeoPoint to = geocode(toAddress);

        double km = haversineKm(from.lat(), from.lon(), to.lat(), to.lon());
        return km;
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            managedConnection.connectionClosed(this);
            log.debug("Logistics connection closed");
        }
    }

    @Override
    public jakarta.resource.cci.ResultSetInfo getResultSetInfo() {
        return null; 
    }

    @Override
    public jakarta.resource.cci.ConnectionMetaData getMetaData() {
        return null; 
    }

    @Override
    public jakarta.resource.cci.LocalTransaction getLocalTransaction() {
        return null; 
    }

    @Override
    public jakarta.resource.cci.Interaction createInteraction() {
        return null; 
    }

    private void checkClosed() throws LogisticsException {
        if (closed) {
            throw new LogisticsException("Logistics connection is closed");
        }
    }

    private GeoPoint geocode(String address) throws LogisticsException {
        String baseUrl = endpointUrl != null && !endpointUrl.isBlank()
                ? endpointUrl
                : "https://geocode-maps.yandex.ru/v1/";

        String url = baseUrl;
        if (!url.endsWith("/")) {
            url = url + "/";
        }

        String query = url
                + "?apikey=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&geocode=" + URLEncoder.encode(address, StandardCharsets.UTF_8)
                + "&format=json";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(query))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new LogisticsException("Geocoder returned HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode posNode = root.at("/response/GeoObjectCollection/featureMember/0/GeoObject/Point/pos");
            if (posNode.isMissingNode() || posNode.asText().isBlank()) {
                throw new LogisticsException("Geocoder returned empty result for address: " + address);
            }

            String[] parts = posNode.asText().trim().split("\\s+");
            if (parts.length != 2) {
                throw new LogisticsException("Unexpected geocoder pos format: " + posNode.asText());
            }
            double lon = Double.parseDouble(parts[0]);
            double lat = Double.parseDouble(parts[1]);
            return new GeoPoint(lat, lon);
        } catch (LogisticsException e) {
            throw e;
        } catch (Exception e) {
            throw new LogisticsException("Failed to geocode address: " + address + " (" + e.getMessage() + ")", e);
        }
    }

    private record GeoPoint(double lat, double lon) {
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0088;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return r * c;
    }
}
