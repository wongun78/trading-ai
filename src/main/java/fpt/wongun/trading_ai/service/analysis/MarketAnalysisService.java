package fpt.wongun.trading_ai.service.analysis;

import fpt.wongun.trading_ai.domain.entity.Candle;
import fpt.wongun.trading_ai.domain.entity.Symbol;
import fpt.wongun.trading_ai.repository.CandleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MarketAnalysisService {

    private final CandleRepository candleRepository;

    public TradeAnalysisContext buildContext(Symbol symbol, String timeframe) {
        List<Candle> candles = candleRepository
                .findTop200BySymbolAndTimeframeOrderByTimestampDesc(symbol, timeframe);

        if (candles.isEmpty()) {
            return TradeAnalysisContext.builder()
                    .symbolCode(symbol.getCode())
                    .timeframe(timeframe)
                    .higherTimeframeTrend("UNKNOWN")
                    .candles(Collections.emptyList())
                    .ema21(Collections.emptyList())
                    .ema25(Collections.emptyList())
                    .build();
        }

        candles.sort(Comparator.comparing(Candle::getTimestamp));

        List<TradeAnalysisContext.CandlePoint> candlePoints = candles.stream()
                .map(c -> TradeAnalysisContext.CandlePoint.builder()
                        .timestamp(c.getTimestamp())
                        .open(c.getOpen())
                        .high(c.getHigh())
                        .low(c.getLow())
                        .close(c.getClose())
                        .volume(c.getVolume())
                        .build())
                .toList();

        List<BigDecimal> closes = candlePoints.stream()
                .map(TradeAnalysisContext.CandlePoint::getClose)
                .toList();

        List<BigDecimal> ema21 = computeEma(closes, 21);
        List<BigDecimal> ema25 = computeEma(closes, 25);

        String trend = inferTrend(closes);

        return TradeAnalysisContext.builder()
                .symbolCode(symbol.getCode())
                .timeframe(timeframe)
                .higherTimeframeTrend(trend)
                .candles(candlePoints)
                .ema21(ema21)
                .ema25(ema25)
                .build();
    }

    private List<BigDecimal> computeEma(List<BigDecimal> closes, int period) {
        if (closes.isEmpty() || closes.size() < period) {
            return Collections.emptyList();
        }
        
        BigDecimal k = BigDecimal.valueOf(2.0 / (period + 1.0));
        BigDecimal[] emaArr = new BigDecimal[closes.size()];
        
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < period; i++) {
            sum = sum.add(closes.get(i));
        }
        BigDecimal emaPrev = sum.divide(BigDecimal.valueOf(period), 6, RoundingMode.HALF_UP);
        
        for (int i = 0; i < period - 1; i++) {
            emaArr[i] = null; 
        }
        emaArr[period - 1] = emaPrev;

        for (int i = period; i < closes.size(); i++) {
            BigDecimal price = closes.get(i);
            emaPrev = price.multiply(k)
                    .add(emaPrev.multiply(BigDecimal.ONE.subtract(k)))
                    .setScale(6, RoundingMode.HALF_UP);
            emaArr[i] = emaPrev;
        }
        
        return Arrays.stream(emaArr)
                .filter(Objects::nonNull)
                .toList();
    }

    private String inferTrend(List<BigDecimal> closes) {
        if (closes.size() < 5) {
            return "UNKNOWN";
        }
        BigDecimal last = closes.getLast();
        BigDecimal prev = closes.get(closes.size() - 5);

        int cmp = last.compareTo(prev);
        if (cmp > 0) {
            return "UP";
        } else if (cmp < 0) {
            return "DOWN";
        } else {
            return "SIDEWAYS";
        }
    }
}
