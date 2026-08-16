package com.rally.participation.client;

import com.rally.participation.exception.DealNotJoinableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Temporary in-memory stand-in for Deal Service until it exists as a real dependency.
 * Active by default (see spring.profiles.active: stub-deal-service in application.yml).
 * Simulates a fixed per-deal capacity so join-capacity conflicts (409) are exercisable
 * end-to-end. Leave-eligibility always succeeds here since the real cutoff-window logic
 * lives entirely in Deal Service and can't be meaningfully faked without deal timing data.
 *
 * Swap to the real-deal-service profile (RealDealServiceClient) once Deal Service exists.
 */
@Component
@Profile("stub-deal-service")
public class StubDealServiceClient implements DealServiceClient {

    private static final Logger log = LoggerFactory.getLogger(StubDealServiceClient.class);

    private final Map<UUID, AtomicInteger> reservedCounts = new ConcurrentHashMap<>();

    @Value("${rally.deal-service.stub.default-capacity:5}")
    private int defaultCapacity;

    @Value("${rally.deal-service.stub.default-min-participants:3}")
    private int defaultMinParticipants;

    @Value("${rally.deal-service.stub.default-end-time-days-from-now:7}")
    private int defaultEndTimeDaysFromNow;

    @Override
    public void reserveSlot(UUID dealId) {
        AtomicInteger counter = reservedCounts.computeIfAbsent(dealId, id -> new AtomicInteger(0));

        int current;
        do {
            current = counter.get();
            if (current >= defaultCapacity) {
                throw new DealNotJoinableException(dealId, "stub capacity (" + defaultCapacity + ") reached");
            }
        } while (!counter.compareAndSet(current, current + 1));

        log.debug("[stub] reserve-slot dealId={} reservedCount={}/{}", dealId, current + 1, defaultCapacity);
    }

    @Override
    public void checkLeaveEligible(UUID dealId) {
        log.debug("[stub] check-leave-eligible dealId={} -> always eligible", dealId);
        // No-op: the stub has no notion of deal timing/cutoff windows.
    }

    @Override
    public DealSummaryResponse getDealSummary(UUID dealId) {
        int reservedCount = reservedCounts.getOrDefault(dealId, new AtomicInteger(0)).get();
        return new DealSummaryResponse(
            dealId,
            "ACTIVE",
            defaultMinParticipants,
            defaultCapacity,
            reservedCount,
            Instant.now().plus(Duration.ofDays(defaultEndTimeDaysFromNow))
        );
    }
}
