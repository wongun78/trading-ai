package fpt.wongun.trading_ai.domain.enums;

/**
 * Status of a trading position.
 * Lifecycle: PENDING → OPEN → CLOSED
 * Or: PENDING → CANCELLED
 */
public enum PositionStatus {
    /**
     * Position created but not yet executed (waiting for entry price to be hit)
     */
    PENDING,
    
    /**
     * Position is currently open and active
     */
    OPEN,
    
    /**
     * Position has been closed (via TP, SL, or manual exit)
     */
    CLOSED,
    
    /**
     * Position was cancelled before execution
     */
    CANCELLED
}
