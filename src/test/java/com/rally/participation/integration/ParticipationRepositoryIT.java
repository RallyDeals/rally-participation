package com.rally.participation.integration;

import com.rally.participation.domain.Participation;
import com.rally.participation.domain.ParticipationStatus;
import com.rally.participation.repository.ParticipationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies FR-021 (one active participation per user per deal) and FR-021a (rejoin
 * allowed after leaving) against the real Postgres partial unique index defined in
 * V1__init.sql. This is exactly the kind of constraint that's worth testing against
 * the real database engine rather than H2's approximation of it.
 */
class ParticipationRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private ParticipationRepository participationRepository;

    @Test
    void rejectsSecondActiveParticipationForSameUserAndDeal() {
        UUID dealId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        participationRepository.saveAndFlush(new Participation(dealId, userId, null));

        Participation duplicate = new Participation(dealId, userId, null);
        assertThatThrownBy(() -> participationRepository.saveAndFlush(duplicate))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsRejoinAfterLeaving() {
        UUID dealId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Participation first = participationRepository.saveAndFlush(new Participation(dealId, userId, null));
        first.markLeft();
        participationRepository.saveAndFlush(first);

        Participation rejoin = participationRepository.saveAndFlush(new Participation(dealId, userId, null));

        assertThat(rejoin.getId()).isNotNull();
        assertThat(participationRepository.countByDealIdAndStatus(dealId, ParticipationStatus.ACTIVE)).isEqualTo(1);
        assertThat(participationRepository.countByDealIdAndStatus(dealId, ParticipationStatus.LEFT)).isEqualTo(1);
    }

    @Test
    void allowsSameUserActiveInDifferentDeals() {
        UUID userId = UUID.randomUUID();
        UUID dealA = UUID.randomUUID();
        UUID dealB = UUID.randomUUID();

        participationRepository.saveAndFlush(new Participation(dealA, userId, null));
        participationRepository.saveAndFlush(new Participation(dealB, userId, null));

        assertThat(participationRepository.countByDealIdAndStatus(dealA, ParticipationStatus.ACTIVE)).isEqualTo(1);
        assertThat(participationRepository.countByDealIdAndStatus(dealB, ParticipationStatus.ACTIVE)).isEqualTo(1);
    }

    @Test
    void allowsDifferentUsersActiveInSameDeal() {
        UUID dealId = UUID.randomUUID();
        participationRepository.saveAndFlush(new Participation(dealId, UUID.randomUUID(), null));
        participationRepository.saveAndFlush(new Participation(dealId, UUID.randomUUID(), null));

        assertThat(participationRepository.countByDealIdAndStatus(dealId, ParticipationStatus.ACTIVE)).isEqualTo(2);
    }
}
