package fpt.wongun.trading_ai.dto;

import fpt.wongun.trading_ai.domain.enums.ExitReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for closing an open position.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClosePositionRequestDto {

    /**
     * Exit price (actual close price)
     */
    @NotNull(message = "Exit price is required")
    @Positive(message = "Exit price must be positive")
    private BigDecimal exitPrice;

    /**
     * Reason for closing
     */
    @NotNull(message = "Exit reason is required")
    private ExitReason exitReason;

    /**
     * Trading fees (optional)
     */
    private BigDecimal fees;

    /**
     * Optional notes about the exit
     */
    private String notes;
}
