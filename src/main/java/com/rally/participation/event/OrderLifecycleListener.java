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
public class OrderLifecycleListener {
    private static final Logger log = LoggerFactory.getLogger(OrderLifecycleListener.class);

    private final ParticipationRepository participationRepository;
    private final ObjectMapper objectMapper;

    public OrderLifecycleListener(ParticipationRepository participationRepository, ObjectMapper objectMapper) {
        this.participationRepository = participationRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${rally.kafka.topics.order-events}")
    @Transactional
    public void onOrderAuthorization(@Header(value = "X-Type", required = false) String eventType, String message) {
        if (EventType.ORDER_AUTHORIZED.equals(eventType)) {
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
        else if(EventType.ORDER_DEAL_CANCELLED.equals(eventType)){
            try {
                OrderDealOrderCancelledPayload payload = objectMapper.readValue(message, OrderDealOrderCancelledPayload.class);

                int declined = participationRepository.flipToDeclinedIfPending(payload.dealId(), payload.userId());
                if (declined > 0) {
                    log.info("Flipped participation to DECLINED via order.deal_order_cancelled: dealId={} userId={}",
                            payload.dealId(), payload.userId());
                    return;
                }

                int left = participationRepository.flipToLeftIfActive(payload.dealId(), payload.userId(), Instant.now());
                if (left == 0) {
                    log.debug("order.deal_order_cancelled for dealId={} userId={} - no ACTIVE/PENDING participation found " +
                            "(already LEFT/DECLINED, or duplicate delivery) - treated as a no-op", payload.dealId(), payload.userId());
                } else {
                    log.info("Flipped participation to LEFT via order.deal_order_cancelled: dealId={} userId={}",
                            payload.dealId(), payload.userId());
                }
            } catch (Exception e) {
                log.error("Failed to process order.deal_order_cancelled message: {}", message, e);
                // TODO: route to a dead-letter topic instead of swallowing, once DLT wiring exists
            }
        }
        else {
            log.debug("Ignoring order.lifecycle_events message with X-Type={} (not {}/{})", eventType, EventType.ORDER_DEAL_CANCELLED, EventType.ORDER_AUTHORIZED);
        }
    }
}
