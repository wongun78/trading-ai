package fpt.wongun.trading_ai.config;

import fpt.wongun.trading_ai.domain.entity.Candle;
import fpt.wongun.trading_ai.domain.entity.Symbol;
import fpt.wongun.trading_ai.domain.enums.SymbolType;
import fpt.wongun.trading_ai.repository.CandleRepository;
import fpt.wongun.trading_ai.repository.SymbolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Initializes fake candle data for XAUUSD/M5 on startup if no data exists.
 * Generates 200 candles using a simple random walk algorithm.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FakeCandleDataInitializer implements CommandLineRunner {

    private final CandleRepository candleRepository;
    private final SymbolRepository symbolRepository;

    private static final String SYMBOL_CODE = "XAUUSD";
    private static final String TIMEFRAME = "M5";
    private static final int NUM_CANDLES = 200;
    private static final BigDecimal BASE_PRICE = new BigDecimal("2000.00");

    @Override
    public void run(String... args) {
        log.info("Checking if fake candle data needs to be seeded for {}/{}", SYMBOL_CODE, TIMEFRAME);

        // Find or create symbol
        Symbol symbol = symbolRepository.findByCode(SYMBOL_CODE)
                .orElseGet(() -> createSymbol());

        // Check if candles already exist
        long existingCount = candleRepository.countBySymbolAndTimeframe(symbol, TIMEFRAME);

        if (existingCount > 0) {
            log.info("Found {} existing candles for {}/{}. Skipping seed.", 
                    existingCount, SYMBOL_CODE, TIMEFRAME);
            return;
        }

        log.info("No candles found for {}/{}. Generating {} fake candles...", 
                SYMBOL_CODE, TIMEFRAME, NUM_CANDLES);

        List<Candle> candles = generateFakeCandles(symbol);
        candleRepository.saveAll(candles);

        log.info("Successfully seeded {} fake candles for {}/{}", 
                candles.size(), SYMBOL_CODE, TIMEFRAME);
    }

    /**
     * Create XAUUSD symbol if it doesn't exist.
     */
    private Symbol createSymbol() {
        Symbol symbol = Symbol.builder()
                .code(SYMBOL_CODE)
                .type(SymbolType.COMMODITY)
                .description("Gold vs USD (Auto-generated)")
                .build();

        return symbolRepository.save(symbol);
    }

    /**
     * Generate fake candles using random walk algorithm.
     */
    private List<Candle> generateFakeCandles(Symbol symbol) {
        List<Candle> candles = new ArrayList<>();
        Random random = new Random();
        Instant now = Instant.now();

        BigDecimal previousClose = BASE_PRICE;

        for (int i = 0; i < NUM_CANDLES; i++) {
            // Calculate timestamp: 5 minutes apart, going backwards from now
            Instant timestamp = now.minus((NUM_CANDLES - i) * 5L, ChronoUnit.MINUTES);

            // Generate random price movement: -0.5% to +0.5%
            double changePercent = (random.nextDouble() - 0.5) * 0.01; // -0.005 to +0.005
            BigDecimal priceChange = previousClose.multiply(BigDecimal.valueOf(changePercent));
            
            // Calculate open and close with some randomness
            BigDecimal open = previousClose;
            BigDecimal close = previousClose.add(priceChange).setScale(2, RoundingMode.HALF_UP);

            // High and low based on open/close
            BigDecimal maxPrice = open.max(close);
            BigDecimal minPrice = open.min(close);

            // Add wick to high (0-0.2% above max)
            BigDecimal highWick = maxPrice.multiply(
                    BigDecimal.valueOf(random.nextDouble() * 0.002)
            );
            BigDecimal high = maxPrice.add(highWick).setScale(2, RoundingMode.HALF_UP);

            // Add wick to low (0-0.2% below min)
            BigDecimal lowWick = minPrice.multiply(
                    BigDecimal.valueOf(random.nextDouble() * 0.002)
            );
            BigDecimal low = minPrice.subtract(lowWick).setScale(2, RoundingMode.HALF_UP);

            // Generate random volume between 10 and 100
            BigDecimal volume = BigDecimal.valueOf(10 + random.nextDouble() * 90)
                    .setScale(2, RoundingMode.HALF_UP);

            Candle candle = Candle.builder()
                    .symbol(symbol)
                    .timeframe(TIMEFRAME)
                    .timestamp(timestamp)
                    .open(open)
                    .high(high)
                    .low(low)
                    .close(close)
                    .volume(volume)
                    .build();

            candles.add(candle);
            previousClose = close; // Use this close as next open
        }

        return candles;
    }
}
