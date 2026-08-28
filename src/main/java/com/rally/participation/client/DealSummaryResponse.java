package com.rally.participation.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DealSummaryResponse(
    UUID dealId,
    UUID productId,
    String status,
    Integer minParticipants,
    Integer stockCap,
    Integer reservedCount,
    BigDecimal dealPrice,
    Instant endTime
) {}
