package com.example.delivery.bitrix.ra;

import java.io.PrintWriter;
import java.io.Serial;
import java.util.Objects;
import java.util.Set;

import javax.security.auth.Subject;

import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterAssociation;

public final class Bitrix24ManagedConnectionFactory
        implements ManagedConnectionFactory, ResourceAdapterAssociation {

    @Serial
    private static final long serialVersionUID = 1L;

    private String webhookUrl = "";
    private Integer entityTypeId = 2;
    private Integer categoryId = 1;
    private String currencyId = "USD";
    private transient PrintWriter logWriter;
    private transient ResourceAdapter resourceAdapter;

    @Override
    public Object createConnectionFactory(ConnectionManager connectionManager) {
        return new Bitrix24ConnectionFactory(this, connectionManager);
    }

    @Override
    public Object createConnectionFactory() throws ResourceException {
        throw new NotSupportedException(
                "Bitrix24 adapter requires a Jakarta Connectors container");
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo requestInfo) {
        return new Bitrix24ManagedConnection(
                webhookUrl, entityTypeId, categoryId, currencyId);
    }

    @Override
    public ManagedConnection matchManagedConnections(
            @SuppressWarnings("rawtypes") Set connectionSet,
            Subject subject,
            ConnectionRequestInfo requestInfo) {
        if (connectionSet == null) {
            return null;
        }
        for (Object candidate : connectionSet) {
            if (candidate instanceof Bitrix24ManagedConnection connection
                    && connection.matches(webhookUrl, entityTypeId, categoryId, currencyId)) {
                return connection;
            }
        }
        return null;
    }

    @Override
    public PrintWriter getLogWriter() {
        return logWriter;
    }

    @Override
    public void setLogWriter(PrintWriter logWriter) {
        this.logWriter = logWriter;
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return resourceAdapter;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter resourceAdapter) throws ResourceException {
        if (!(resourceAdapter instanceof Bitrix24ResourceAdapter)) {
            throw new ResourceException("Unsupported resource adapter type");
        }
        this.resourceAdapter = resourceAdapter;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public Integer getEntityTypeId() {
        return entityTypeId;
    }

    public void setEntityTypeId(Integer entityTypeId) {
        this.entityTypeId = entityTypeId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(String currencyId) {
        this.currencyId = currencyId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Bitrix24ManagedConnectionFactory factory)) {
            return false;
        }
        return Objects.equals(webhookUrl, factory.webhookUrl)
                && Objects.equals(entityTypeId, factory.entityTypeId)
                && Objects.equals(categoryId, factory.categoryId)
                && Objects.equals(currencyId, factory.currencyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(webhookUrl, entityTypeId, categoryId, currencyId);
    }
}
