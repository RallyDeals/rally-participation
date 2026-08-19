package com.rally.participation.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ParticipantJoinedPayload(
    UUID participantId,
    UUID dealId,
    UUID userId,
    UUID productId,
    BigDecimal price,
    String paymentMethodId,
    String address,
    Instant joinedAt
) {}
