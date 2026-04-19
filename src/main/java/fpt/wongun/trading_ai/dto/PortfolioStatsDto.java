package fpt.wongun.trading_ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioStatsDto {

    // Position counts
    private Long totalPositions;
    private Long openPositions;
    private Long closedPositions;
    private Long pendingPositions;

    // P&L metrics
    private BigDecimal totalPnL;
    private BigDecimal averagePnL;
    private BigDecimal bestTradePnL;
    private BigDecimal worstTradePnL;

    // Performance metrics
    private Double winRate;              
    private Double longWinRate;          
    private Double shortWinRate;         
    private BigDecimal averageRiskReward;
    private BigDecimal totalFees;

    // Symbol breakdown
    private BigDecimal btcPnL;
    private BigDecimal ethPnL;

    // Risk metrics
    private Integer consecutiveWins;
    private Integer consecutiveLosses;
    private Integer maxConsecutiveWins;
    private Integer maxConsecutiveLosses;

    // Time metrics
    private Long averageTradeDurationMs;
    private Long shortestTradeMs;
    private Long longestTradeMs;
}
