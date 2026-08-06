package com.example.delivery.jca;

import java.io.PrintWriter;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterAssociation;
import javax.security.auth.Subject;


public class LogisticsManagedConnectionFactory
        implements ManagedConnectionFactory, ResourceAdapterAssociation {

    private static final Logger log = LoggerFactory.getLogger(LogisticsManagedConnectionFactory.class);

    private ResourceAdapter resourceAdapter;
    private PrintWriter logWriter;
    private String logisticsEndpointUrl = "https://geocode-maps.yandex.ru/v1/";
    private String apiKey = "";

    @Override
    public Object createConnectionFactory(ConnectionManager cxManager)
            throws ResourceException {
        log.debug("Creating connection factory with ConnectionManager");
        return new LogisticsConnectionFactory(this, cxManager);
    }

    @Override
    public Object createConnectionFactory() throws ResourceException {
        log.debug("Creating connection factory without ConnectionManager");
        return new LogisticsConnectionFactory(this, null);
    }

    @Override
    public ManagedConnection createManagedConnection(
            Subject subject, ConnectionRequestInfo cxRequestInfo)
            throws ResourceException {
        log.debug("Creating new LogisticsManagedConnection");
        return new LogisticsManagedConnection(logisticsEndpointUrl, apiKey);
    }

    @Override
    public ManagedConnection matchManagedConnections(
            @SuppressWarnings("rawtypes") Set connectionSet,
            Subject subject, ConnectionRequestInfo cxRequestInfo)
            throws ResourceException {
        log.debug("Matching managed connections (pool size: {})",
                connectionSet != null ? connectionSet.size() : 0);

        if (connectionSet != null) {
            for (Object obj : connectionSet) {
                if (obj instanceof LogisticsManagedConnection) {
                    return (LogisticsManagedConnection) obj;
                }
            }
        }
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws ResourceException {
        this.logWriter = out;
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return logWriter;
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return resourceAdapter;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter resourceAdapter)
            throws ResourceException {
        this.resourceAdapter = resourceAdapter;
    }

    

    public String getLogisticsEndpointUrl() {
        return logisticsEndpointUrl;
    }

    public void setLogisticsEndpointUrl(String logisticsEndpointUrl) {
        this.logisticsEndpointUrl = logisticsEndpointUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LogisticsManagedConnectionFactory that = (LogisticsManagedConnectionFactory) obj;
        return logisticsEndpointUrl.equals(that.logisticsEndpointUrl);
    }

    @Override
    public int hashCode() {
        return logisticsEndpointUrl.hashCode();
    }
}
