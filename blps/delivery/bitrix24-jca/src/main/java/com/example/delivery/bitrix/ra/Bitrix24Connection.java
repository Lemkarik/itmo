package com.example.delivery.bitrix.ra;

import java.math.BigDecimal;

import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionMetaData;
import jakarta.resource.cci.Interaction;
import jakarta.resource.cci.LocalTransaction;
import jakarta.resource.cci.ResultSetInfo;

public final class Bitrix24Connection implements Connection {

    private Bitrix24ManagedConnection managedConnection;
    private boolean closed;

    Bitrix24Connection(Bitrix24ManagedConnection managedConnection) {
        this.managedConnection = managedConnection;
    }

    @Override
    public synchronized Interaction createInteraction() throws ResourceException {
        ensureOpen();
        return new Bitrix24Interaction(this);
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException("Bitrix24 adapter does not support local transactions");
    }

    @Override
    public synchronized ConnectionMetaData getMetaData() throws ResourceException {
        ensureOpen();
        return new Bitrix24ConnectionMetaData();
    }

    @Override
    public ResultSetInfo getResultSetInfo() throws ResourceException {
        throw new NotSupportedException("Bitrix24 adapter does not return result sets");
    }

    @Override
    public void close() {
        Bitrix24ManagedConnection owner;
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            owner = managedConnection;
        }
        if (owner != null) {
            owner.connectionClosed(this);
        }
    }

    synchronized long createOrder(
            long localOrderId,
            String title,
            String stageId,
            String customerName,
            String deliveryAddress,
            String deliveryTime,
            BigDecimal amount,
            String comments) throws ResourceException {
        ensureOpen();
        return managedConnection.createOrder(
                localOrderId, title, stageId, customerName,
                deliveryAddress, deliveryTime, amount, comments);
    }

    synchronized void updateOrderStatus(long crmItemId, String stageId)
            throws ResourceException {
        ensureOpen();
        managedConnection.updateOrderStatus(crmItemId, stageId);
    }

    synchronized void associate(Bitrix24ManagedConnection newManagedConnection) {
        managedConnection = newManagedConnection;
        closed = false;
    }

    synchronized void invalidate() {
        closed = true;
        managedConnection = null;
    }

    synchronized void ensureOpen() throws ResourceException {
        if (closed || managedConnection == null) {
            throw new ResourceException("Bitrix24 connection is closed");
        }
    }
}
