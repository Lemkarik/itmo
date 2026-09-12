package com.example.delivery.bitrix.ra;

import javax.transaction.xa.XAResource;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;

public final class Bitrix24ResourceAdapter implements ResourceAdapter {

    @Override
    public void start(BootstrapContext bootstrapContext) throws ResourceAdapterInternalException {
    }

    @Override
    public void stop() {
    }

    @Override
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec specification)
            throws ResourceException {
        throw new ResourceException("Bitrix24 adapter supports outbound connections only");
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec specification) {
    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] specifications) {
        return new XAResource[0];
    }

}
