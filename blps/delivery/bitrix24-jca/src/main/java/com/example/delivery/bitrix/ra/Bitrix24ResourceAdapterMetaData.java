package com.example.delivery.bitrix.ra;

import jakarta.resource.cci.ConnectionMetaData;
import jakarta.resource.cci.ResourceAdapterMetaData;
import jakarta.resource.spi.ManagedConnectionMetaData;

final class Bitrix24ResourceAdapterMetaData implements ResourceAdapterMetaData {

    @Override
    public String getAdapterVersion() {
        return "1.0.0";
    }

    @Override
    public String getAdapterVendorName() {
        return "Delivery Service";
    }

    @Override
    public String getAdapterName() {
        return "Bitrix24 CRM JCA Resource Adapter";
    }

    @Override
    public String getAdapterShortDescription() {
        return "Outbound JCA adapter for Bitrix24 CRM REST API";
    }

    @Override
    public String getSpecVersion() {
        return "2.1";
    }

    @Override
    public String[] getInteractionSpecsSupported() {
        return new String[0];
    }

    @Override
    public boolean supportsExecuteWithInputAndOutputRecord() {
        return true;
    }

    @Override
    public boolean supportsExecuteWithInputRecordOnly() {
        return true;
    }

    @Override
    public boolean supportsLocalTransactionDemarcation() {
        return false;
    }
}

final class Bitrix24ConnectionMetaData implements ConnectionMetaData {

    @Override
    public String getEISProductName() {
        return "Bitrix24 CRM REST API";
    }

    @Override
    public String getEISProductVersion() {
        return "crm.item";
    }

    @Override
    public String getUserName() {
        return "webhook-user";
    }
}

final class Bitrix24ManagedConnectionMetaData implements ManagedConnectionMetaData {

    @Override
    public String getEISProductName() {
        return "Bitrix24 CRM REST API";
    }

    @Override
    public String getEISProductVersion() {
        return "crm.item";
    }

    @Override
    public int getMaxConnections() {
        return 10;
    }

    @Override
    public String getUserName() {
        return "webhook-user";
    }
}
