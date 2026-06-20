package com.example.delivery.jca;


public class LogisticsException extends Exception {

    public LogisticsException(String message) {
        super(message);
    }

    public LogisticsException(String message, Throwable cause) {
        super(message, cause);
    }
}
