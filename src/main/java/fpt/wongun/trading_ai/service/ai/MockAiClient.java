package fpt.wongun.trading_ai.service.ai;

import fpt.wongun.trading_ai.domain.enums.Direction;
import fpt.wongun.trading_ai.service.analysis.TradeAnalysisContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Mock AI client for local development and testing.
 * 
 * This implementation provides simple rule-based trade suggestions
 * without calling external AI services. Useful for:
 * - Local development without API costs
 * - Integration testing
 * - Fallback when OpenAI is unavailable
 * 
 * Note: OpenAiClient is marked @Primary and will be used by default in production.
 * To use this mock instead, remove @Primary from OpenAiClient or use @Qualifier.
 */
@Component
public class MockAiClient implements AiClient {

    @Override
    public TradeSuggestion suggestTrade(TradeAnalysisContext context, String mode) {
        List<TradeAnalysisContext.CandlePoint> candles = context.getCandles();
        if (candles.isEmpty()) {
            // Return a mock signal with fake data when no candles available
            BigDecimal mockPrice = BigDecimal.valueOf(2000.00);
            BigDecimal delta = mockPrice.multiply(BigDecimal.valueOf(0.002));
            
            return TradeSuggestion.builder()
                    .direction(Direction.LONG)
                    .entryPrice(mockPrice)
                    .stopLoss(mockPrice.subtract(delta))
                    .takeProfit1(mockPrice.add(delta.multiply(BigDecimal.valueOf(1.5))))
                    .takeProfit2(mockPrice.add(delta.multiply(BigDecimal.valueOf(3.0))))
                    .riskReward1(BigDecimal.valueOf(1.5))
                    .riskReward2(BigDecimal.valueOf(3.0))
                    .reasoning("Mock signal - No candle data available. Using simulated prices for demonstration.")
                    .build();
        }

        BigDecimal lastClose = candles.get(candles.size() - 1).getClose();
        Direction direction = switch (context.getHigherTimeframeTrend()) {
            case "UP" -> Direction.LONG;
            case "DOWN" -> Direction.SHORT;
            default -> Direction.NEUTRAL;
        };

        // spacing SL/TP giả lập
        BigDecimal delta = lastClose.multiply(BigDecimal.valueOf(0.002)); // 0.2%

        BigDecimal entry = lastClose.setScale(2, RoundingMode.HALF_UP);
        BigDecimal sl;
        BigDecimal tp1;
        BigDecimal tp2;

        if (direction == Direction.LONG) {
            sl = entry.subtract(delta);
            tp1 = entry.add(delta.multiply(BigDecimal.valueOf(1.5)));
            tp2 = entry.add(delta.multiply(BigDecimal.valueOf(3.0)));
        } else if (direction == Direction.SHORT) {
            sl = entry.add(delta);
            tp1 = entry.subtract(delta.multiply(BigDecimal.valueOf(1.5)));
            tp2 = entry.subtract(delta.multiply(BigDecimal.valueOf(3.0)));
        } else {
            sl = null;
            tp1 = null;
            tp2 = null;
        }

        return TradeSuggestion.builder()
                .direction(direction)
                .entryPrice(entry)
                .stopLoss(sl)
                .takeProfit1(tp1)
                .takeProfit2(tp2)
                .riskReward1(direction == Direction.NEUTRAL ? null : BigDecimal.valueOf(1.5))
                .riskReward2(direction == Direction.NEUTRAL ? null : BigDecimal.valueOf(3.0))
                .riskReward3(null)
                .reasoning("Mock AI suggestion based on simple trend = " + context.getHigherTimeframeTrend())
                .build();
    }
}
