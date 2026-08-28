package com.rally.participation.event;

import com.rally.participation.config.KafkaTopicsProperties;
import com.rally.participation.domain.ParticipationOutbox;
import com.rally.participation.repository.ParticipationOutboxRepository;
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
            try {
                String topic = topics.getParticipation();
                String key = row.getAggregateId().toString();
                String eventType = row.getEventType();

                ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, row.getPayload());
                record.headers().add(new RecordHeader("X-Id", row.getId().toString().getBytes(StandardCharsets.UTF_8)));
                record.headers().add(new RecordHeader("X-Type", eventType.getBytes(StandardCharsets.UTF_8)));
                record.headers().add(new RecordHeader("X-Correlation-Id", row.getAggregateId().toString().getBytes(StandardCharsets.UTF_8)));
                record.headers().add(new RecordHeader("X-Causation-Id", row.getAggregateId().toString().getBytes(StandardCharsets.UTF_8)));
                record.headers().add(new RecordHeader("X-Trace-Id", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));

                kafkaTemplate.send(record).get();
                row.markPublished();
            } catch (Exception e) {
                log.error("Failed to publish outbox row id={} eventType={} - will retry next poll",
                    row.getId(), row.getEventType(), e);
            }
        }
    }
}
