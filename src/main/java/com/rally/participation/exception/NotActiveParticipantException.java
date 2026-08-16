package com.rally.participation.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class NotActiveParticipantException extends ParticipationApiException {
    public NotActiveParticipantException(UUID dealId, UUID userId) {
        super(HttpStatus.NOT_FOUND, "User " + userId + " has no active participation for deal " + dealId);
    }
}
