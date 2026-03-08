-- V4__fix_currency_columns_char_to_varchar.sql
-- Hibernate validates @Column(length=3) on String as VARCHAR(3).
-- PostgreSQL CHAR(3) is stored as bpchar, causing schema-validation failure on startup.
-- This migration converts both currency columns from CHAR(3) to VARCHAR(3).

ALTER TABLE transactions
    ALTER COLUMN source_currency TYPE VARCHAR(3),
    ALTER COLUMN target_currency TYPE VARCHAR(3);

