ALTER TABLE refresh_token
    MODIFY refresh VARCHAR(512) NULL,
    ADD COLUMN refresh_token_hash VARCHAR(64) NULL AFTER refresh,
    ADD COLUMN revoked_reason VARCHAR(100) NULL AFTER revoked,
    ADD COLUMN revoked_at DATETIME(6) NULL AFTER revoked_reason,
    ADD COLUMN revoked_by VARCHAR(100) NULL AFTER revoked_at;

UPDATE refresh_token
SET refresh_token_hash = LOWER(SHA2(refresh, 256))
WHERE refresh_token_hash IS NULL
  AND refresh IS NOT NULL;

CREATE UNIQUE INDEX idx_refresh_token_hash
    ON refresh_token (refresh_token_hash);
