package com.rally.participation.service;

import com.rally.participation.client.DealServiceClient;
import com.rally.participation.client.DealSummaryResponse;
import com.rally.participation.domain.Participation;
import com.rally.participation.domain.ParticipationStatus;
import com.rally.participation.domain.ReferralLink;
import com.rally.participation.dto.ActivityEvent;
import com.rally.participation.dto.DealProgressResponse;
import com.rally.participation.dto.ParticipantSummary;
import com.rally.participation.dto.ParticipantsPageResponse;
import com.rally.participation.dto.ParticipationResponse;
import com.rally.participation.event.EventType;
import com.rally.participation.event.OutboxEventWriter;
import com.rally.participation.event.ParticipantJoinedPayload;
import com.rally.participation.event.ParticipantLeftPayload;
import com.rally.participation.exception.*;
import com.rally.participation.repository.ParticipationRepository;
import com.rally.participation.repository.ReferralLinkRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ParticipationServiceImpl implements ParticipationService {

    private final ParticipationRepository participationRepository;
    private final ReferralLinkRepository referralLinkRepository;
    private final DealServiceClient dealServiceClient;
    private final OutboxEventWriter outboxEventWriter;

    public ParticipationServiceImpl(ParticipationRepository participationRepository,
                                     ReferralLinkRepository referralLinkRepository,
                                     DealServiceClient dealServiceClient,
                                     OutboxEventWriter outboxEventWriter) {
        this.participationRepository = participationRepository;
        this.referralLinkRepository = referralLinkRepository;
        this.dealServiceClient = dealServiceClient;
        this.outboxEventWriter = outboxEventWriter;
    }

    @Override
    @Transactional
    public ParticipationResponse join(UUID dealId, UUID userId, String referralCode, String paymentMethodId, String address) {
        UUID referredBy = resolveReferrer(dealId, referralCode);

        if (participationRepository.findByDealIdAndUserIdAndStatus(dealId, userId, ParticipationStatus.ACTIVE).isPresent()) {
            throw new AlreadyActiveParticipantException(dealId, userId);
        }

        // Fetch deal info for productId and dealPrice BEFORE reserve call
        DealSummaryResponse dealSummary = dealServiceClient.getDealSummary(dealId);

        // Sync gate call - see docs §6 and §8.1 for the crash-window risk this leaves open.
        dealServiceClient.reserveSlot(dealId);

        Participation participation = new Participation(dealId, userId, referredBy);
        try {
            participation = participationRepository.save(participation);
        } catch (DataIntegrityViolationException e) {
            throw new AlreadyActiveParticipantException(dealId, userId);
        }

        ParticipantJoinedPayload payload = new ParticipantJoinedPayload(
            participation.getId(),
            participation.getDealId(),
            participation.getUserId(),
            dealSummary.productId(),
            dealSummary.dealPrice(),
            paymentMethodId,
            address,
            participation.getJoinedAt()
        );
        outboxEventWriter.write(participation.getId(), EventType.PARTICIPANT_JOINED, payload);

        return ParticipationResponse.from(participation);
    }

    @Override
    @Transactional
    public void leave(UUID dealId, UUID userId) {
        Participation participation = participationRepository
            .findByDealIdAndUserIdAndStatus(dealId, userId, ParticipationStatus.ACTIVE)
            .orElseThrow(() -> new NotActiveParticipantException(dealId, userId));

        // Sync gate call - see docs §6.
        dealServiceClient.checkLeaveEligible(dealId);

        participation.markLeft();
        participationRepository.save(participation);

        ParticipantLeftPayload payload = new ParticipantLeftPayload(
            participation.getId(),
            participation.getDealId()
        );
        outboxEventWriter.write(participation.getId(), EventType.PARTICIPANT_LEFT, payload);
    }

    @Override
    @Transactional(readOnly = true)
    public ParticipantsPageResponse listParticipants(UUID dealId, boolean activeOnly, Pageable pageable) {
        Page<Participation> page = activeOnly
            ? participationRepository.findByDealIdAndStatus(dealId, ParticipationStatus.ACTIVE, pageable)
            : participationRepository.findByDealId(dealId, pageable);

        List<ParticipantSummary> summaries = page.getContent().stream()
            .map(ParticipantSummary::from)
            .toList();

        long activeCount = participationRepository.countByDealIdAndStatus(dealId, ParticipationStatus.ACTIVE);

        return new ParticipantsPageResponse(summaries, activeCount, pageable.getPageNumber(), pageable.getPageSize());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActiveParticipant(UUID dealId, UUID participationId) {
       return participationRepository.existsByDealIdAndIdAndStatus(dealId, participationId, ParticipationStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public DealProgressResponse getProgress(UUID dealId) {
        long activeCount = participationRepository.countByDealIdAndStatus(dealId, ParticipationStatus.ACTIVE);
        DealSummaryResponse summary = dealServiceClient.getDealSummary(dealId);

        Duration timeRemaining = null;
        if (summary.endTime() != null) {
            Duration remaining = Duration.between(Instant.now(), summary.endTime());
            timeRemaining = remaining.isNegative() ? Duration.ZERO : remaining;
        }

        return new DealProgressResponse(
            dealId,
            activeCount,
            summary.minParticipants(),
            summary.stockCap(),
            summary.endTime(),
            timeRemaining
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityEvent> getActivity(UUID dealId) {
        List<Participation> participations = participationRepository.findTop50ByDealIdOrderByJoinedAtDesc(dealId);
        List<ActivityEvent> events = new java.util.ArrayList<>();
        for (Participation p : participations) {
            if(p.getStatus().equals(ParticipationStatus.PENDING)) {
                events.add(new ActivityEvent(p.getUserId(), "PENDING", p.getJoinedAt()));
                continue;
            }
            events.add(new ActivityEvent(p.getUserId(), "JOINED", p.getJoinedAt()));
            if (p.getLeftAt() != null) {
                events.add(new ActivityEvent(p.getUserId(), "LEFT", p.getLeftAt()));
            }
        }
        events.sort((a, b) -> b.timestamp().compareTo(a.timestamp()));
        return events;
    }

    private UUID resolveReferrer(UUID dealId, String referralCode) {
        if (referralCode == null || referralCode.isBlank()) {
            return null;
        }
        ReferralLink link = referralLinkRepository.findById(referralCode)
            .orElseThrow(() -> new ReferralCodeNotFoundException(referralCode));
        if (link.isExpired()) {
            throw new ReferralCodeExpiredException(referralCode);
        }
        if (!link.getDealId().equals(dealId)) {
            // Code belongs to a different deal - treat like "not found" for this deal
            // rather than silently misattributing the referral.
            throw new ReferralCodeNotFoundException(referralCode);
        }
        return link.getReferrerUserId();
    }
}
