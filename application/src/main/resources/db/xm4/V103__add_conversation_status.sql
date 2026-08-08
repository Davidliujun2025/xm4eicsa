ALTER TABLE conversation
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX idx_conversation_customer_status
    ON conversation (customer_id, status);
