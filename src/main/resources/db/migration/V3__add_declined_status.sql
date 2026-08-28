ALTER TABLE participations
DROP CONSTRAINT chk_participations_status;

ALTER TABLE participations
    ADD CONSTRAINT chk_participations_status
        CHECK (status IN ('ACTIVE', 'PENDING', 'LEFT', 'DECLINED'));
