package com.rally.participation.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Transactional outbox row. Written in the SAME database transaction as the
 * participations insert/update that caused it, so publication survives crashes
 * between "DB commit" and "Kafka publish" (see docs §8.2).
 * A separate poller (OutboxPoller) reads unpublished rows and pushes them to Kafka.
 */
@Entity
@Table(name = "participation_outbox")
public class ParticipationOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "published_at")
    private Instant publishedAt;

    protected ParticipationOutbox() {
        // JPA
    }

    public ParticipationOutbox(UUID aggregateId, String eventType, String payload) {
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public void markPublished() {
        this.publishedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}
