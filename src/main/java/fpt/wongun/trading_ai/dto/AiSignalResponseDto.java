package fpt.wongun.trading_ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fpt.wongun.trading_ai.domain.enums.Direction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSignalResponseDto {

    private Long id;
    private String symbolCode;
    private String timeframe;
    private Direction direction;
    private BigDecimal entryPrice;
    private BigDecimal stopLoss;
    private BigDecimal takeProfit;
    private BigDecimal riskReward;
    private String reasoning;
    private Instant createdAt;

    @JsonProperty("isActionable")
    public boolean isActionable() {
        return direction != null && direction != Direction.NEUTRAL;
    }

    @JsonProperty("potentialProfit")
    public BigDecimal getPotentialProfit() {
        if (entryPrice != null && takeProfit != null) {
            return takeProfit.subtract(entryPrice).abs();
        }
        return null;
    }

    @JsonProperty("riskAmount")
    public BigDecimal getRiskAmount() {
        if (entryPrice != null && stopLoss != null) {
            return entryPrice.subtract(stopLoss).abs();
        }
        return null;
    }
}
