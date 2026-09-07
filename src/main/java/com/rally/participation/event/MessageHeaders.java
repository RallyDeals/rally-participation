package com.rally.participation.event;

/**
 * Kafka/HTTP header names used for correlation, causation, and the outbox envelope
 * (see OBSERVABILITY_GUIDE.md §1, §6-§10). Centralized here instead of hardcoded string
 * literals so the filter, interceptor, and outbox writer/poller all agree on the exact
 * header names.
 */
public final class MessageHeaders {

    public static final String ID = "X-Id";
    public static final String TYPE = "X-Type";
    public static final String CORRELATION_ID = "X-Correlation-Id";
    public static final String CAUSATION_ID = "X-Causation-Id";
    public static final String TRACE_ID = "X-Trace-Id";

    private MessageHeaders() {
    }
}
