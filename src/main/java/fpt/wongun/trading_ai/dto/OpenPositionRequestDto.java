package fpt.wongun.trading_ai.dto;

import fpt.wongun.trading_ai.domain.enums.Direction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for opening a new position.
 * Can be based on AI signal or manual entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenPositionRequestDto {

    /**
     * AI signal ID (optional - null for manual trades)
     */
    private Long signalId;

    /**
     * Symbol code (required)
     */
    @NotNull(message = "Symbol code is required")
    private String symbolCode;

    /**
     * Trade direction
     */
    @NotNull(message = "Direction is required")
    private Direction direction;

    /**
     * Planned entry price
     */
    @NotNull(message = "Planned entry price is required")
    @Positive(message = "Planned entry price must be positive")
    private BigDecimal plannedEntryPrice;

    /**
     * Stop loss price
     */
    @NotNull(message = "Stop loss is required")
    @Positive(message = "Stop loss must be positive")
    private BigDecimal stopLoss;

    /**
     * Take profit 1 price (optional)
     */
    private BigDecimal takeProfit1;

    /**
     * Take profit 2 price (optional)
     */
    private BigDecimal takeProfit2;

    /**
     * Take profit 3 price (optional)
     */
    private BigDecimal takeProfit3;

    /**
     * Position size (quantity)
     */
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    /**
     * Optional notes
     */
    private String notes;
}
