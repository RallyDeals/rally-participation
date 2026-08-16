package com.rally.participation.dto;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record DealProgressResponse(
    UUID dealId,
    long activeParticipants,
    Integer minParticipants,
    Integer stockCap,
    Instant endTime,
    Duration timeRemaining
) {}
