package com.example.delivery.jca;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionFactory;
import jakarta.resource.cci.Interaction;
import jakarta.resource.cci.MappedRecord;
import jakarta.resource.cci.Record;

@Service
public class Bitrix24EisService {

    private static final Logger log = LoggerFactory.getLogger(Bitrix24EisService.class);

    private final ConnectionFactory connectionFactory;

    public Bitrix24EisService(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public Long createOrder(
            Long localOrderId,
            String title,
            String stageId,
            String customerName,
            String deliveryAddress,
            OffsetDateTime deliveryTime,
            BigDecimal amount,
            String comments) throws Bitrix24IntegrationException {
        MappedRecord<String, Object> request = createRecord("createOrder");
        request.put("localOrderId", localOrderId);
        request.put("title", title);
        request.put("stageId", stageId);
        request.put("customerName", customerName);
        request.put("deliveryAddress", deliveryAddress);
        request.put("deliveryTime", deliveryTime != null ? deliveryTime.toString() : null);
        request.put("amount", amount);
        request.put("comments", comments);

        MappedRecord<?, ?> response = execute(request);
        Object crmItemId = response.get("crmItemId");
        if (!(crmItemId instanceof Number number)) {
            throw new Bitrix24IntegrationException("Bitrix24 JCA adapter did not return crmItemId");
        }
        return number.longValue();
    }

    public void updateOrderStatus(Long crmItemId, String stageId)
            throws Bitrix24IntegrationException {
        MappedRecord<String, Object> request = createRecord("updateOrderStatus");
        request.put("crmItemId", crmItemId);
        request.put("stageId", stageId);
        execute(request);
    }

    private MappedRecord<String, Object> createRecord(String name)
            throws Bitrix24IntegrationException {
        try {
            return connectionFactory.getRecordFactory().createMappedRecord(name);
        } catch (ResourceException | RuntimeException exception) {
            throw new Bitrix24IntegrationException(
                    "Could not create Bitrix24 JCA request", exception);
        }
    }

    private MappedRecord<?, ?> execute(MappedRecord<String, Object> request)
            throws Bitrix24IntegrationException {
        Connection connection = null;
        Interaction interaction = null;
        try {
            connection = connectionFactory.getConnection();
            interaction = connection.createInteraction();
            Record result = interaction.execute(null, request);
            if (!(result instanceof MappedRecord<?, ?> response)) {
                throw new Bitrix24IntegrationException("Bitrix24 JCA adapter returned an invalid response");
            }
            return response;
        } catch (ResourceException | RuntimeException exception) {
            throw new Bitrix24IntegrationException("Bitrix24 EIS request failed", exception);
        } finally {
            closeInteraction(interaction);
            closeConnection(connection);
        }
    }

    private void closeInteraction(Interaction interaction) {
        if (interaction != null) {
            try {
                interaction.close();
            } catch (ResourceException exception) {
                log.warn("Error closing Bitrix24 interaction: {}", exception.getMessage());
            }
        }
    }

    private void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (ResourceException exception) {
                log.warn("Error closing Bitrix24 connection: {}", exception.getMessage());
            }
        }
    }
}
