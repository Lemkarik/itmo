package com.example.delivery.bitrix.ra;

import java.math.BigDecimal;

import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.Interaction;
import jakarta.resource.cci.InteractionSpec;
import jakarta.resource.cci.MappedRecord;
import jakarta.resource.cci.Record;
import jakarta.resource.cci.ResourceWarning;

final class Bitrix24Interaction implements Interaction {

    private Bitrix24Connection connection;

    Bitrix24Interaction(Bitrix24Connection connection) {
        this.connection = connection;
    }

    @Override
    public void close() {
        connection = null;
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public boolean execute(InteractionSpec specification, Record input, Record output)
            throws ResourceException {
        throw new NotSupportedException(
                "Execution with a caller-provided output record is not supported");
    }

    @Override
    public Record execute(InteractionSpec specification, Record input) throws ResourceException {
        Bitrix24Connection activeConnection = connection;
        if (activeConnection == null) {
            throw new ResourceException("Interaction is closed");
        }
        activeConnection.ensureOpen();
        if (!(input instanceof MappedRecord<?, ?> request)) {
            throw new ResourceException("Input must be a MappedRecord");
        }

        return switch (input.getRecordName()) {
            case "createOrder" -> createOrder(activeConnection, request);
            case "updateOrderStatus" -> updateOrderStatus(activeConnection, request);
            default -> throw new ResourceException(
                    "Unsupported Bitrix24 operation: " + input.getRecordName());
        };
    }

    private Record createOrder(Bitrix24Connection activeConnection, MappedRecord<?, ?> request)
            throws ResourceException {
        long crmItemId = activeConnection.createOrder(
                requiredLong(request, "localOrderId"),
                requiredString(request, "title"),
                requiredString(request, "stageId"),
                optionalString(request, "customerName"),
                optionalString(request, "deliveryAddress"),
                optionalString(request, "deliveryTime"),
                optionalDecimal(request, "amount"),
                optionalString(request, "comments"));
        MappedRecord<String, Object> response = new Bitrix24MappedRecord<>("createOrderResponse");
        response.put("crmItemId", crmItemId);
        return response;
    }

    private Record updateOrderStatus(
            Bitrix24Connection activeConnection,
            MappedRecord<?, ?> request) throws ResourceException {
        long crmItemId = requiredLong(request, "crmItemId");
        activeConnection.updateOrderStatus(
                crmItemId,
                requiredString(request, "stageId"));
        MappedRecord<String, Object> response =
                new Bitrix24MappedRecord<>("updateOrderStatusResponse");
        response.put("crmItemId", crmItemId);
        return response;
    }

    @Override
    public ResourceWarning getWarnings() {
        return null;
    }

    @Override
    public void clearWarnings() {
    }

    private String requiredString(MappedRecord<?, ?> request, String name)
            throws ResourceException {
        String value = optionalString(request, name);
        if (value == null || value.isBlank()) {
            throw new ResourceException(name + " is required");
        }
        return value;
    }

    private String optionalString(MappedRecord<?, ?> request, String name)
            throws ResourceException {
        Object value = request.get(name);
        if (value == null) {
            return null;
        }
        if (!(value instanceof String text)) {
            throw new ResourceException(name + " must be a string");
        }
        return text;
    }

    private long requiredLong(MappedRecord<?, ?> request, String name)
            throws ResourceException {
        Object value = request.get(name);
        if (!(value instanceof Number number)) {
            throw new ResourceException(name + " must be a number");
        }
        return number.longValue();
    }

    private BigDecimal optionalDecimal(MappedRecord<?, ?> request, String name)
            throws ResourceException {
        Object value = request.get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        throw new ResourceException(name + " must be a number");
    }
}
