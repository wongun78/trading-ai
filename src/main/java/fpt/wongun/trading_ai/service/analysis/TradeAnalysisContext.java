package fpt.wongun.trading_ai.service.analysis;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class TradeAnalysisContext {

    @Data
    @Builder
    public static class CandlePoint {
        private Instant timestamp;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private BigDecimal volume;
    }

    private String symbolCode;
    private String timeframe;
    private String higherTimeframeTrend; // simple: UP/DOWN/SIDEWAYS
    private List<CandlePoint> candles;
    private List<BigDecimal> ema21;
    private List<BigDecimal> ema25;
    // support/resistance có thể để sau
}
