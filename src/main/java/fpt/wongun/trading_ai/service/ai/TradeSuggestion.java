package fpt.wongun.trading_ai.service.ai;

import fpt.wongun.trading_ai.domain.enums.Direction;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TradeSuggestion {
    private Direction direction;
    private BigDecimal entryPrice;
    private BigDecimal stopLoss;
    private BigDecimal takeProfit1;
    private BigDecimal takeProfit2;
    private BigDecimal takeProfit3;
    private BigDecimal riskReward1;
    private BigDecimal riskReward2;
    private BigDecimal riskReward3;
    private String reasoning;
}
