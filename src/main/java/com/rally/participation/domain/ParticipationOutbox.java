package com.rally.participation.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Transactional outbox row. Written in the SAME database transaction as the
 * participations insert/update that caused it, so publication survives crashes
 * between "DB commit" and "Kafka publish" (see docs §8.2).
 * A separate poller (OutboxPoller) reads unpublished rows and pushes them to Kafka.
 *
 * traceId/correlationId are captured from MDC at write time (see OutboxEventWriter) and
 * let the poller re-parent its publish span back onto the trace that created the row
 * (OBSERVABILITY_GUIDE.md §9-§10) instead of starting a disconnected new trace.
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

    @Column(name = "trace_id", length = 32)
    private String traceId;

    @Column(name = "correlation_id")
    private UUID correlationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "published_at")
    private Instant publishedAt;

    protected ParticipationOutbox() {
        // JPA
    }

    public ParticipationOutbox(UUID aggregateId, String eventType, String payload, String traceId, UUID correlationId) {
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.traceId = traceId;
        this.correlationId = correlationId;
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

    public String getTraceId() {
        return traceId;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}
