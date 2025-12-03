package fpt.wongun.trading_ai.dto;

import fpt.wongun.trading_ai.domain.enums.Direction;
import fpt.wongun.trading_ai.domain.enums.ExitReason;
import fpt.wongun.trading_ai.domain.enums.PositionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for Position entity.
 * Contains full position details and P&L information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionResponseDto {

    private Long id;
    private Long signalId;
    private String symbolCode;
    private PositionStatus status;
    private Direction direction;

    // Prices
    private BigDecimal plannedEntryPrice;
    private BigDecimal actualEntryPrice;
    private BigDecimal stopLoss;
    private BigDecimal takeProfit1;
    private BigDecimal takeProfit2;
    private BigDecimal takeProfit3;
    private BigDecimal exitPrice;

    // Position details
    private BigDecimal quantity;
    private BigDecimal realizedPnL;
    private BigDecimal realizedPnLPercent;
    private BigDecimal actualRiskReward;

    // Exit info
    private ExitReason exitReason;
    private Instant openedAt;
    private Instant closedAt;
    
    // Costs & metrics
    private BigDecimal fees;
    private BigDecimal slippage;
    private Long durationMs;
    private String notes;

    // Audit fields
    private Instant createdAt;
    private String createdBy;
}
