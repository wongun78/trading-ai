-- V3: Create positions table for trade execution tracking
-- Author: Trading AI System
-- Date: 2025-12-01

-- Create positions table
CREATE TABLE IF NOT EXISTS positions (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,
    
    -- Foreign keys
    signal_id BIGINT REFERENCES ai_signals(id) ON DELETE SET NULL,
    symbol_id BIGINT NOT NULL REFERENCES symbols(id) ON DELETE CASCADE,
    
    -- Position info
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'OPEN', 'CLOSED', 'CANCELLED')),
    direction VARCHAR(10) NOT NULL CHECK (direction IN ('LONG', 'SHORT', 'NEUTRAL')),
    
    -- Prices
    planned_entry_price DECIMAL(28, 18) NOT NULL,
    actual_entry_price DECIMAL(28, 18),
    stop_loss DECIMAL(28, 18) NOT NULL,
    take_profit1 DECIMAL(28, 18),
    take_profit2 DECIMAL(28, 18),
    take_profit3 DECIMAL(28, 18),
    exit_price DECIMAL(28, 18),
    
    -- Position size & P&L
    quantity DECIMAL(28, 8) NOT NULL CHECK (quantity > 0),
    realized_pn_l DECIMAL(28, 8),
    realized_pn_l_percent DECIMAL(10, 4),
    actual_risk_reward DECIMAL(10, 2),
    
    -- Exit info
    exit_reason VARCHAR(30) CHECK (exit_reason IN ('TP1_HIT', 'TP2_HIT', 'TP3_HIT', 'SL_HIT', 'MANUAL_EXIT', 'TIME_EXIT', 'TRAILING_STOP', 'RISK_MANAGEMENT')),
    
    -- Timestamps
    opened_at TIMESTAMP,
    closed_at TIMESTAMP,
    
    -- Trading costs & metrics
    fees DECIMAL(28, 8) DEFAULT 0,
    slippage DECIMAL(10, 4),
    duration_ms BIGINT,
    
    -- Notes
    notes TEXT,
    
    -- BaseEntity auditing fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100),
    version BIGINT DEFAULT 0,
    
    -- Soft delete
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_position_status ON positions(status) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_position_symbol ON positions(symbol_id) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_position_opened_at ON positions(opened_at DESC) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_position_user ON positions(created_by, status) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_position_signal ON positions(signal_id) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_position_direction ON positions(direction, status) WHERE deleted = false;

-- Create trigger to auto-update updated_at
CREATE OR REPLACE FUNCTION update_positions_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_positions_updated_at
    BEFORE UPDATE ON positions
    FOR EACH ROW
    EXECUTE FUNCTION update_positions_updated_at();

-- Add comments for documentation
COMMENT ON TABLE positions IS 'Trading positions for tracking actual trade execution and P&L';
COMMENT ON COLUMN positions.signal_id IS 'Reference to AI signal that triggered this position (null for manual trades)';
COMMENT ON COLUMN positions.status IS 'Current position status: PENDING, OPEN, CLOSED, CANCELLED';
COMMENT ON COLUMN positions.planned_entry_price IS 'Target entry price from signal or manual plan';
COMMENT ON COLUMN positions.actual_entry_price IS 'Actual filled entry price';
COMMENT ON COLUMN positions.realized_pn_l IS 'Realized profit/loss in quote currency (positive = profit)';
COMMENT ON COLUMN positions.exit_reason IS 'Why position was closed: TP1/2/3_HIT, SL_HIT, MANUAL_EXIT, etc.';
COMMENT ON COLUMN positions.slippage IS 'Slippage percentage between planned and actual entry';
COMMENT ON COLUMN positions.duration_ms IS 'Trade duration in milliseconds';
