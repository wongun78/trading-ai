-- Add BaseEntity auditing columns to existing tables
-- This migration adds created_at, updated_at, last_modified_by, version columns
-- to support enterprise-grade auditing and soft delete functionality

-- Add columns to symbols table
ALTER TABLE symbols
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(100),
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Add columns to ai_signals table
ALTER TABLE ai_signals
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(100),
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Add columns to candles table
ALTER TABLE candles
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(100),
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Create update trigger function for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create triggers to auto-update updated_at on changes
DROP TRIGGER IF EXISTS update_symbols_updated_at ON symbols;
CREATE TRIGGER update_symbols_updated_at
    BEFORE UPDATE ON symbols
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_ai_signals_updated_at ON ai_signals;
CREATE TRIGGER update_ai_signals_updated_at
    BEFORE UPDATE ON ai_signals
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_candles_updated_at ON candles;
CREATE TRIGGER update_candles_updated_at
    BEFORE UPDATE ON candles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add indexes for common queries
CREATE INDEX IF NOT EXISTS idx_symbols_created_at ON symbols(created_at);
CREATE INDEX IF NOT EXISTS idx_ai_signals_created_at ON ai_signals(created_at);
CREATE INDEX IF NOT EXISTS idx_candles_created_at ON candles(created_at);

COMMENT ON COLUMN symbols.created_at IS 'Timestamp when the record was created';
COMMENT ON COLUMN symbols.updated_at IS 'Timestamp when the record was last updated';
COMMENT ON COLUMN symbols.last_modified_by IS 'User who last modified the record';
COMMENT ON COLUMN symbols.version IS 'Version number for optimistic locking';
