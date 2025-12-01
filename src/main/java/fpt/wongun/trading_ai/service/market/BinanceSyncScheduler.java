package fpt.wongun.trading_ai.service.market;

import fpt.wongun.trading_ai.domain.entity.Candle;
import fpt.wongun.trading_ai.domain.entity.Symbol;
import fpt.wongun.trading_ai.domain.enums.SymbolType;
import fpt.wongun.trading_ai.repository.CandleRepository;
import fpt.wongun.trading_ai.repository.SymbolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled service to automatically sync latest candles from Binance API.
 * Runs every 5 minutes to keep crypto data up-to-date.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BinanceSyncScheduler {

    private final BinanceClient binanceClient;
    private final SymbolRepository symbolRepository;
    private final CandleRepository candleRepository;

    /**
     * Sync latest candles for all crypto symbols.
     * Runs every 5 seconds for real-time updates.
     * 
     * Fetches only the latest 20 candles to minimize API calls.
     */
    @Scheduled(fixedRate = 5000)  // 5 seconds
    @Transactional
    public void syncLatestCandles() {
        log.info("Starting scheduled Binance candle sync...");

        // Find all crypto symbols (BTCUSDT, ETHUSDT, etc.)
        List<Symbol> cryptoSymbols = symbolRepository.findAll().stream()
                .filter(s -> s.getType() == SymbolType.CRYPTO)
                .toList();

        if (cryptoSymbols.isEmpty()) {
            log.info("No crypto symbols found. Skipping sync.");
            return;
        }

        int totalSynced = 0;

        for (Symbol symbol : cryptoSymbols) {
            try {
                // Get the most recent candle to determine which timeframes exist
                List<Candle> existingCandles = candleRepository.findTop1BySymbolOrderByTimestampDesc(symbol);
                
                if (existingCandles.isEmpty()) {
                    log.warn("No existing candles for {}. Skipping sync.", symbol.getCode());
                    continue;
                }

                String timeframe = existingCandles.get(0).getTimeframe();
                String interval = BinanceClient.mapTimeframeToInterval(timeframe);

                // Fetch latest 200 candles (required for Bob Volman analysis with trend context)
                List<BinanceKline> klines = binanceClient.fetchKlines(symbol.getCode(), interval, 200);

                if (klines.isEmpty()) {
                    log.warn("No data from Binance for {}/{}. Skipping.", symbol.getCode(), timeframe);
                    continue;
                }

                // Get existing candles to avoid duplicates
                List<Candle> existingCandlesForTimeframe = candleRepository.findBySymbolAndTimeframe(symbol, timeframe);
                
                // Create a set of existing timestamps for fast lookup
                var existingTimestamps = existingCandlesForTimeframe.stream()
                        .map(Candle::getTimestamp)
                        .collect(java.util.stream.Collectors.toSet());

                // Convert to Candle entities (only new ones)
                List<Candle> newCandles = klines.stream()
                        .filter(kline -> !existingTimestamps.contains(Instant.ofEpochMilli(kline.getOpenTime())))
                        .map(kline -> Candle.builder()
                                .symbol(symbol)
                                .timeframe(timeframe)
                                .timestamp(Instant.ofEpochMilli(kline.getOpenTime()))
                                .open(kline.getOpen())
                                .high(kline.getHigh())
                                .low(kline.getLow())
                                .close(kline.getClose())
                                .volume(kline.getVolume())
                                .build())
                        .toList();

                // Save only new candles
                if (!newCandles.isEmpty()) {
                    candleRepository.saveAll(newCandles);
                }

                totalSynced += newCandles.size();
                log.info("Synced {} new candles for {}/{}", 
                        newCandles.size(), symbol.getCode(), timeframe);

            } catch (Exception e) {
                log.error("Failed to sync candles for {}: {}", symbol.getCode(), e.getMessage());
            }
        }

        log.info("Binance sync completed. Total candles synced: {}", totalSynced);
    }

    /**
     * Initial sync on application startup.
     * Delays 30 seconds to allow app to fully initialize.
     */
    @Scheduled(initialDelay = 30000, fixedDelay = Long.MAX_VALUE)  // Run once 30s after startup
    @Transactional
    public void initialSync() {
        log.info("Running initial Binance candle sync...");
        syncLatestCandles();
    }
}
