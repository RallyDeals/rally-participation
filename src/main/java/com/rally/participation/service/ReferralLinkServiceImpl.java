package com.rally.participation.service;

import com.rally.participation.domain.ParticipationStatus;
import com.rally.participation.domain.ReferralLink;
import com.rally.participation.dto.InviteResolutionResponse;
import com.rally.participation.dto.ReferralLinkResponse;
import com.rally.participation.exception.NotAnActiveParticipantForInviteException;
import com.rally.participation.exception.ReferralCodeNotFoundException;
import com.rally.participation.repository.ParticipationRepository;
import com.rally.participation.repository.ReferralLinkRepository;
import com.rally.participation.util.ReferralCodeGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ReferralLinkServiceImpl implements ReferralLinkService {

    private final ReferralLinkRepository referralLinkRepository;
    private final ParticipationRepository participationRepository;

    public ReferralLinkServiceImpl(ReferralLinkRepository referralLinkRepository,
                                    ParticipationRepository participationRepository) {
        this.referralLinkRepository = referralLinkRepository;
        this.participationRepository = participationRepository;
    }

    @Override
    @Transactional
    public ReferralLinkResponse createLink(UUID dealId, UUID userId) {
        boolean isActiveParticipant = participationRepository
            .findByDealIdAndUserIdAndStatus(dealId, userId, ParticipationStatus.ACTIVE)
            .isPresent();
        if (!isActiveParticipant) {
            throw new NotAnActiveParticipantForInviteException(dealId, userId);
        }

        String code = generateUniqueCode();
        ReferralLink link = new ReferralLink(code, dealId, userId, null);
        referralLinkRepository.save(link);
        return ReferralLinkResponse.from(link);
    }

    @Override
    @Transactional(readOnly = true)
    public InviteResolutionResponse resolve(String code) {
        ReferralLink link = referralLinkRepository.findById(code)
            .orElseThrow(() -> new ReferralCodeNotFoundException(code));
        return new InviteResolutionResponse(link.getDealId(), link.getReferrerUserId(), link.isExpired());
    }

    private String generateUniqueCode() {
        String code;
        int attempts = 0;
        do {
            code = ReferralCodeGenerator.generate();
            attempts++;
            if (attempts > 5) {
                // Astronomically unlikely at 8 base62 chars, but fail loudly rather than loop forever.
                throw new IllegalStateException("Could not generate a unique referral code after " + attempts + " attempts");
            }
        } while (referralLinkRepository.existsById(code));
        return code;
    }
}
