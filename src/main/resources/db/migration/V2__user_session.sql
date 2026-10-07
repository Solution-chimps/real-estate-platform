CREATE TABLE user_session (
    id           CHAR(36)     NOT NULL,
    user_id      CHAR(36)     NOT NULL,
    token_hash   CHAR(64)     NOT NULL,
    user_agent   VARCHAR(255),
    created_at   ${timestampType} NOT NULL,
    last_seen_at ${timestampType} NOT NULL,
    expires_at   ${timestampType} NOT NULL,
    revoked_at   ${timestampType},
    CONSTRAINT pk_user_session PRIMARY KEY (id),
    CONSTRAINT uk_user_session_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_user_session_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_session_user ON user_session (user_id, revoked_at, expires_at);
CREATE INDEX idx_user_session_expires ON user_session (expires_at);
