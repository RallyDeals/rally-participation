package com.rally.participation.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class DealNotFoundException extends ParticipationApiException {
    public DealNotFoundException(UUID dealId) {
        super(HttpStatus.NOT_FOUND, "Deal not found: " + dealId);
    }
}
