package com.rally.participation.service;

import com.rally.participation.client.DealServiceClient;
import com.rally.participation.client.DealSummaryResponse;
import com.rally.participation.domain.Participation;
import com.rally.participation.domain.ParticipationStatus;
import com.rally.participation.domain.ReferralLink;
import com.rally.participation.dto.ParticipationResponse;
import com.rally.participation.event.OutboxEventWriter;
import com.rally.participation.exception.*;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import com.rally.participation.repository.ParticipationRepository;
import com.rally.participation.repository.ReferralLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParticipationServiceImplTest {

    @Mock
    private ParticipationRepository participationRepository;
    @Mock
    private ReferralLinkRepository referralLinkRepository;
    @Mock
    private DealServiceClient dealServiceClient;
    @Mock
    private OutboxEventWriter outboxEventWriter;

    private ParticipationServiceImpl service;

    private final UUID dealId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    private DealSummaryResponse stubDealSummary() {
        return new DealSummaryResponse(dealId, productId, "ACTIVE", 10, 100, 0, BigDecimal.valueOf(49.99), Instant.now().plusSeconds(86400));
    }

    @BeforeEach
    void setUp() {
        service = new ParticipationServiceImpl(participationRepository, referralLinkRepository, dealServiceClient, outboxEventWriter);
    }

    @Test
    void join_success_reservesSlotInsertsRowAndWritesOutboxEvent() throws Exception {
        when(participationRepository.findByDealIdAndUserIdAndStatus(dealId, userId, ParticipationStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(dealServiceClient.getDealSummary(dealId)).thenReturn(stubDealSummary());

        when(participationRepository.save(any(Participation.class)))
                .thenAnswer(invocation -> {
                    Participation participation = invocation.getArgument(0);
                    Field idField = Participation.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(participation, UUID.randomUUID());
                    return participation;
                });

        ParticipationResponse response = service.join(dealId, userId, null, "pm_123", "123 Main St");

        assertThat(response.dealId()).isEqualTo(dealId);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.status()).isEqualTo("ACTIVE");

        verify(dealServiceClient).getDealSummary(dealId);
        verify(dealServiceClient).reserveSlot(dealId);

        verify(outboxEventWriter).write(
                any(UUID.class),
                eq("Participant.Joined"),
                any()
        );
    }

    @Test
    void join_enrichedPayload_containsProductIdAndPriceAndPaymentDetails() throws Exception {
        when(participationRepository.findByDealIdAndUserIdAndStatus(dealId, userId, ParticipationStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(dealServiceClient.getDealSummary(dealId)).thenReturn(stubDealSummary());
        when(participationRepository.save(any(Participation.class)))
            .thenAnswer(inv -> {
                Participation p = inv.getArgument(0);
                Field idField = Participation.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(p, UUID.randomUUID());
                return p;
            });

        service.join(dealId, userId, null, "pm_abc", "456 Oak Ave");

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(outboxEventWriter).write(any(UUID.class), eq("Participant.Joined"), captor.capture());
        var payload = (com.rally.participation.event.ParticipantJoinedPayload) captor.getValue();
        assertThat(payload.productId()).isEqualTo(productId);
        assertThat(payload.price()).isEqualByComparingTo(BigDecimal.valueOf(49.99));
        assertThat(payload.paymentMethodId()).isEqualTo("pm_abc");
        assertThat(payload.address()).isEqualTo("456 Oak Ave");
    }

    @Test
    void join_alreadyActiveParticipant_throwsWithoutCallingDealService() {
        when(participationRepository.findByDealIdAndUserIdAndStatus(dealId, userId, ParticipationStatus.ACTIVE))
            .thenReturn(Optional.of(new Participation(dealId, userId, null)));

        assertThatThrownBy(() -> service.join(dealId, userId, null, "pm_1", "addr"))
            .isInstanceOf(AlreadyActiveParticipantException.class);

        verifyNoInteractions(dealServiceClient);
        verifyNoInteractions(outboxEventWriter);
    }

    @Test
    void join_dealServiceRejectsCapacity_throwsAndNeverInsertsRow() {
        when(participationRepository.findByDealIdAndUserIdAndStatus(dealId, userId, ParticipationStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(dealServiceClient.getDealSummary(dealId)).thenReturn(stubDealSummary());
        doThrow(new DealNotJoinableException(dealId, "full")).when(dealServiceClient).reserveSlot(dealId);

        assertThatThrownBy(() -> service.join(dealId, userId, null, "pm_1", "addr"))
            .isInstanceOf(DealNotJoinableException.class);

        verify(participationRepository, never()).save(any());
        verifyNoInteractions(outboxEventWriter);
    }

    @Test
    void join_concurrentRaceOnUniqueIndex_translatesToAlreadyActiveException() {
        when(participationRepository.findByDealIdAndUserIdAndStatus(dealId, userId, ParticipationStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(dealServiceClient.getDealSummary(dealId)).thenReturn(stubDealSummary());
        when(participationRepository.save(any(Participation.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> service.join(dealId, userId, null, "pm_1", "addr"))
            .isInstanceOf(AlreadyActiveParticipantException.class);
    }

    @Test
    void join_withValidReferralCode_setsReferredByFromLink() {
        UUID referrerId = UUID.randomUUID();
        ReferralLink link = new ReferralLink("aB3xQ9zK", dealId, referrerId, null);

        when(referralLinkRepository.findById("aB3xQ9zK")).thenReturn(Optional.of(link));
        when(participationRepository.findByDealIdAndUserIdAndStatus(dealId, userId, ParticipationStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(dealServiceClient.getDealSummary(dealId)).thenReturn(stubDealSummary());
        when(participationRepository.save(any(Participation.class))).thenAnswer(inv -> inv.getArgument(0));

        ParticipationResponse response = service.join(dealId, userId, "aB3xQ9zK", "pm_1", "addr");

        assertThat(response.referredBy()).isEqualTo(referrerId);
    }

    @Test
    void join_withExpiredReferralCode_throwsAndNeverReservesSlot() {
        ReferralLink expiredLink = new ReferralLink("expired1", dealId, UUID.randomUUID(), Instant.now().minusSeconds(60));
        when(referralLinkRepository.findById("expired1")).thenReturn(Optional.of(expiredLink));

        assertThatThrownBy(() -> service.join(dealId, userId, "expired1", "pm_1", "addr"))
            .isInstanceOf(ReferralCodeExpiredException.class);

        verifyNoInteractions(dealServiceClient);
    }

    @Test
    void join_withReferralCodeForDifferentDeal_throwsNotFound() {
        UUID otherDealId = UUID.randomUUID();
        ReferralLink link = new ReferralLink("wrongdeal", otherDealId, UUID.randomUUID(), null);
        when(referralLinkRepository.findById("wrongdeal")).thenReturn(Optional.of(link));

        assertThatThrownBy(() -> service.join(dealId, userId, "wrongdeal", "pm_1", "addr"))
            .isInstanceOf(ReferralCodeNotFoundException.class);
    }

    @Test
    void leave_success_flipsStatusAndPublishesEvent() {
        Participation active = new Participation(dealId, userId, null);
        when(participationRepository.findByDealIdAndUserIdAndStatus(dealId, userId, ParticipationStatus.ACTIVE))
            .thenReturn(Optional.of(active));

        service.leave(dealId, userId);

        assertThat(active.getStatus()).isEqualTo(ParticipationStatus.LEFT);
        assertThat(active.getLeftAt()).isNotNull();

        verify(dealServiceClient).checkLeaveEligible(dealId);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(outboxEventWriter).write(eq(active.getId()), eq("Participant.Left"), payloadCaptor.capture());
    }

    @Test
    void leave_noActiveParticipation_throwsNotFound() {
        when(participationRepository.findByDealIdAndUserIdAndStatus(dealId, userId, ParticipationStatus.ACTIVE))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.leave(dealId, userId))
            .isInstanceOf(NotActiveParticipantException.class);

        verifyNoInteractions(dealServiceClient);
    }

    @Test
    void leave_dealServiceRejectsEligibility_throwsAndDoesNotFlipStatus() {
        Participation active = new Participation(dealId, userId, null);
        when(participationRepository.findByDealIdAndUserIdAndStatus(dealId, userId, ParticipationStatus.ACTIVE))
            .thenReturn(Optional.of(active));
        doThrow(new LeaveNotEligibleException(dealId, "inside cutoff window")).when(dealServiceClient).checkLeaveEligible(dealId);

        assertThatThrownBy(() -> service.leave(dealId, userId))
            .isInstanceOf(LeaveNotEligibleException.class);

        assertThat(active.getStatus()).isEqualTo(ParticipationStatus.ACTIVE);
        verifyNoInteractions(outboxEventWriter);
    }

    @Test
    void getProgress_combinesLocalCountWithDealServiceSummary() {
        when(participationRepository.countByDealIdAndStatus(dealId, ParticipationStatus.ACTIVE)).thenReturn(42L);
        java.time.Instant endTime = java.time.Instant.now().plusSeconds(3600);
        when(dealServiceClient.getDealSummary(dealId))
            .thenReturn(new DealSummaryResponse(dealId, productId, "ACTIVE", 10, 100, 42, BigDecimal.valueOf(29.99), endTime));

        var progress = service.getProgress(dealId);

        assertThat(progress.activeParticipants()).isEqualTo(42L);
        assertThat(progress.minParticipants()).isEqualTo(10);
        assertThat(progress.stockCap()).isEqualTo(100);
        assertThat(progress.endTime()).isEqualTo(endTime);
        assertThat(progress.timeRemaining()).isNotNull();
        assertThat(progress.timeRemaining().isNegative()).isFalse();
    }

    @Test
    void getProgress_pastEndTime_clampsTimeRemainingToZero() {
        when(participationRepository.countByDealIdAndStatus(dealId, ParticipationStatus.ACTIVE)).thenReturn(5L);
        java.time.Instant pastEndTime = java.time.Instant.now().minusSeconds(60);
        when(dealServiceClient.getDealSummary(dealId))
            .thenReturn(new DealSummaryResponse(dealId, productId, "EXPIRED", 10, 100, 5, BigDecimal.valueOf(29.99), pastEndTime));

        var progress = service.getProgress(dealId);

        assertThat(progress.timeRemaining()).isEqualTo(java.time.Duration.ZERO);
    }
}
