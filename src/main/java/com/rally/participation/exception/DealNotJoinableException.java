package com.rally.participation.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class DealNotJoinableException extends ParticipationApiException {
    public DealNotJoinableException(UUID dealId, String reason) {
        super(HttpStatus.CONFLICT, "Deal " + dealId + " is not joinable: " + reason);
    }
}
