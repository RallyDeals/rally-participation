package com.rally.participation.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ParticipantNotFoundException extends ParticipationApiException {
    public ParticipantNotFoundException(UUID dealId, UUID participantId) {
        super(HttpStatus.NOT_FOUND, "No participation " + participantId + " for deal " + dealId);
    }
}
