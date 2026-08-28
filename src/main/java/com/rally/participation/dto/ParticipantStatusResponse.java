package com.rally.participation.dto;

import com.rally.participation.domain.ParticipationStatus;

public record ParticipantStatusResponse(String status) {
    public static ParticipantStatusResponse from(ParticipationStatus status) {
        return new ParticipantStatusResponse(status.name());
    }
}
