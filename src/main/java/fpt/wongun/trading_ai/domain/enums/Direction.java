package fpt.wongun.trading_ai.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Direction {
    
    LONG("Buy", "Long", "↑", "#22c55e"),

    SHORT("Sell", "Short", "↓", "#ef4444"),

    NEUTRAL("Wait", "Neutral", "→", "#94a3b8");

    private final String action;
    private final String displayName;
    private final String arrow;
    private final String color;  // Hex color for frontend
}
