package com.rally.participation.client;

import java.util.UUID;

public record LeaveEligibilityResponse(
    boolean eligible,
    UUID dealId,
    String reason
) {}
