package com.rally.participation.repository;

import com.rally.participation.domain.Participation;
import com.rally.participation.domain.ParticipationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParticipationRepository extends JpaRepository<Participation, UUID> {

    Optional<Participation> findByDealIdAndUserIdAndStatus(UUID dealId, UUID userId, ParticipationStatus status);

    Page<Participation> findByDealIdAndStatus(UUID dealId, ParticipationStatus status, Pageable pageable);

    Page<Participation> findByDealId(UUID dealId, Pageable pageable);

    long countByDealIdAndStatus(UUID dealId, ParticipationStatus status);

    List<Participation> findTop50ByDealIdOrderByJoinedAtDesc(UUID dealId);

    boolean existsByDealIdAndIdAndStatus(UUID dealId, UUID participantId, ParticipationStatus status);

    Optional<Participation> findByDealIdAndId(UUID dealId, UUID id);

    /**
     * Guarded flip used by the async order.deal_order_cancelled consumer (docs §5.3).
     * Only affects rows still ACTIVE, making redelivery idempotent.
     */
    @Modifying
    @Query("UPDATE Participation p SET p.status = 'ACTIVE' " +
            "WHERE p.dealId = :dealId AND p.userId = :userId AND p.status = 'PENDING'")
    int flipToActiveIfPending(@Param("dealId") UUID dealId, @Param("userId") UUID userId);

    /**
     * Guarded flip used by the async order.deal_order_cancelled consumer (docs §5.3).
     * Only affects rows still ACTIVE, making redelivery idempotent. Covers cancellation
     * arriving after the participant had already joined (e.g. the deal itself failed).
     */
    @Modifying
    @Query("UPDATE Participation p SET p.status = 'LEFT', p.leftAt = :leftAt " +
           "WHERE p.dealId = :dealId AND p.userId = :userId AND p.status = 'ACTIVE'")
    int flipToLeftIfActive(@Param("dealId") UUID dealId, @Param("userId") UUID userId, @Param("leftAt") Instant leftAt);

    /**
     * Guarded flip used by the async order.deal_order_cancelled consumer (docs §5.3).
     * Only affects rows still PENDING, making redelivery idempotent. Covers cancellation
     * arriving before authorization completed (e.g. payment declined).
     */
    @Modifying
    @Query("UPDATE Participation p SET p.status = 'DECLINED' " +
           "WHERE p.dealId = :dealId AND p.userId = :userId AND p.status = 'PENDING'")
    int flipToDeclinedIfPending(@Param("dealId") UUID dealId, @Param("userId") UUID userId);
}
