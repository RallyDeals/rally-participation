package com.rally.participation.client;

import java.time.Instant;
import java.util.UUID;

/**
 * Shape for the new GET /deals/{dealId}/summary contract (docs §6 addition). Confirm
 * field names/types against Deal Service's actual response once it's built - this is
 * Participation Service's assumption of what it needs for the progress endpoint.
 */
public record DealSummaryResponse(
    UUID dealId,
    String status,           // e.g. ACTIVE, SUCCEEDED, FAILED, EXPIRED
    Integer minParticipants,
    Integer stockCap,
    Integer reservedCount,   // Deal Service's own reservation counter, may differ briefly from our activeParticipants
    Instant endTime
) {}
