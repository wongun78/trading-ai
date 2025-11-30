package fpt.wongun.trading_ai.dto;

import fpt.wongun.trading_ai.domain.enums.Direction;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class AiSignalResponseDto {

    private Long id;
    private String symbolCode;
    private String timeframe;
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
    private Instant createdAt;
}
