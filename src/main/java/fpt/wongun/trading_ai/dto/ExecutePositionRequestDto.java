package fpt.wongun.trading_ai.dto;

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
public class ExecutePositionRequestDto {

    @NotNull(message = "Actual entry price is required")
    @Positive(message = "Actual entry price must be positive")
    private BigDecimal actualEntryPrice;
}
