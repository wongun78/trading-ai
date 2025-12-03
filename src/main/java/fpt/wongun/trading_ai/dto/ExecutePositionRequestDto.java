package fpt.wongun.trading_ai.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for executing a pending position
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutePositionRequestDto {

    /**
     * Actual entry price (filled price)
     */
    @NotNull(message = "Actual entry price is required")
    @Positive(message = "Actual entry price must be positive")
    private BigDecimal actualEntryPrice;
}
