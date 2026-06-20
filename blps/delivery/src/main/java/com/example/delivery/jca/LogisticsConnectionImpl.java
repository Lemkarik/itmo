package com.example.delivery.jca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of LogisticsConnection.
 * In a real system, this would communicate with an actual
 * logistics provider API. Here we simulate the interaction.
 */
public class LogisticsConnectionImpl implements LogisticsConnection {

    private static final Logger log = LoggerFactory.getLogger(LogisticsConnectionImpl.class);

    private final LogisticsManagedConnection managedConnection;
    private boolean closed = false;

    public LogisticsConnectionImpl(LogisticsManagedConnection managedConnection) {
        this.managedConnection = managedConnection;
        log.debug("Logistics connection opened");
    }

    @Override
    public String submitOrder(String orderData) throws LogisticsException {
        checkClosed();
        log.info("Submitting order to logistics EIS via JCA: {}", orderData);

        try {
            // Simulate network call to external logistics provider
            Thread.sleep(150);

            // Generate a mock tracking ID
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

        // Simulate status check
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
    public void close() {
        if (!closed) {
            closed = true;
            managedConnection.connectionClosed(this);
            log.debug("Logistics connection closed");
        }
    }

    @Override
    public jakarta.resource.cci.ResultSetInfo getResultSetInfo() {
        return null; // Not applicable for this resource adapter
    }

    @Override
    public jakarta.resource.cci.ConnectionMetaData getMetaData() {
        return null; // Not applicable for this resource adapter
    }

    @Override
    public jakarta.resource.cci.LocalTransaction getLocalTransaction() {
        return null; // Not applicable for this resource adapter
    }

    @Override
    public jakarta.resource.cci.Interaction createInteraction() {
        return null; // Not applicable for this resource adapter
    }

    private void checkClosed() throws LogisticsException {
        if (closed) {
            throw new LogisticsException("Logistics connection is closed");
        }
    }
}
