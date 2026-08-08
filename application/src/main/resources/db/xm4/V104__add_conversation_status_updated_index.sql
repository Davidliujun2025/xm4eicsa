CREATE INDEX idx_conversation_status_updated_at
    ON conversation (status, updated_at);
