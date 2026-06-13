ALTER TABLE refresh_token
    ADD COLUMN family_id VARCHAR(64) NULL AFTER jti,
    ADD COLUMN token_sequence BIGINT NOT NULL DEFAULT 0 AFTER family_id,
    ADD COLUMN rotated_at DATETIME(6) NULL AFTER revoked_at,
    ADD COLUMN rotation_grace_until DATETIME(6) NULL AFTER rotated_at,
    ADD COLUMN reuse_detected_at DATETIME(6) NULL AFTER rotation_grace_until;

UPDATE refresh_token
SET family_id = jti
WHERE family_id IS NULL;

ALTER TABLE refresh_token
    MODIFY family_id VARCHAR(64) NOT NULL;

CREATE INDEX idx_refresh_family_id
    ON refresh_token (family_id);

CREATE INDEX idx_refresh_family_active
    ON refresh_token (family_id, revoked, token_sequence);

CREATE INDEX idx_refresh_rotation_grace
    ON refresh_token (family_id, rotation_grace_until);
