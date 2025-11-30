package fpt.wongun.trading_ai.service.market;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Client for fetching real-time market data from Binance API.
 * 
 * FREE - No API key required for public market data.
 * 
 * Binance API Docs: https://binance-docs.github.io/apidocs/spot/en/
 * 
 * Supported intervals:
 * - 1m, 3m, 5m, 15m, 30m, 1h, 2h, 4h, 6h, 8h, 12h, 1d, 3d, 1w, 1M
 */
@Service
@Slf4j
public class BinanceClient {

    private static final String BINANCE_API_BASE = "https://api.binance.com";
    private final WebClient webClient;

    public BinanceClient() {
        this.webClient = WebClient.builder()
                .baseUrl(BINANCE_API_BASE)
                .build();
    }

    /**
     * Fetch klines/candlestick data from Binance.
     * 
     * @param symbol Trading pair (e.g., "BTCUSDT", "ETHUSDT")
     * @param interval Timeframe (e.g., "5m", "15m", "1h")
     * @param limit Number of candles to fetch (max 1000, default 500)
     * @return List of BinanceKline objects
     * 
     * Example:
     * fetchKlines("BTCUSDT", "5m", 200)
     */
    public List<BinanceKline> fetchKlines(String symbol, String interval, int limit) {
        try {
            log.info("Fetching {} candles for {}/{} from Binance...", limit, symbol, interval);

            // Call Binance API: GET /api/v3/klines
            Mono<List> responseMono = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v3/klines")
                            .queryParam("symbol", symbol)
                            .queryParam("interval", interval)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(List.class);

            List<Object> response = responseMono.block();

            if (response == null || response.isEmpty()) {
                log.warn("No data returned from Binance for {}/{}", symbol, interval);
                return new ArrayList<>();
            }

            // Parse response to BinanceKline objects
            List<BinanceKline> klines = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof List) {
                    Object[] arr = ((List<?>) item).toArray();
                    klines.add(BinanceKline.fromArray(arr));
                }
            }

            log.info("Successfully fetched {} candles for {}/{}", klines.size(), symbol, interval);
            return klines;

        } catch (Exception e) {
            log.error("Error fetching data from Binance for {}/{}: {}", symbol, interval, e.getMessage());
            throw new RuntimeException("Failed to fetch Binance data: " + e.getMessage(), e);
        }
    }

    /**
     * Map Binance interval to your internal timeframe format.
     * 
     * Binance: 5m, 15m, 1h
     * Your system: M5, M15, H1
     */
    public static String mapIntervalToTimeframe(String binanceInterval) {
        return switch (binanceInterval) {
            case "1m" -> "M1";
            case "3m" -> "M3";
            case "5m" -> "M5";
            case "15m" -> "M15";
            case "30m" -> "M30";
            case "1h" -> "H1";
            case "4h" -> "H4";
            case "1d" -> "D1";
            default -> binanceInterval.toUpperCase();
        };
    }

    /**
     * Map your timeframe format to Binance interval.
     */
    public static String mapTimeframeToInterval(String timeframe) {
        return switch (timeframe.toUpperCase()) {
            case "M1" -> "1m";
            case "M3" -> "3m";
            case "M5" -> "5m";
            case "M15" -> "15m";
            case "M30" -> "30m";
            case "H1" -> "1h";
            case "H4" -> "4h";
            case "D1" -> "1d";
            default -> timeframe.toLowerCase();
        };
    }

    /**
     * Check if symbol is valid on Binance.
     * Common pairs: BTCUSDT, ETHUSDT, BNBUSDT, ADAUSDT, SOLUSDT
     */
    public boolean isValidSymbol(String symbol) {
        try {
            List<BinanceKline> klines = fetchKlines(symbol, "5m", 1);
            return !klines.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
