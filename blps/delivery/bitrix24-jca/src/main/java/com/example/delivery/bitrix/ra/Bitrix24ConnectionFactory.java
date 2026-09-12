package com.example.delivery.bitrix.ra;

import java.io.Serial;

import javax.naming.Reference;

import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionFactory;
import jakarta.resource.cci.ConnectionSpec;
import jakarta.resource.cci.RecordFactory;
import jakarta.resource.cci.ResourceAdapterMetaData;
import jakarta.resource.spi.ConnectionManager;

public final class Bitrix24ConnectionFactory implements ConnectionFactory {

    @Serial
    private static final long serialVersionUID = 1L;

    private Bitrix24ManagedConnectionFactory managedConnectionFactory;
    private ConnectionManager connectionManager;
    private Reference reference;

    public Bitrix24ConnectionFactory() {
    }

    Bitrix24ConnectionFactory(
            Bitrix24ManagedConnectionFactory managedConnectionFactory,
            ConnectionManager connectionManager) {
        this.managedConnectionFactory = managedConnectionFactory;
        this.connectionManager = connectionManager;
    }

    @Override
    public Connection getConnection() throws ResourceException {
        if (managedConnectionFactory == null || connectionManager == null) {
            throw new ResourceException("Connection factory is not initialized");
        }
        return (Connection) connectionManager.allocateConnection(managedConnectionFactory, null);
    }

    @Override
    public Connection getConnection(ConnectionSpec connectionSpec) throws ResourceException {
        return getConnection();
    }

    @Override
    public RecordFactory getRecordFactory() {
        return new Bitrix24RecordFactory();
    }

    @Override
    public ResourceAdapterMetaData getMetaData() {
        return new Bitrix24ResourceAdapterMetaData();
    }

    @Override
    public void setReference(Reference reference) {
        this.reference = reference;
    }

    @Override
    public Reference getReference() {
        return reference;
    }
}
