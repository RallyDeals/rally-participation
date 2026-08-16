package com.rally.participation.exception;

import org.springframework.http.HttpStatus;

public abstract class ParticipationApiException extends RuntimeException {
    private final HttpStatus status;

    protected ParticipationApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
