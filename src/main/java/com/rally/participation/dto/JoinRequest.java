package com.rally.participation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinRequest(
    @Size(max = 16) String referralCode,
    @NotBlank String paymentMethodId,
    @NotBlank @Size(max = 500) String address
) {}
