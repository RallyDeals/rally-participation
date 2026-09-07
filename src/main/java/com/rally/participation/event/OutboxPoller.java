package com.rally.participation.event;

import com.rally.participation.config.KafkaTopicsProperties;
import com.rally.participation.domain.ParticipationOutbox;
import com.rally.participation.repository.ParticipationOutboxRepository;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * Polls participation_outbox for unpublished rows and pushes them to Kafka, then marks
 * them published. Decouples "the DB write committed" from "Kafka publish succeeded" so a
 * crash between those two steps just means the poller catches up on next run instead of
 * silently losing the event (docs §8.1, §8.2).
 *
 * This is the "relay" from OBSERVABILITY_GUIDE.md §10: it runs on a scheduled thread with
 * no active trace, so without re-parenting, every publish would start a brand-new,
 * disconnected trace. Instead it re-parents a span onto the traceId persisted on the row
 * (by OutboxEventWriter, at the time the row was created) before publishing, so the
 * publish - and everything downstream that consumes it - stays on the original trace.
 */
@Component
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);
    private static final Pattern OTLP_TRACE_ID_PATTERN = Pattern.compile("^[0-9a-fA-F]{32}$");

    private final ParticipationOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicsProperties topics;
    private final Tracer tracer;

    @Value("${rally.outbox.batch-size:100}")
    private int batchSize;

    public OutboxPoller(ParticipationOutboxRepository outboxRepository,
                         KafkaTemplate<String, String> kafkaTemplate,
                         KafkaTopicsProperties topics,
                         Tracer tracer) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
        this.tracer = tracer;
    }

    @Scheduled(fixedDelayString = "${rally.outbox.poll-interval-ms:1000}")
    @Transactional
    public void publishPending() {
        List<ParticipationOutbox> pending =
            outboxRepository.findByPublishedAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, batchSize));

        for (ParticipationOutbox row : pending) {
            Span span = reParentToStoredTrace(row);
            try (Tracer.SpanInScope ignored = span != null ? tracer.withSpan(span) : null) {
                publishOne(row);
            } catch (Exception e) {
                log.error("Failed to publish outbox row id={} eventType={} - will retry next poll",
                    row.getId(), row.getEventType(), e);
                // leave unpublished; next poll retries. Delivery is at-least-once by design,
                // so downstream consumers (Order Service etc.) must be idempotent.
            } finally {
                if (span != null) {
                    span.end();
                }
            }
        }
    }

    private void publishOne(ParticipationOutbox row) throws Exception {
        String topic = topics.getParticipation();
        String key = row.getAggregateId().toString();
        String eventType = row.getEventType();

        String correlationId = row.getCorrelationId() != null
            ? row.getCorrelationId().toString()
            : row.getAggregateId().toString(); // fallback for rows written before V5 added the column

        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, row.getPayload());
        record.headers().add(new RecordHeader(MessageHeaders.ID, row.getId().toString().getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader(MessageHeaders.TYPE, eventType.getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader(MessageHeaders.CORRELATION_ID, correlationId.getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader(MessageHeaders.CAUSATION_ID, row.getAggregateId().toString().getBytes(StandardCharsets.UTF_8)));
        if (row.getTraceId() != null) {
            record.headers().add(new RecordHeader(MessageHeaders.TRACE_ID, row.getTraceId().getBytes(StandardCharsets.UTF_8)));
        }
        // No manual traceparent header here - spring.kafka.template.observation-enabled=true
        // makes this send derive and inject it from the current span (the re-parented one,
        // if we're inside its scope) automatically.

        kafkaTemplate.send(record).get();
        row.markPublished();
    }

    private Span reParentToStoredTrace(ParticipationOutbox row) {
        String traceId = row.getTraceId();
        if (traceId == null || !OTLP_TRACE_ID_PATTERN.matcher(traceId).matches()) {
            return null;
        }
        return tracer.spanBuilder()
            .name("relay-outbox-message")
            .setParent(tracer.traceContextBuilder()
                .traceId(traceId)
                .spanId(randomValidSpanId())
                .sampled(true)
                .build())
            .start();
    }

    private static String randomValidSpanId() {
        return String.format("%016x", ThreadLocalRandom.current().nextLong());
    }
}
