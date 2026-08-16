package com.rally.participation.event;

import com.rally.participation.config.KafkaTopicsProperties;
import com.rally.participation.domain.ParticipationOutbox;
import com.rally.participation.repository.ParticipationOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Polls participation_outbox for unpublished rows and pushes them to Kafka, then marks
 * them published. Decouples "the DB write committed" from "Kafka publish succeeded" so a
 * crash between those two steps just means the poller catches up on next run instead of
 * silently losing the event (docs §8.1, §8.2).
 *
 * Keyed by aggregateId (participation id) so Kafka partitioning preserves per-participation
 * ordering.
 */
@Component
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    private final ParticipationOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicsProperties topics;

    @Value("${rally.outbox.batch-size:100}")
    private int batchSize;

    public OutboxPoller(ParticipationOutboxRepository outboxRepository,
                         KafkaTemplate<String, String> kafkaTemplate,
                         KafkaTopicsProperties topics) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
    }

    @Scheduled(fixedDelayString = "${rally.outbox.poll-interval-ms:1000}")
    @Transactional
    public void publishPending() {
        List<ParticipationOutbox> pending =
            outboxRepository.findByPublishedAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, batchSize));

        for (ParticipationOutbox row : pending) {
            String topic = resolveTopic(row.getEventType());
            try {
                kafkaTemplate.send(topic, row.getAggregateId().toString(), row.getPayload()).get();
                row.markPublished();
            } catch (Exception e) {
                log.error("Failed to publish outbox row id={} eventType={} - will retry next poll",
                    row.getId(), row.getEventType(), e);
                // leave unpublished; next poll retries. Delivery is at-least-once by design,
                // so downstream consumers (Order Service etc.) must be idempotent.
            }
        }
    }

    private String resolveTopic(String eventType) {
        return switch (eventType) {
            case EventType.PARTICIPANT_JOINED -> topics.getParticipantJoined();
            case EventType.PARTICIPANT_LEFT -> topics.getParticipantLeft();
            default -> throw new IllegalStateException("Unknown outbox event type: " + eventType);
        };
    }
}
