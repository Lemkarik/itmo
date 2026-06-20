package com.example.delivery.jca;

import javax.naming.Reference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionFactory;
import jakarta.resource.cci.ConnectionSpec;
import jakarta.resource.cci.RecordFactory;
import jakarta.resource.cci.ResourceAdapterMetaData;
import jakarta.resource.spi.ConnectionManager;

/**
 * JCA ConnectionFactory for the Logistics EIS.
 * This is the client-facing factory that application code uses
 * to obtain connections to the external logistics system.
 * 
 * Implements jakarta.resource.cci.ConnectionFactory for CCI-style access.
 */
public class LogisticsConnectionFactory implements ConnectionFactory {

    private static final Logger log = LoggerFactory.getLogger(LogisticsConnectionFactory.class);

    private final LogisticsManagedConnectionFactory managedConnectionFactory;
    private final ConnectionManager connectionManager;
    private Reference reference;

    public LogisticsConnectionFactory(
            LogisticsManagedConnectionFactory managedConnectionFactory,
            ConnectionManager connectionManager) {
        this.managedConnectionFactory = managedConnectionFactory;
        this.connectionManager = connectionManager;
    }

    @Override
    public Connection getConnection() throws ResourceException {
        if (connectionManager == null) {
            log.debug("Creating connection directly (no ConnectionManager)");
            LogisticsManagedConnection mc = (LogisticsManagedConnection)
                    managedConnectionFactory.createManagedConnection(null, null);
            return (Connection) mc.getConnection(null, null);
        }

        log.debug("Getting connection via ConnectionManager (pooled)");
        return (Connection) connectionManager.allocateConnection(
                managedConnectionFactory, null);
    }

    @Override
    public Connection getConnection(ConnectionSpec connectionSpec)
            throws ResourceException {
        return getConnection();
    }

    @Override
    public RecordFactory getRecordFactory() throws ResourceException {
        // Not using CCI records - using direct Java interfaces
        return null;
    }

    @Override
    public ResourceAdapterMetaData getMetaData() throws ResourceException {
        return new ResourceAdapterMetaData() {
            @Override
            public String getAdapterVersion() {
                return "1.0";
            }

            @Override
            public String getAdapterName() {
                return "LogisticsEISAdapter";
            }

            @Override
            public String getAdapterVendorName() {
                return "Delivery Service";
            }

            @Override
            public String getAdapterShortDescription() {
                return "JCA Resource Adapter for External Logistics EIS";
            }

            @Override
            public String[] getInteractionSpecsSupported() {
                return new String[0];
            }

            @Override
            public boolean supportsExecuteWithInputAndOutputRecord() {
                return false;
            }

            @Override
            public boolean supportsExecuteWithInputRecordOnly() {
                return false;
            }

            @Override
            public boolean supportsLocalTransactionDemarcation() {
                return false;
            }

            @Override
            public String getSpecVersion() {
                return "2.1";
            }
        };
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
