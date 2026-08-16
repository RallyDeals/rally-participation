package com.rally.participation.exception;

import org.springframework.http.HttpStatus;

public class ReferralCodeExpiredException extends ParticipationApiException {
    public ReferralCodeExpiredException(String code) {
        super(HttpStatus.GONE, "Referral code expired: " + code);
    }
}
