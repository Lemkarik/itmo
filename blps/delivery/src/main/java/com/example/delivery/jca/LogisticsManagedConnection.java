package com.example.delivery.jca;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionEvent;
import jakarta.resource.spi.ConnectionEventListener;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.LocalTransaction;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;


public class LogisticsManagedConnection implements ManagedConnection {

    private static final Logger log = LoggerFactory.getLogger(LogisticsManagedConnection.class);

    private final List<ConnectionEventListener> listeners = new ArrayList<>();
    private final LogisticsConnectionImpl connection;
    private boolean destroyed = false;

    private final String endpointUrl;
    private final String apiKey;

    public LogisticsManagedConnection(String endpointUrl, String apiKey) {
        this.endpointUrl = endpointUrl;
        this.apiKey = apiKey;
        this.connection = new LogisticsConnectionImpl(this, endpointUrl, apiKey);
        log.debug("LogisticsManagedConnection created");
    }

    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo cxRequestInfo)
            throws ResourceException {
        return connection;
    }

    @Override
    public void destroy() throws ResourceException {
        destroyed = true;
        connection.close();
        log.debug("LogisticsManagedConnection destroyed");
    }

    @Override
    public void cleanup() throws ResourceException {
        
        log.debug("LogisticsManagedConnection cleaned up");
    }

    @Override
    public void associateConnection(Object connection) throws ResourceException {
        if (connection instanceof LogisticsConnectionImpl logisticsConn) {
            log.debug("Associating connection");
        }
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        
        return null;
    }

    @Override
    public javax.transaction.xa.XAResource getXAResource() throws ResourceException {
        
        return null;
    }

    @Override
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        return new ManagedConnectionMetaData() {
            @Override
            public String getEISProductName() throws ResourceException {
                return "Yandex Maps Geocoder API";
            }

            @Override
            public String getEISProductVersion() throws ResourceException {
                return "v1";
            }

            @Override
            public int getMaxConnections() throws ResourceException {
                return 10;
            }

            @Override
            public String getUserName() throws ResourceException {
                return apiKey != null && !apiKey.isBlank() ? "yandex-api-key" : "anonymous";
            }
        };
    }

    @Override
    public void setLogWriter(java.io.PrintWriter out) throws ResourceException {
        
    }

    @Override
    public java.io.PrintWriter getLogWriter() throws ResourceException {
        return null;
    }

    
    public void connectionClosed(LogisticsConnectionImpl closedConnection) {
        ConnectionEvent event = new ConnectionEvent(
                this, ConnectionEvent.CONNECTION_CLOSED);
        event.setConnectionHandle(closedConnection);

        for (ConnectionEventListener listener : listeners) {
            listener.connectionClosed(event);
        }
    }
}
