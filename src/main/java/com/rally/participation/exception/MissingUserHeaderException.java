package com.rally.participation.exception;

public class MissingUserHeaderException extends RuntimeException {
    public MissingUserHeaderException() {
        super("Missing or invalid X-User-Id header - expected to be set by the API Gateway after JWT validation");
    }
}
