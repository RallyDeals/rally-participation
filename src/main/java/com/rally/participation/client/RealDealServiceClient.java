package com.rally.participation.client;

import com.rally.participation.exception.DealNotFoundException;
import com.rally.participation.exception.DealNotJoinableException;
import com.rally.participation.exception.DealServiceUnavailableException;
import com.rally.participation.exception.LeaveNotEligibleException;
import com.rally.participation.exception.ParticipationApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Real implementation of the sync contracts to Deal Service (docs §6). Inactive by
 * default - activate with spring.profiles.active=real-deal-service once Deal Service
 * exists and rally.deal-service.base-url points at it.
 *
 * Business errors (404/409) are propagated as our domain exceptions and are never
 * retried. Transient failures (timeout, connection refused, 5xx) get a small bounded
 * retry with backoff, then surface as DealServiceUnavailableException - we deliberately
 * never assume success on ambiguity (see docs §6 recommendation).
 */
@Component
@Profile("real-deal-service")
public class RealDealServiceClient implements DealServiceClient {

    private static final Logger log = LoggerFactory.getLogger(RealDealServiceClient.class);

    private final WebClient webClient;
    private final Duration timeout;
    private final long maxRetryAttempts;
    private final Duration retryBackoff;

    public RealDealServiceClient(WebClient dealServiceWebClient,
                                  @Value("${rally.deal-service.timeout-ms:2000}") long timeoutMs,
                                  @Value("${rally.deal-service.retry.max-attempts:2}") long maxRetryAttempts,
                                  @Value("${rally.deal-service.retry.backoff-ms:200}") long backoffMs) {
        this.webClient = dealServiceWebClient;
        this.timeout = Duration.ofMillis(timeoutMs);
        this.maxRetryAttempts = maxRetryAttempts;
        this.retryBackoff = Duration.ofMillis(backoffMs);
    }

    @Override
    public void reserveSlot(UUID dealId) {
        post("/deals/" + dealId + "/reserve-slot", dealId,
            () -> new DealNotJoinableException(dealId, "rejected by Deal Service"));
    }

    @Override
    public void checkLeaveEligible(UUID dealId) {
        post("/deals/" + dealId + "/check-leave-eligible", dealId,
            () -> new LeaveNotEligibleException(dealId, "rejected by Deal Service"));
    }

    @Override
    public DealSummaryResponse getDealSummary(UUID dealId) {
        try {
            return webClient.get()
                .uri("/deals/{dealId}/summary", dealId)
                .retrieve()
                .onStatus(status -> status.value() == 404, resp -> Mono.error(new DealNotFoundException(dealId)))
                .bodyToMono(DealSummaryResponse.class)
                .timeout(timeout)
                .retryWhen(Retry.backoff(maxRetryAttempts, retryBackoff).filter(this::isRetryable))
                .block();
        } catch (ParticipationApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Deal Service getDealSummary failed for dealId={}", dealId, e);
            throw new DealServiceUnavailableException("Deal Service summary call failed for deal " + dealId + ": " + e.getMessage());
        }
    }

    private void post(String path, UUID dealId, Supplier<ParticipationApiException> conflictSupplier) {
        try {
            webClient.post()
                .uri(path)
                .retrieve()
                .onStatus(status -> status.value() == 404, resp -> Mono.error(new DealNotFoundException(dealId)))
                .onStatus(status -> status.value() == 409, resp -> Mono.error(conflictSupplier.get()))
                .toBodilessEntity()
                .timeout(timeout)
                .retryWhen(Retry.backoff(maxRetryAttempts, retryBackoff).filter(this::isRetryable))
                .block();
        } catch (ParticipationApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Deal Service call failed: {}", path, e);
            throw new DealServiceUnavailableException("Deal Service call failed: " + path + " - " + e.getMessage());
        }
    }

    private boolean isRetryable(Throwable throwable) {
        // Business errors (404/409) are deliberate outcomes, not transient failures - never retry them.
        return !(throwable instanceof ParticipationApiException);
    }
}
