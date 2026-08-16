package com.rally.participation.dto;

import com.rally.participation.domain.Participation;

import java.time.Instant;
import java.util.UUID;

public record ParticipationResponse(
    UUID id,
    UUID dealId,
    UUID userId,
    UUID referredBy,
    String status,
    Instant joinedAt,
    Instant leftAt
) {
    public static ParticipationResponse from(Participation p) {
        return new ParticipationResponse(
            p.getId(), p.getDealId(), p.getUserId(), p.getReferredBy(),
            p.getStatus().name(), p.getJoinedAt(), p.getLeftAt()
        );
    }
}
