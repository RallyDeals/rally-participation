package com.rally.participation.dto;

import java.util.UUID;

public record InviteResolutionResponse(
    UUID dealId,
    UUID referrerUserId,
    boolean expired
) {}
