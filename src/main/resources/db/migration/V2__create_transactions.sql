-- V2__create_transactions.sql
-- Create transactions table with foreign key to users, idempotency key, and exchange rate tracking.

CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users ON DELETE RESTRICT,
    idempotency_key UUID NOT NULL UNIQUE,
    source_currency CHAR(3) NOT NULL,
    source_amount NUMERIC(19,4) NOT NULL,
    target_currency CHAR(3) NOT NULL,
    target_amount NUMERIC(19,4) NOT NULL,
    exchange_rate NUMERIC(27,8) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index on user_id for fast filtering by user
CREATE INDEX idx_transactions_user_id ON transactions(user_id);

-- Index on idempotency_key (already unique, but listed for clarity)
CREATE INDEX idx_transactions_idempotency_key ON transactions(idempotency_key);

-- Composite index on user_id + created_at for paginated queries
CREATE INDEX idx_transactions_user_id_created_at ON transactions(user_id, created_at DESC);

