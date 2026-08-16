package com.rally.participation.event;

import java.time.Instant;
import java.util.UUID;

public record ParticipantLeftPayload(
    UUID eventId,
    UUID participationId,
    UUID dealId,
    UUID userId,
    Instant leftAt,
    String reason // currently always SELF_INITIATED - see docs §5.2
) {}
