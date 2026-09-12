package com.example.delivery.jca;

public class Bitrix24IntegrationException extends Exception {

    public Bitrix24IntegrationException(String message) {
        super(message);
    }

    public Bitrix24IntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
