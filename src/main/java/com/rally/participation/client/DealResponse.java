package com.rally.participation.client;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DealResponse(
    UUID id,
    UUID productId,
    UUID sellerId,
    BigDecimal originalPrice,
    BigDecimal dealPrice,
    Integer dealStock,
    Integer currentParticipants,
    Integer authorizedCount,
    Integer minParticipants,
    String status,
    OffsetDateTime startTime,
    Integer durationMinutes,
    OffsetDateTime endTime,
    Long timeRemainingSeconds,
    OffsetDateTime createdAt
) {}
