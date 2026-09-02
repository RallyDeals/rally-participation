package com.rally.participation.integration;

import com.rally.participation.domain.ParticipationOutbox;
import com.rally.participation.event.EventType;
import com.rally.participation.event.OutboxPoller;
import com.rally.participation.repository.ParticipationOutboxRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the transactional outbox actually gets from a DB row to a real Kafka topic -
 * the piece the design doc (§8.2) relies on to close the leave-flip-before-publish gap.
 */
@DirtiesContext
class OutboxPollerIT extends AbstractIntegrationTest {

    @Autowired
    private ParticipationOutboxRepository outboxRepository;

    @Autowired
    private OutboxPoller outboxPoller;

    @Value("${rally.kafka.topics.participation}")
    private String participantJoinedTopic;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUpConsumer() {
        var consumerProps = KafkaTestUtils.consumerProps(KAFKA.getBootstrapServers(), "outbox-poller-it", "true");
        consumerProps.put("key.deserializer", StringDeserializer.class);
        consumerProps.put("value.deserializer", StringDeserializer.class);
        consumerProps.put("auto.offset.reset", "earliest");
        consumer = new DefaultKafkaConsumerFactory<String, String>(consumerProps).createConsumer();
        consumer.subscribe(List.of(participantJoinedTopic));
    }

    @AfterEach
    void tearDownConsumer() {
        consumer.close();
    }

    @Test
    void publishesUnpublishedRowToKafkaAndMarksItPublished() {
        UUID aggregateId = UUID.randomUUID();
        String payloadJson = "{\"dealId\":\"" + aggregateId + "\",\"note\":\"outbox-it\"}";
        outboxRepository.saveAndFlush(new ParticipationOutbox(aggregateId, EventType.PARTICIPANT_JOINED, payloadJson));

        outboxPoller.publishPending();

        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, participantJoinedTopic, Duration.ofSeconds(15));
        assertThat(record.key()).isEqualTo(aggregateId.toString());
        assertThat(record.value()).contains("outbox-it");

        List<ParticipationOutbox> stillUnpublished =
            outboxRepository.findByPublishedAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, 10));
        assertThat(stillUnpublished).isEmpty();
    }

    @Test
    void alreadyPublishedRowsAreNotRepublished() {
        UUID aggregateId = UUID.randomUUID();
        ParticipationOutbox row = outboxRepository.saveAndFlush(
            new ParticipationOutbox(aggregateId, EventType.PARTICIPANT_JOINED, "{\"already\":\"published\"}"));

        outboxPoller.publishPending(); // publishes it once
        KafkaTestUtils.getSingleRecord(consumer, participantJoinedTopic, Duration.ofSeconds(15)); // drain it

        outboxPoller.publishPending(); // should be a no-op now

        var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(3));
        assertThat(records.count()).isZero();
    }
}
