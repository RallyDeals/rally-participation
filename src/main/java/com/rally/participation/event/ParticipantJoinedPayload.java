package com.rally.participation.event;

import java.time.Instant;
import java.util.UUID;

public record ParticipantJoinedPayload(
    UUID eventId,
    UUID participationId,
    UUID dealId,
    UUID userId,
    UUID referredBy,
    Instant joinedAt
) {}
