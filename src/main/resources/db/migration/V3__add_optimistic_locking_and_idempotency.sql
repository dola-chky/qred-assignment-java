-- Add version columns for optimistic locking
ALTER TABLE clients
ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

ALTER TABLE contracts
ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- Add idempotency key to payments table
ALTER TABLE payments
ADD COLUMN idempotency_key VARCHAR(36) NOT NULL UNIQUE,
ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- Create index on idempotency_key for faster duplicate detection
CREATE INDEX idx_payments_idempotency_key ON payments (idempotency_key);

