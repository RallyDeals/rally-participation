package com.rally.participation.event;

import java.time.Instant;
import java.util.UUID;

public record ParticipantLeftPayload(
    UUID participantId,
    UUID dealId
) {}
