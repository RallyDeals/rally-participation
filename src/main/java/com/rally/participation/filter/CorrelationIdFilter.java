package com.rally.participation.filter;

import com.rally.participation.event.MessageHeaders;
import io.micrometer.tracing.BaggageInScope;
import io.micrometer.tracing.BaggageManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Reuses a caller-provided X-Correlation-Id or generates one, writes it to MDC + baggage
 * for the request's duration, and echoes it back on the response so callers can correlate
 * their own logging. See OBSERVABILITY_GUIDE.md §6-§7.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private final BaggageManager baggageManager;

    public CorrelationIdFilter(BaggageManager baggageManager) {
        this.baggageManager = baggageManager;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String correlationId = request.getHeader(MessageHeaders.CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MessageHeaders.CORRELATION_ID, correlationId);

        try (BaggageInScope ignored = baggageManager.createBaggageInScope(MessageHeaders.CORRELATION_ID, correlationId)) {
            response.addHeader(MessageHeaders.CORRELATION_ID, correlationId);
            try {
                filterChain.doFilter(request, response);
            } finally {
                MDC.remove(MessageHeaders.CORRELATION_ID);
            }
        }
    }
}
