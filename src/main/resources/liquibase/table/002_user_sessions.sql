CREATE TABLE user_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT NOT NULL,
    refresh_token_hash TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    ip TEXT,
    device_id TEXT,
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER user_session_modified_trigger
BEFORE UPDATE ON user_sessions
FOR EACH ROW EXECUTE FUNCTION update_modified_at();