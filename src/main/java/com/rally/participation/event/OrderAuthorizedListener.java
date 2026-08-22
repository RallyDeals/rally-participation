package com.rally.participation.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rally.participation.repository.ParticipationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class OrderAuthorizedListener {
    private static final Logger log = LoggerFactory.getLogger(OrderAuthorizedListener.class);

    private final ParticipationRepository participationRepository;
    private final ObjectMapper objectMapper;

    public OrderAuthorizedListener(ParticipationRepository participationRepository, ObjectMapper objectMapper) {
        this.participationRepository = participationRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${rally.kafka.topics.order-events}")
    @Transactional
    public void onOrderAuthorization(@Header(value = "X-Type", required = false) String eventType, String message) {
        if (!EventType.ORDER_AUTHORIZED.equals(eventType)) {
            log.debug("Ignoring order.lifecycle_events message with X-Type={} (not {})", eventType, EventType.ORDER_AUTHORIZED);
            return;
        }
        try {
            OrderAuthorizedPayload payload = objectMapper.readValue(message, OrderAuthorizedPayload.class);
            int updated = participationRepository.flipToActiveIfPending(payload.dealId(), payload.userId());
            if (updated == 0) {
                log.debug("order.authorized for dealId={} userId={} - no PENDING participation found " +
                        "(already ACTIVE or LEFT, or duplicate delivery) - treated as a no-op", payload.dealId(), payload.userId());
            } else {
                log.info("Flipped participation to ACTIVE via order.authorized: dealId={} userId={}",
                        payload.dealId(), payload.userId());
            }
        } catch (Exception e) {
            log.error("Failed to process order.authorized message: {}", message, e);
            // TODO: route to a dead-letter topic instead of swallowing, once DLT wiring exists
        }
    }
}
