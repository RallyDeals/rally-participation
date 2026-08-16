package com.rally.participation.dto;

import java.util.List;

public record ParticipantsPageResponse(
    List<ParticipantSummary> participants,
    long activeCount,
    int page,
    int size
) {}
