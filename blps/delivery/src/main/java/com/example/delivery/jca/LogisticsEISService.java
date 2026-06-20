package com.example.delivery.jca;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.delivery.model.Order;
import com.example.delivery.model.OrderStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
public class LogisticsEISService {

    private static final Logger log = LoggerFactory.getLogger(LogisticsEISService.class);

    private final LogisticsConnectionFactory connectionFactory;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public LogisticsEISService(
            LogisticsConnectionFactory connectionFactory,
            ObjectMapper objectMapper,
            Clock clock) {
        this.connectionFactory = connectionFactory;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    
    public String submitOrderToLogistics(Order order, String eventType)
            throws LogisticsException {
        LogisticsConnection connection = null;
        try {
            jakarta.resource.cci.Connection cciConnection = connectionFactory.getConnection();
            connection = (LogisticsConnection) cciConnection;

            
            String orderData = buildOrderPayload(order, eventType);
            String trackingId = connection.submitOrder(orderData);

            log.info("Order {} submitted to logistics EIS via JCA. Tracking: {}",
                    order.getId(), trackingId);

            return trackingId;
        } catch (LogisticsException e) {
            throw e;
        } catch (jakarta.resource.ResourceException e) {
            log.error("Failed to submit order {} to logistics EIS: {}",
                    order.getId(), e.getMessage(), e);
            throw new LogisticsException("Logistics EIS connection failed for order "
                    + order.getId(), e);
        } catch (Exception e) {
            log.error("Failed to submit order {} to logistics EIS: {}",
                    order.getId(), e.getMessage(), e);
            throw new LogisticsException("Logistics EIS submission failed for order "
                    + order.getId(), e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (jakarta.resource.ResourceException e) {
                    log.warn("Error closing logistics connection: {}", e.getMessage());
                }
            }
        }
    }

    
    public String checkDeliveryStatus(String trackingId) throws LogisticsException {
        LogisticsConnection connection = null;
        try {
            jakarta.resource.cci.Connection cciConnection = connectionFactory.getConnection();
            connection = (LogisticsConnection) cciConnection;
            return connection.checkDeliveryStatus(trackingId);
        } catch (LogisticsException e) {
            throw e;
        } catch (jakarta.resource.ResourceException e) {
            throw new LogisticsException("Failed to connect for delivery status check", e);
        } catch (Exception e) {
            throw new LogisticsException("Failed to check delivery status", e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (jakarta.resource.ResourceException e) {
                    log.warn("Error closing logistics connection: {}", e.getMessage());
                }
            }
        }
    }

    
    public boolean cancelDelivery(String trackingId) throws LogisticsException {
        LogisticsConnection connection = null;
        try {
            jakarta.resource.cci.Connection cciConnection = connectionFactory.getConnection();
            connection = (LogisticsConnection) cciConnection;
            return connection.cancelDelivery(trackingId);
        } catch (LogisticsException e) {
            throw e;
        } catch (jakarta.resource.ResourceException e) {
            throw new LogisticsException("Failed to connect for delivery cancellation", e);
        } catch (Exception e) {
            throw new LogisticsException("Failed to cancel delivery", e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (jakarta.resource.ResourceException e) {
                    log.warn("Error closing logistics connection: {}", e.getMessage());
                }
            }
        }
    }

    private String buildOrderPayload(Order order, String eventType) {
        try {
            return objectMapper.writeValueAsString(java.util.Map.of(
                    "orderId", order.getId(),
                    "eventType", eventType,
                    "customerId", order.getCustomer().getId(),
                    "shopId", order.getShop().getId(),
                    "courierId", order.getCourier() != null ? order.getCourier().getId() : null,
                    "status", order.getStatus().name(),
                    "deliveryAddress", order.getDeliveryAddress(),
                    "deliveryTime", order.getDeliveryTime() != null
                            ? order.getDeliveryTime().toString() : null,
                    "timestamp", java.time.OffsetDateTime.now(clock).toString()
            ));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize order payload: {}", e.getMessage());
            return "{\"orderId\":" + order.getId() + ",\"eventType\":\"" + eventType + "\"}";
        }
    }
}
