package com.example.delivery.jca;

import jakarta.resource.cci.Connection;

/**
 * Connection interface for the Logistics EIS (Enterprise Information System).
 * This represents a connection to an external logistics provider
 * via JCA (Jakarta Connectors).
 * 
 * Methods allow submitting orders, checking delivery status,
 * and tracking shipments through the external logistics system.
 */
public interface LogisticsConnection extends Connection {

    /**
     * Submit an order to the external logistics system for delivery.
     * 
     * @param orderData JSON-formatted order data
     * @return tracking ID assigned by the logistics system
     * @throws LogisticsException if submission fails
     */
    String submitOrder(String orderData) throws LogisticsException;

    /**
     * Check the current status of a delivery in the external system.
     * 
     * @param trackingId the tracking ID returned from submitOrder
     * @return current delivery status description
     * @throws LogisticsException if status check fails
     */
    String checkDeliveryStatus(String trackingId) throws LogisticsException;

    /**
     * Cancel a delivery in the external logistics system.
     * 
     * @param trackingId the tracking ID to cancel
     * @return true if cancellation was successful
     * @throws LogisticsException if cancellation fails
     */
    boolean cancelDelivery(String trackingId) throws LogisticsException;
}
