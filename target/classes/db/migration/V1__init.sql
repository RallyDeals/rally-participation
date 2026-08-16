CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE participations (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    deal_id      UUID NOT NULL,              -- FK by reference only (different service/DB)
    user_id      UUID NOT NULL,              -- FK by reference only
    referred_by  UUID NULL,                  -- user_id of referrer, nullable
    status       VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | LEFT
    joined_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    left_at      TIMESTAMPTZ NULL,

    CONSTRAINT chk_participations_status CHECK (status IN ('ACTIVE', 'LEFT'))
);

-- Enforce FR-021: a user can only hold ONE active participation per deal at a time.
CREATE UNIQUE INDEX uq_participations_active_per_user_deal
    ON participations (deal_id, user_id)
    WHERE status = 'ACTIVE';

-- Hot-path lookups
CREATE INDEX idx_participations_deal_id ON participations (deal_id);
CREATE INDEX idx_participations_deal_status ON participations (deal_id, status);
CREATE INDEX idx_participations_user_id ON participations (user_id);

-- Referral link persistence
CREATE TABLE referral_links (
    code               VARCHAR(16) PRIMARY KEY,
    deal_id            UUID NOT NULL,
    referrer_user_id   UUID NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at         TIMESTAMPTZ NULL
);

CREATE INDEX idx_referral_links_deal_id ON referral_links (deal_id);

-- Transactional outbox for reliable Kafka publication of participant.joined / participant.left
CREATE TABLE participation_outbox (
    id             BIGSERIAL PRIMARY KEY,
    aggregate_id   UUID NOT NULL,
    event_type     VARCHAR(50) NOT NULL,
    payload        TEXT NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ NULL
);

CREATE INDEX idx_participation_outbox_unpublished
    ON participation_outbox (created_at)
    WHERE published_at IS NULL;
