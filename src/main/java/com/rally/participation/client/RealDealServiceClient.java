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
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

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
        try {
            Map<String, UUID> body = Map.of("requestId", UUID.randomUUID());
            webClient.post()
                .uri("/internal/deals/{dealId}/reserve-slot", dealId)
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.value() == 404, resp -> Mono.error(new DealNotFoundException(dealId)))
                .onStatus(status -> status.value() == 409, resp -> Mono.error(new DealNotJoinableException(dealId, "rejected by Deal Service")))
                .toBodilessEntity()
                .timeout(timeout)
                .retryWhen(Retry.backoff(maxRetryAttempts, retryBackoff).filter(this::isRetryable))
                .block();
        } catch (ParticipationApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Deal Service reserveSlot failed for dealId={}", dealId, e);
            throw new DealServiceUnavailableException("Deal Service reserveSlot failed for deal " + dealId + ": " + e.getMessage());
        }
    }

    @Override
    public void checkLeaveEligible(UUID dealId) {
        try {
            LeaveEligibilityResponse response = webClient.get()
                .uri("/internal/deals/{dealId}/check-leave-eligible", dealId)
                .retrieve()
                .onStatus(status -> status.value() == 404, resp -> Mono.error(new DealNotFoundException(dealId)))
                .bodyToMono(LeaveEligibilityResponse.class)
                .timeout(timeout)
                .retryWhen(Retry.backoff(maxRetryAttempts, retryBackoff).filter(this::isRetryable))
                .block();

            if (response != null && !response.eligible()) {
                throw new LeaveNotEligibleException(dealId, response.reason());
            }
        } catch (ParticipationApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Deal Service checkLeaveEligible failed for dealId={}", dealId, e);
            throw new DealServiceUnavailableException("Deal Service checkLeaveEligible failed for deal " + dealId + ": " + e.getMessage());
        }
    }

    @Override
    public DealSummaryResponse getDealSummary(UUID dealId) {
        try {
            DealResponse dealResponse = webClient.get()
                .uri("/deals/{dealId}", dealId)
                .retrieve()
                .onStatus(status -> status.value() == 404, resp -> Mono.error(new DealNotFoundException(dealId)))
                .bodyToMono(DealResponse.class)
                .timeout(timeout)
                .retryWhen(Retry.backoff(maxRetryAttempts, retryBackoff).filter(this::isRetryable))
                .block();

            if (dealResponse == null) {
                throw new DealNotFoundException(dealId);
            }

            Instant endTime = dealResponse.endTime() != null ? dealResponse.endTime().toInstant() : null;

            return new DealSummaryResponse(
                dealResponse.id(),
                dealResponse.status(),
                dealResponse.minParticipants(),
                dealResponse.dealStock(),
                dealResponse.currentParticipants(),
                endTime
            );
        } catch (ParticipationApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Deal Service getDealSummary failed for dealId={}", dealId, e);
            throw new DealServiceUnavailableException("Deal Service summary call failed for deal " + dealId + ": " + e.getMessage());
        }
    }

    private boolean isRetryable(Throwable throwable) {
        return !(throwable instanceof ParticipationApiException);
    }
}
