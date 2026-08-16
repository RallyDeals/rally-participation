package com.rally.participation.dto;

import com.rally.participation.domain.Participation;

import java.time.Instant;
import java.util.UUID;

public record ParticipantSummary(
    UUID userId,
    UUID referredBy,
    Instant joinedAt
) {
    public static ParticipantSummary from(Participation p) {
        return new ParticipantSummary(p.getUserId(), p.getReferredBy(), p.getJoinedAt());
    }
}
