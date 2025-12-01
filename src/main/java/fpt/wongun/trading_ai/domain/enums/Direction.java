package fpt.wongun.trading_ai.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Trading direction with display metadata for frontend.
 */
@Getter
@AllArgsConstructor
public enum Direction {
    /**
     * Buy position - expecting price to increase
     */
    LONG("Buy", "Long", "↑", "#22c55e"),
    
    /**
     * Sell position - expecting price to decrease
     */
    SHORT("Sell", "Short", "↓", "#ef4444"),
    
    /**
     * No clear setup - wait for better opportunity
     */
    NEUTRAL("Wait", "Neutral", "→", "#94a3b8");

    private final String action;
    private final String displayName;
    private final String arrow;
    private final String color;  // Hex color for frontend
}
