package com.rally.participation.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "referral_links")
public class ReferralLink {

    @Id
    @Column(name = "code", length = 16)
    private String code;

    @Column(name = "deal_id", nullable = false)
    private UUID dealId;

    @Column(name = "referrer_user_id", nullable = false)
    private UUID referrerUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected ReferralLink() {
        // JPA
    }

    public ReferralLink(String code, UUID dealId, UUID referrerUserId, Instant expiresAt) {
        this.code = code;
        this.dealId = dealId;
        this.referrerUserId = referrerUserId;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public String getCode() {
        return code;
    }

    public UUID getDealId() {
        return dealId;
    }

    public UUID getReferrerUserId() {
        return referrerUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
