package com.rally.participation.event;

import io.micrometer.tracing.BaggageManager;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Populates MDC + baggage with the inbound X-Correlation-Id header for the duration of
 * each @KafkaListener invocation, then cleans up. Picked up automatically by the
 * auto-configured ConcurrentKafkaListenerContainerFactory since this service doesn't
 * define its own factory bean (see OBSERVABILITY_GUIDE.md §8 - option (a)). If a custom
 * factory is ever introduced here, it must call factory.setRecordInterceptor(this)
 * explicitly, or correlation silently stops propagating on Kafka consumption.
 */
@Component
public class KafkaCorrelationIdInterceptor implements RecordInterceptor<Object, Object> {

    private final BaggageManager baggageManager;

    public KafkaCorrelationIdInterceptor(BaggageManager baggageManager) {
        this.baggageManager = baggageManager;
    }

    @Override
    public ConsumerRecord<Object, Object> intercept(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
        String correlationId = extractHeader(record, MessageHeaders.CORRELATION_ID);
        if (correlationId != null) {
            MDC.put(MessageHeaders.CORRELATION_ID, correlationId);
            baggageManager.createBaggageInScope(MessageHeaders.CORRELATION_ID, correlationId);
        }
        return record;
    }

    @Override
    public void afterRecord(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
        MDC.remove(MessageHeaders.CORRELATION_ID);
    }

    private String extractHeader(ConsumerRecord<Object, Object> record, String key) {
        var header = record.headers().lastHeader(key);
        if (header == null || header.value() == null) {
            return null;
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }
}
