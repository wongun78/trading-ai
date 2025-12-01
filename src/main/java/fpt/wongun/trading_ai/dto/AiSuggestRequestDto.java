package fpt.wongun.trading_ai.dto;

import fpt.wongun.trading_ai.domain.enums.TradingMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for AI signal generation.
 * Contains parameters needed to analyze market and generate trading signals.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSuggestRequestDto {

    @NotBlank(message = "Symbol code is required")
    private String symbolCode;

    @NotBlank(message = "Timeframe is required (e.g., M5, M15, H1)")
    private String timeframe; // e.g. M5, M15, H1

    @NotNull(message = "Trading mode is required (SCALPING, INTRADAY, or SWING)")
    private TradingMode mode;

    /**
     * Number of candles to analyze.
     * If not provided, uses mode's default candle count.
     */
    @Min(value = 20, message = "Minimum 20 candles required for analysis")
    @Max(value = 500, message = "Maximum 500 candles allowed")
    private Integer candleCount;

    /**
     * Get candle count based on mode if not explicitly set
     */
    public int getEffectiveCandleCount() {
        if (candleCount != null) {
            return candleCount;
        }
        return mode != null ? mode.getCandleCount() : TradingMode.SCALPING.getCandleCount();
    }
}
