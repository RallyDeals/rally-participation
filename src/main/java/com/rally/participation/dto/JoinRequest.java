package com.rally.participation.dto;

import jakarta.validation.constraints.Size;

public record JoinRequest(
    @Size(max = 16) String referralCode
) {}
