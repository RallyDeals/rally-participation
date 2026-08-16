package com.rally.participation.service;

import com.rally.participation.dto.InviteResolutionResponse;
import com.rally.participation.dto.ReferralLinkResponse;

import java.util.UUID;

public interface ReferralLinkService {

    ReferralLinkResponse createLink(UUID dealId, UUID userId);

    InviteResolutionResponse resolve(String code);
}
