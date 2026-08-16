package com.rally.participation.web;

import com.rally.participation.dto.InviteResolutionResponse;
import com.rally.participation.dto.ReferralLinkResponse;
import com.rally.participation.service.ReferralLinkService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class ReferralController {

    private final ReferralLinkService referralLinkService;

    public ReferralController(ReferralLinkService referralLinkService) {
        this.referralLinkService = referralLinkService;
    }

    @PostMapping("/deals/{dealId}/invite-link")
    @ResponseStatus(HttpStatus.CREATED)
    public ReferralLinkResponse createInviteLink(@PathVariable UUID dealId, @CurrentUser UUID userId) {
        return referralLinkService.createLink(dealId, userId);
    }

    @GetMapping("/invites/{code}")
    public InviteResolutionResponse resolveInvite(@PathVariable String code) {
        return referralLinkService.resolve(code);
    }
}
