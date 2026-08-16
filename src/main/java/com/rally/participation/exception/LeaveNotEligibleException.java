package com.rally.participation.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class LeaveNotEligibleException extends ParticipationApiException {
    public LeaveNotEligibleException(UUID dealId, String reason) {
        super(HttpStatus.CONFLICT, "Deal " + dealId + " is not eligible to leave: " + reason);
    }
}
