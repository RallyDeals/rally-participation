package com.rally.participation.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rally.participation.domain.ParticipationOutbox;
import com.rally.participation.repository.ParticipationOutboxRepository;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Writes outbox rows. Must always be called within the same @Transactional boundary as
 * the participations insert/update it's recording, so both commit or roll back together
 * (docs §8.1 / §8.2). Actual Kafka publication is decoupled - see OutboxPoller.
 *
 * Captures traceId/correlationId from MDC at write time and persists them on the row
 * (OBSERVABILITY_GUIDE.md §9) so the poller can re-parent its publish span onto the trace
 * that created the row, later, from a scheduled thread with no active trace of its own.
 * Both call sites today (join/leave) run on the HTTP request thread, where
 * CorrelationIdFilter has already set X-Correlation-Id in MDC and OTel has already set
 * traceId - if either is missing (e.g. a future caller outside an HTTP request), the
 * column is simply left null and the poller skips re-parenting for that row.
 */
@Component
public class OutboxEventWriter {

    private static final String TRACE_ID_MDC_KEY = "traceId";

    private final ParticipationOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventWriter(ParticipationOutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public void write(UUID aggregateId, String eventType, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            String traceId = MDC.get(TRACE_ID_MDC_KEY);
            UUID correlationId = parseCorrelationId(MDC.get(MessageHeaders.CORRELATION_ID));
            outboxRepository.save(new ParticipationOutbox(aggregateId, eventType, json, traceId, correlationId));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox payload for event " + eventType, e);
        }
    }

    private UUID parseCorrelationId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            // Correlation id isn't always guaranteed to be a UUID from every possible
            // caller, but ours always generates one (CorrelationIdFilter). Don't fail the
            // write over a malformed inbound value - just skip persisting it.
            return null;
        }
    }
}
