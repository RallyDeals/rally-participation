package com.rally.participation.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "participations")
public class Participation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "deal_id", nullable = false)
    private UUID dealId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "referred_by")
    private UUID referredBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private ParticipationStatus status = ParticipationStatus.ACTIVE;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();

    @Column(name = "left_at")
    private Instant leftAt;

    protected Participation() {
        // JPA
    }

    public Participation(UUID dealId, UUID userId, UUID referredBy) {
        this.dealId = dealId;
        this.userId = userId;
        this.referredBy = referredBy;
        this.status = ParticipationStatus.PENDING;
        this.joinedAt = Instant.now();
    }

    public void markLeft() {
        this.status = ParticipationStatus.LEFT;
        this.leftAt = Instant.now();
    }

    public boolean isActive() {
        return status == ParticipationStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDealId() {
        return dealId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getReferredBy() {
        return referredBy;
    }

    public ParticipationStatus getStatus() {
        return status;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public Instant getLeftAt() {
        return leftAt;
    }
}
