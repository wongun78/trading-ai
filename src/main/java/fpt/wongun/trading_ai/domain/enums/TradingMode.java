package fpt.wongun.trading_ai.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TradingMode {
    
    SCALPING(50, "Scalping", "Quick trades with tight SL/TP"),

    INTRADAY(100, "Intraday", "Hold positions within the day"),

    SWING(200, "Swing Trading", "Multi-day position holding");

    private final int candleCount;
    private final String displayName;
    private final String description;

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
