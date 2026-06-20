package com.example.delivery.jca;

import jakarta.resource.cci.Connection;


public interface LogisticsConnection extends Connection {

    
    String submitOrder(String orderData) throws LogisticsException;

    
    String checkDeliveryStatus(String trackingId) throws LogisticsException;

    
    boolean cancelDelivery(String trackingId) throws LogisticsException;
}
