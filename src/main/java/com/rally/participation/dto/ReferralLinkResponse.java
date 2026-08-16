package com.rally.participation.dto;

import com.rally.participation.domain.ReferralLink;

import java.time.Instant;
import java.util.UUID;

public record ReferralLinkResponse(
    String code,
    UUID dealId,
    UUID referrerUserId,
    Instant expiresAt
) {
    public static ReferralLinkResponse from(ReferralLink link) {
        return new ReferralLinkResponse(link.getCode(), link.getDealId(), link.getReferrerUserId(), link.getExpiresAt());
    }
}
