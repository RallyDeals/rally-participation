package com.rally.participation.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class AlreadyActiveParticipantException extends ParticipationApiException {
    public AlreadyActiveParticipantException(UUID dealId, UUID userId) {
        super(HttpStatus.CONFLICT, "User " + userId + " already has an active participation for deal " + dealId);
    }
}
