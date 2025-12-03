package fpt.wongun.trading_ai.domain.enums;

/**
 * Reason for closing a position.
 * Used for analytics and performance tracking.
 */
public enum ExitReason {
    /**
     * Take Profit 1 was hit
     */
    TP1_HIT,
    
    /**
     * Take Profit 2 was hit
     */
    TP2_HIT,
    
    /**
     * Take Profit 3 was hit
     */
    TP3_HIT,
    
    /**
     * Stop Loss was hit
     */
    SL_HIT,
    
    /**
     * Position manually closed by user
     */
    MANUAL_EXIT,
    
    /**
     * Position closed due to time-based rule (e.g., end of day)
     */
    TIME_EXIT,
    
    /**
     * Position closed due to trailing stop
     */
    TRAILING_STOP,
    
    /**
     * Position closed due to risk management rule
     */
    RISK_MANAGEMENT
}
