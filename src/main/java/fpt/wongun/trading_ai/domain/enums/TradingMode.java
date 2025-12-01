package fpt.wongun.trading_ai.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Trading modes with different candle count requirements.
 * Based on Bob Volman methodology for different trading timeframes.
 */
@Getter
@AllArgsConstructor
public enum TradingMode {
    /**
     * Fast scalping with tight stops and quick exits.
     * Analyzes 50 recent candles for immediate opportunities.
     */
    SCALPING(50, "Scalping", "Quick trades with tight SL/TP"),

    /**
     * Intraday trading with wider stops.
     * Analyzes 100 candles for better trend confirmation.
     */
    INTRADAY(100, "Intraday", "Hold positions within the day"),

    /**
     * Swing trading with longer holding period.
     * Analyzes 200 candles for major trend moves.
     */
    SWING(200, "Swing Trading", "Multi-day position holding");

    private final int candleCount;
    private final String displayName;
    private final String description;

    /**
     * Get mode by name (case-insensitive)
     */
    public static TradingMode fromString(String mode) {
        if (mode == null) {
            return SCALPING; // Default
        }
        try {
            return TradingMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid trading mode: " + mode + 
                ". Valid values: SCALPING, INTRADAY, SWING");
        }
    }
}
