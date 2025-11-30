package fpt.wongun.trading_ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for bulk importing candle data.
 */
@Data
public class CandleImportDto {

    @NotBlank(message = "Symbol code is required")
    private String symbolCode;

    @NotBlank(message = "Timeframe is required")
    private String timeframe;

    @NotNull(message = "Timestamp is required")
    private Instant timestamp;

    @NotNull(message = "Open price is required")
    private BigDecimal open;

    @NotNull(message = "High price is required")
    private BigDecimal high;

    @NotNull(message = "Low price is required")
    private BigDecimal low;

    @NotNull(message = "Close price is required")
    private BigDecimal close;

    @NotNull(message = "Volume is required")
    private BigDecimal volume;
}
