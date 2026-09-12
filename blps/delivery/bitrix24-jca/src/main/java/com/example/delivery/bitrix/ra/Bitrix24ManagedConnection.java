package com.example.delivery.bitrix.ra;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionEvent;
import jakarta.resource.spi.ConnectionEventListener;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.LocalTransaction;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionMetaData;

final class Bitrix24ManagedConnection implements ManagedConnection {

    private final String webhookUrl;
    private final Integer entityTypeId;
    private final Integer categoryId;
    private final String currencyId;
    private final Bitrix24Client client;
    private final List<ConnectionEventListener> listeners = new ArrayList<>();
    private final Set<Bitrix24Connection> handles =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private PrintWriter logWriter;
    private boolean destroyed;

    Bitrix24ManagedConnection(
            String webhookUrl,
            Integer entityTypeId,
            Integer categoryId,
            String currencyId) {
        this.webhookUrl = webhookUrl;
        this.entityTypeId = entityTypeId;
        this.categoryId = categoryId;
        this.currencyId = currencyId;
        this.client = new Bitrix24Client(webhookUrl, entityTypeId, categoryId, currencyId);
    }

    @Override
    public synchronized Object getConnection(Subject subject, ConnectionRequestInfo requestInfo)
            throws ResourceException {
        ensureOpen();
        Bitrix24Connection connection = new Bitrix24Connection(this);
        handles.add(connection);
        return connection;
    }

    @Override
    public synchronized void destroy() {
        destroyed = true;
        invalidateHandles();
    }

    @Override
    public synchronized void cleanup() throws ResourceException {
        ensureOpen();
        invalidateHandles();
    }

    @Override
    public synchronized void associateConnection(Object connection) throws ResourceException {
        ensureOpen();
        if (!(connection instanceof Bitrix24Connection handle)) {
            throw new ResourceException("Unsupported connection handle type");
        }
        handle.associate(this);
        handles.add(handle);
    }

    @Override
    public synchronized void addConnectionEventListener(ConnectionEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public synchronized void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public XAResource getXAResource() throws ResourceException {
        throw new NotSupportedException("Bitrix24 adapter does not support XA transactions");
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException("Bitrix24 adapter does not support local transactions");
    }

    @Override
    public ManagedConnectionMetaData getMetaData() {
        return new Bitrix24ManagedConnectionMetaData();
    }

    @Override
    public synchronized void setLogWriter(PrintWriter logWriter) {
        this.logWriter = logWriter;
    }

    @Override
    public synchronized PrintWriter getLogWriter() {
        return logWriter;
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
        return client.createOrder(
                localOrderId, title, stageId, customerName,
                deliveryAddress, deliveryTime, amount, comments);
    }

    synchronized void updateOrderStatus(long crmItemId, String stageId)
            throws ResourceException {
        ensureOpen();
        client.updateOrderStatus(crmItemId, stageId);
    }

    synchronized void connectionClosed(Bitrix24Connection connection) {
        if (!handles.remove(connection)) {
            return;
        }
        ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        event.setConnectionHandle(connection);
        for (ConnectionEventListener listener : List.copyOf(listeners)) {
            listener.connectionClosed(event);
        }
    }

    synchronized boolean matches(
            String expectedWebhookUrl,
            Integer expectedEntityTypeId,
            Integer expectedCategoryId,
            String expectedCurrencyId) {
        return !destroyed
                && Objects.equals(webhookUrl, expectedWebhookUrl)
                && Objects.equals(entityTypeId, expectedEntityTypeId)
                && Objects.equals(categoryId, expectedCategoryId)
                && Objects.equals(currencyId, expectedCurrencyId);
    }

    private void ensureOpen() throws ResourceException {
        if (destroyed) {
            throw new ResourceException("Managed connection is destroyed");
        }
    }

    private void invalidateHandles() {
        for (Bitrix24Connection handle : List.copyOf(handles)) {
            handle.invalidate();
        }
        handles.clear();
    }
}
