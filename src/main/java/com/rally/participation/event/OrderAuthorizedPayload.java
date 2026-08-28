package com.rally.participation.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderAuthorizedPayload(UUID orderId, UUID dealId, UUID userId, BigDecimal totalPrice) {
}
