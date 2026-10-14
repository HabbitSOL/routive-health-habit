CREATE TABLE auth_sessions (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    KEY ix_auth_sessions_user (user_id, revoked_at, expires_at),
    CONSTRAINT fk_auth_sessions_user FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Pre-session tokens cannot be safely associated with a login session. Require login again.
UPDATE refresh_tokens SET revoked_at = CURRENT_TIMESTAMP(6) WHERE revoked_at IS NULL;
ALTER TABLE refresh_tokens ADD COLUMN session_id BINARY(16) NULL;
ALTER TABLE refresh_tokens ADD KEY ix_refresh_tokens_session (session_id);
ALTER TABLE refresh_tokens ADD CONSTRAINT fk_refresh_tokens_session
    FOREIGN KEY (session_id) REFERENCES auth_sessions(id) ON UPDATE RESTRICT ON DELETE RESTRICT;
