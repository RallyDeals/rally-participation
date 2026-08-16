package com.rally.participation.exception;

import org.springframework.http.HttpStatus;

public class ReferralCodeNotFoundException extends ParticipationApiException {
    public ReferralCodeNotFoundException(String code) {
        super(HttpStatus.NOT_FOUND, "Referral code not found: " + code);
    }
}
