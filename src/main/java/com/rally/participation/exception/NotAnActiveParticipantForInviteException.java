package com.rally.participation.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class NotAnActiveParticipantForInviteException extends ParticipationApiException {
    public NotAnActiveParticipantForInviteException(UUID dealId, UUID userId) {
        super(HttpStatus.FORBIDDEN, "User " + userId + " must be an active participant of deal " + dealId + " to generate an invite link");
    }
}
