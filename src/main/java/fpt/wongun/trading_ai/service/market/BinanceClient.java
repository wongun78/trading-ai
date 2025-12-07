package fpt.wongun.trading_ai.service.market;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

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
                if (item instanceof List<?> list) {
                    Object[] arr = list.toArray();
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

    public boolean isValidSymbol(String symbol) {
        try {
            List<BinanceKline> klines = fetchKlines(symbol, "5m", 1);
            return !klines.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
