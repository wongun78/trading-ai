package fpt.wongun.trading_ai.dto;

import fpt.wongun.trading_ai.domain.enums.Direction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenPositionRequestDto {

    private Long signalId;

    @NotNull(message = "Symbol code is required")
    private String symbolCode;

    @NotNull(message = "Direction is required")
    private Direction direction;

    @NotNull(message = "Planned entry price is required")
    @Positive(message = "Planned entry price must be positive")
    private BigDecimal plannedEntryPrice;

    @NotNull(message = "Stop loss is required")
    @Positive(message = "Stop loss must be positive")
    private BigDecimal stopLoss;

    private BigDecimal takeProfit1;

    private BigDecimal takeProfit2;

    private BigDecimal takeProfit3;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    private String notes;
}
