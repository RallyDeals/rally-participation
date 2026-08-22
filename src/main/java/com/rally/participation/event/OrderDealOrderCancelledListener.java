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

/**
 * Consumes order.deal_order_cancelled for termination paths Participation Service did
 * NOT initiate itself (payment declined at join, authorization timeout). Guarded update
 * makes this idempotent under Kafka's at-least-once redelivery (docs §5.3). Deliberately
 * does not publish participant.left in response - Order Service already knows about
 * this cancellation since it caused it.
 */
@Component
public class OrderDealOrderCancelledListener {

    private static final Logger log = LoggerFactory.getLogger(OrderDealOrderCancelledListener.class);

    private final ParticipationRepository participationRepository;
    private final ObjectMapper objectMapper;

    public OrderDealOrderCancelledListener(ParticipationRepository participationRepository, ObjectMapper objectMapper) {
        this.participationRepository = participationRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${rally.kafka.topics.order-events}")
    @Transactional
    public void onOrderDealOrderCancelled(@Header(value = "X-Type", required = false) String eventType, String message) {
        if (!EventType.ORDER_DEAL_CANCELLED.equals(eventType)) {
            log.debug("Ignoring order.lifecycle_events message with X-Type={} (not {})", eventType, EventType.ORDER_DEAL_CANCELLED);
            return;
        }
        try {
            OrderDealOrderCancelledPayload payload = objectMapper.readValue(message, OrderDealOrderCancelledPayload.class);
            int updated = participationRepository.flipToLeftIfActive(payload.dealId(), payload.userId(), Instant.now());
            if (updated == 0) {
                log.debug("order.deal_order_cancelled for dealId={} userId={} - no ACTIVE participation found " +
                    "(already LEFT, or duplicate delivery) - treated as a no-op", payload.dealId(), payload.userId());
            } else {
                log.info("Flipped participation to LEFT via order.deal_order_cancelled: dealId={} userId={}",
                    payload.dealId(), payload.userId());
            }
        } catch (Exception e) {
            log.error("Failed to process order.deal_order_cancelled message: {}", message, e);
            // TODO: route to a dead-letter topic instead of swallowing, once DLT wiring exists
        }
    }
}
