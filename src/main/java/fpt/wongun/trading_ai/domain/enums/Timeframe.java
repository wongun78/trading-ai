package fpt.wongun.trading_ai.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum Timeframe {
    M1("1m", "1 Minute", 60),
    M5("5m", "5 Minutes", 300),
    M15("15m", "15 Minutes", 900),
    M30("30m", "30 Minutes", 1800),
    H1("1h", "1 Hour", 3600),
    H4("4h", "4 Hours", 14400),
    D1("1d", "1 Day", 86400),
    W1("1w", "1 Week", 604800);

    private final String binanceInterval;
    private final String displayName;
    private final int seconds;

    public static Timeframe fromBinanceInterval(String interval) {
        return Arrays.stream(values())
            .filter(tf -> tf.binanceInterval.equals(interval))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Invalid Binance interval: " + interval));
    }

    public static Timeframe fromString(String timeframe) {
        if (timeframe == null) {
            return M5; // Default
        }
        try {
            return Timeframe.valueOf(timeframe.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid timeframe: " + timeframe + 
                ". Valid values: M1, M5, M15, M30, H1, H4, D1, W1");
        }
    }
}
