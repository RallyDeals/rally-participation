package com.rally.participation.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rally.participation.domain.ParticipationOutbox;
import com.rally.participation.repository.ParticipationOutboxRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Writes outbox rows. Must always be called within the same @Transactional boundary as
 * the participations insert/update it's recording, so both commit or roll back together
 * (docs §8.1 / §8.2). Actual Kafka publication is decoupled - see OutboxPoller.
 */
@Component
public class OutboxEventWriter {

    private final ParticipationOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventWriter(ParticipationOutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public void write(UUID aggregateId, String eventType, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            outboxRepository.save(new ParticipationOutbox(aggregateId, eventType, json));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox payload for event " + eventType, e);
        }
    }
}
