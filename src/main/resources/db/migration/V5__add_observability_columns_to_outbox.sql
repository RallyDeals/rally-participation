-- Persist the originating trace/correlation on each outbox row so the relay (OutboxPoller)
-- can re-parent its publish span back onto the trace that created the row
-- (see OBSERVABILITY_GUIDE.md §9-§10).
ALTER TABLE participation_outbox
    ADD COLUMN trace_id       VARCHAR(32) NULL,
    ADD COLUMN correlation_id UUID NULL;

CREATE INDEX idx_participation_outbox_correlation_id ON participation_outbox (correlation_id);
