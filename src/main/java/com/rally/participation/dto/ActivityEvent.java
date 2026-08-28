package com.rally.participation.dto;

import java.time.Instant;
import java.util.UUID;

public record ActivityEvent(
    UUID userId,
    String type,
    Instant timestamp
) {}
