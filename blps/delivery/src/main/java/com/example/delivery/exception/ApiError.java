package com.example.delivery.exception;

import java.time.OffsetDateTime;

public class ApiError {
	private OffsetDateTime timestamp;
	private int status;
	private String error;
	private String message;

	public ApiError(OffsetDateTime timestamp, int status, String error, String message) {
		this.timestamp = timestamp;
		this.status = status;
		this.error = error;
		this.message = message;
	}

	public OffsetDateTime getTimestamp() {
		return timestamp;
	}

	public int getStatus() {
		return status;
	}

	public String getError() {
		return error;
	}

	public String getMessage() {
		return message;
	}
}
