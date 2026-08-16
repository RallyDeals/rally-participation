package com.rally.participation.event;

import java.time.Instant;
import java.util.UUID;


public record OrderDealOrderCancelledPayload(
    UUID orderId,
    UUID dealId,
    UUID userId,
    Instant cancelledAt
) {}
