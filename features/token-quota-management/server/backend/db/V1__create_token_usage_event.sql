CREATE TABLE IF NOT EXISTS token_usage_event (
    idempotency_key        VARCHAR(160) PRIMARY KEY,
    user_id                VARCHAR(128) NOT NULL,
    conversation_id        VARCHAR(160),
    request_id             VARCHAR(160) NOT NULL UNIQUE,
    provider               VARCHAR(32) NOT NULL,
    model                  VARCHAR(160) NOT NULL,
    input_content_length   BIGINT NOT NULL DEFAULT 0 CHECK (input_content_length >= 0),
    output_content_length  BIGINT NOT NULL DEFAULT 0 CHECK (output_content_length >= 0),
    input_tokens           BIGINT NOT NULL CHECK (input_tokens >= 0),
    output_tokens          BIGINT NOT NULL CHECK (output_tokens >= 0),
    cached_input_tokens    BIGINT NOT NULL DEFAULT 0 CHECK (cached_input_tokens >= 0),
    total_tokens           BIGINT NOT NULL CHECK (total_tokens >= input_tokens + output_tokens),
    response_time_ms       BIGINT NOT NULL CHECK (response_time_ms >= 0),
    status                 VARCHAR(16) NOT NULL,
    occurred_at            TIMESTAMPTZ NOT NULL,
    provider_reported_cost NUMERIC(24, 10) NOT NULL DEFAULT 0,
    cost_currency          VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
    created_at             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_token_usage_user_time
    ON token_usage_event (user_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_token_usage_user_provider_time
    ON token_usage_event (user_id, provider, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_token_usage_user_model_time
    ON token_usage_event (user_id, model, occurred_at DESC);
