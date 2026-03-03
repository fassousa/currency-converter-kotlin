-- V3__add_constraints.sql
-- Add business logic constraints to transactions table.

-- Ensure amounts are positive
ALTER TABLE transactions
ADD CONSTRAINT check_source_amount_positive CHECK (source_amount > 0),
ADD CONSTRAINT check_target_amount_positive CHECK (target_amount > 0),
ADD CONSTRAINT check_exchange_rate_positive CHECK (exchange_rate > 0);

-- Ensure currency codes are 3 uppercase letters
ALTER TABLE transactions
ADD CONSTRAINT check_source_currency_format CHECK (source_currency ~ '^[A-Z]{3}$'),
ADD CONSTRAINT check_target_currency_format CHECK (target_currency ~ '^[A-Z]{3}$');

-- Ensure source and target currencies differ
ALTER TABLE transactions
ADD CONSTRAINT check_currencies_differ CHECK (source_currency <> target_currency);

