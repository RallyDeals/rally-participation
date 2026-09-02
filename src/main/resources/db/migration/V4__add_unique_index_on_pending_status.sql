DROP INDEX IF EXISTS uq_participations_active_per_user_deal;

CREATE UNIQUE INDEX uq_participations_pending_or_active_per_user_deal
    ON participations (deal_id, user_id)
    WHERE status IN ('PENDING', 'ACTIVE');
