package com.rally.participation.client;

import java.util.UUID;

/**
 * Sync outbound contract to Deal Service (docs §6). Participation Service never mutates
 * a deal's capacity itself - it only ever reserves (join) or checks eligibility (leave).
 *
 * TODO: swap the {@code StubDealServiceClient} bean for a real WebClient-backed
 * implementation once Deal Service is up (point it at rally.deal-service.base-url).
 */
public interface DealServiceClient {

    /**
     * Calls POST /deals/{dealId}/reserve-slot.
     * Throws DealNotFoundException if the deal doesn't exist,
     * DealNotJoinableException if the deal is full or not in a joinable state,
     * DealServiceUnavailableException if the call couldn't be completed at all.
     */
    void reserveSlot(UUID dealId);

    /**
     * Calls POST /deals/{dealId}/check-leave-eligible.
     * Throws DealNotFoundException if the deal doesn't exist,
     * LeaveNotEligibleException if the deal isn't active/pending or is inside the cutoff window,
     * DealServiceUnavailableException if the call couldn't be completed at all.
     */
    void checkLeaveEligible(UUID dealId);

    /**
     * Calls GET /deals/{dealId}/summary. Read-only; used by the progress endpoint.
     * Throws DealNotFoundException if the deal doesn't exist,
     * DealServiceUnavailableException if the call couldn't be completed at all.
     */
    DealSummaryResponse getDealSummary(UUID dealId);
}
