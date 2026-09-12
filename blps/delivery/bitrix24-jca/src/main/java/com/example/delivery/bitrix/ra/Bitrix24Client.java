package com.example.delivery.bitrix.ra;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.resource.ResourceException;

final class Bitrix24Client {

    private static final String ORIGINATOR_ID = "delivery-service";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String webhookUrl;
    private final int entityTypeId;
    private final int categoryId;
    private final String currencyId;

    Bitrix24Client(
            String webhookUrl,
            Integer entityTypeId,
            Integer categoryId,
            String currencyId) {
        this.webhookUrl = normalizeWebhookUrl(webhookUrl);
        this.entityTypeId = entityTypeId != null ? entityTypeId : 2;
        this.categoryId = categoryId != null ? categoryId : 1;
        this.currencyId = currencyId == null || currencyId.isBlank() ? "USD" : currencyId;
    }

    long createOrder(
            long localOrderId,
            String title,
            String stageId,
            String customerName,
            String deliveryAddress,
            String deliveryTime,
            BigDecimal amount,
            String comments) throws ResourceException {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("title", title);
        fields.put("categoryId", categoryId);
        fields.put("stageId", stageId);
        fields.put("originatorId", ORIGINATOR_ID);
        fields.put("originId", Long.toString(localOrderId));
        fields.put("opened", "Y");
        fields.put("comments", comments);
        fields.put("additionalInfo", additionalInfo(customerName, deliveryAddress));
        if (deliveryTime != null && !deliveryTime.isBlank()) {
            fields.put("closedate", deliveryTime);
        }
        if (amount != null) {
            fields.put("opportunity", amount);
            fields.put("currencyId", currencyId);
            fields.put("isManualOpportunity", "Y");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("entityTypeId", entityTypeId);
        payload.put("fields", fields);
        JsonNode response = post("crm.item.add", payload);
        JsonNode id = response.at("/result/item/id");
        if (!id.canConvertToLong()) {
            throw new ResourceException("Bitrix24 crm.item.add did not return item.id");
        }
        return id.longValue();
    }

    void updateOrderStatus(long crmItemId, String stageId) throws ResourceException {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("categoryId", categoryId);
        fields.put("stageId", stageId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("entityTypeId", entityTypeId);
        payload.put("id", crmItemId);
        payload.put("fields", fields);
        post("crm.item.update", payload);
    }

    private JsonNode post(String method, Map<String, Object> payload) throws ResourceException {
        if (webhookUrl == null) {
            throw new ResourceException("Bitrix24 webhook URL is not configured");
        }
        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl + method))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResourceException(
                        "Bitrix24 " + method + " returned HTTP " + response.statusCode());
            }
            JsonNode json = objectMapper.readTree(response.body());
            if (json.hasNonNull("error")) {
                String description = json.path("error_description").asText(json.path("error").asText());
                throw new ResourceException("Bitrix24 " + method + " failed: " + description);
            }
            return json;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResourceException("Bitrix24 request was interrupted", exception);
        } catch (ResourceException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResourceException("Could not execute Bitrix24 " + method, exception);
        }
    }

    private static String additionalInfo(String customerName, String deliveryAddress) {
        String customer = customerName == null ? "" : customerName;
        String address = deliveryAddress == null ? "" : deliveryAddress;
        return "Customer: " + customer + "; delivery address: " + address;
    }

    private static String normalizeWebhookUrl(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return null;
        }
        String normalized = webhookUrl.trim();
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }
}
