package com.example.delivery.jca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

/**
 * JCA ResourceAdapter for the Logistics EIS (Enterprise Information System).
 * 
 * This resource adapter enables integration with an external logistics provider
 * through the Jakarta Connectors (JCA) architecture.
 * 
 * In the context of this lab, it simulates communication with an external
 * logistics system that processes delivery orders, tracks shipments,
 * and provides status updates.
 */
public class LogisticsResourceAdapter implements ResourceAdapter {

    private static final Logger log = LoggerFactory.getLogger(LogisticsResourceAdapter.class);

    private volatile boolean started = false;
    private BootstrapContext bootstrapContext;

    @Override
    public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
        this.bootstrapContext = ctx;
        this.started = true;
        log.info("Logistics ResourceAdapter started");
        log.info("Using XATerminator: {}", ctx.getXATerminator());
    }

    @Override
    public void stop() {
        this.started = false;
        log.info("Logistics ResourceAdapter stopped");
    }

    @Override
    public void endpointActivation(
            MessageEndpointFactory endpointFactory,
            ActivationSpec spec) throws ResourceException {
        log.info("Endpoint activation for: {}", spec);
    }

    @Override
    public void endpointDeactivation(
            MessageEndpointFactory endpointFactory,
            ActivationSpec spec) {
        log.info("Endpoint deactivation for: {}", spec);
    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] specs)
            throws ResourceException {
        // This resource adapter does not provide XA resources directly
        // (transactions are handled at the JMS/JPA level)
        return new XAResource[0];
    }

    public boolean isStarted() {
        return started;
    }

    public BootstrapContext getBootstrapContext() {
        return bootstrapContext;
    }
}
