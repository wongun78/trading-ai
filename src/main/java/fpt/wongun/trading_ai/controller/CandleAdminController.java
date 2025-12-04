package fpt.wongun.trading_ai.controller;

import fpt.wongun.trading_ai.domain.entity.Candle;
import fpt.wongun.trading_ai.domain.entity.Symbol;
import fpt.wongun.trading_ai.domain.enums.SymbolType;
import fpt.wongun.trading_ai.dto.ApiResponse;
import fpt.wongun.trading_ai.dto.CandleImportDto;
import fpt.wongun.trading_ai.dto.CandleResponseDto;
import fpt.wongun.trading_ai.exception.SymbolNotFoundException;
import fpt.wongun.trading_ai.repository.CandleRepository;
import fpt.wongun.trading_ai.repository.SymbolRepository;
import fpt.wongun.trading_ai.service.market.BinanceClient;
import fpt.wongun.trading_ai.service.market.BinanceKline;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * Admin controller for managing candle data import and deletion.
 * Supports importing real-time data from Binance API.
 */
@RestController
@RequestMapping("/api/admin/candles")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Admin - Candles", description = "Market data management (Admin only)")
@PreAuthorize("hasRole('ADMIN')")
public class CandleAdminController {

    private final CandleRepository candleRepository;
    private final SymbolRepository symbolRepository;
    private final BinanceClient binanceClient;

    /**
     * Get candles for a specific symbol and timeframe.
     * 
     * GET /api/admin/candles?symbolCode=BTCUSDT&timeframe=M5&limit=100
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CandleResponseDto>>> getCandles(
            @RequestParam String symbolCode,
            @RequestParam String timeframe,
            @RequestParam(defaultValue = "200") @Min(1) @Max(1000) int limit) {
        
        log.debug("Fetching {} candles for {}/{}", limit, symbolCode, timeframe);
        
        Symbol symbol = symbolRepository.findByCode(symbolCode)
                .orElseThrow(() -> new SymbolNotFoundException(symbolCode));
        
        List<Candle> candles = candleRepository
                .findTop200BySymbolAndTimeframeOrderByTimestampDesc(symbol, timeframe);
        
        // Limit results
        if (limit < candles.size()) {
            candles = candles.subList(0, limit);
        }
        
        List<CandleResponseDto> response = candles.stream()
                .map(c -> CandleResponseDto.builder()
                        .time(c.getTimestamp())
                        .open(c.getOpen())
                        .high(c.getHigh())
                        .low(c.getLow())
                        .close(c.getClose())
                        .build())
                .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Bulk import candles from JSON array.
     * 
     * POST /api/admin/candles/bulk-import
     */
    @PostMapping("/bulk-import")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkImport(
            @Valid @RequestBody List<CandleImportDto> candleDtos) {
        
        log.info("Bulk importing {} candles", candleDtos.size());
        
        Set<String> uniqueSymbols = new HashSet<>();
        Set<String> timeframes = new HashSet<>();
        List<Candle> candles = new ArrayList<>();
        Map<String, Symbol> symbolCache = new HashMap<>();

        for (CandleImportDto dto : candleDtos) {
            Symbol symbol = symbolCache.computeIfAbsent(dto.getSymbolCode(), code -> {
                return symbolRepository.findByCode(code)
                        .orElseGet(() -> createSymbol(code));
            });

            uniqueSymbols.add(dto.getSymbolCode());
            timeframes.add(dto.getTimeframe());

            Candle candle = Candle.builder()
                    .symbol(symbol)
                    .timeframe(dto.getTimeframe())
                    .timestamp(dto.getTimestamp())
                    .open(dto.getOpen())
                    .high(dto.getHigh())
                    .low(dto.getLow())
                    .close(dto.getClose())
                    .volume(dto.getVolume())
                    .build();

            candles.add(candle);
        }

        candleRepository.saveAll(candles);

        Map<String, Object> result = new HashMap<>();
        result.put("importedCount", candles.size());
        result.put("uniqueSymbols", uniqueSymbols.size());
        result.put("timeframes", new ArrayList<>(timeframes));
        result.put("message", "Bulk import completed successfully");

        log.info("Bulk import completed: {} candles", candles.size());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Delete candles by symbol and/or timeframe.
     * 
     * DELETE /api/admin/candles?symbolCode=BTCUSDT&timeframe=M5
     */
    @DeleteMapping
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteCandles(
            @RequestParam(required = false) String symbolCode,
            @RequestParam(required = false) String timeframe) {

        log.warn("Deleting candles: symbolCode={}, timeframe={}", symbolCode, timeframe);

        long deletedCount;

        if (symbolCode != null && timeframe != null) {
            Symbol symbol = symbolRepository.findByCode(symbolCode)
                    .orElseThrow(() -> new SymbolNotFoundException(symbolCode));
            deletedCount = candleRepository.deleteBySymbolAndTimeframe(symbol, timeframe);

        } else if (symbolCode != null) {
            Symbol symbol = symbolRepository.findByCode(symbolCode)
                    .orElseThrow(() -> new SymbolNotFoundException(symbolCode));
            deletedCount = candleRepository.deleteBySymbol(symbol);

        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_REQUEST", 
                          "Must provide at least symbolCode parameter"));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("deletedCount", deletedCount);
        result.put("message", "Candles deleted successfully");

        log.info("Deleted {} candles", deletedCount);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Import real-time candles from Binance API.
     * 
     * POST /api/admin/candles/import-binance?symbol=BTCUSDT&timeframe=M5&limit=200
     */
    @PostMapping("/import-binance")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> importFromBinance(
            @RequestParam String symbol,
            @RequestParam String timeframe,
            @RequestParam(defaultValue = "200") @Min(1) @Max(1000) int limit) {

        log.info("Importing {} candles from Binance: {}/{}", limit, symbol, timeframe);

        try {
            String interval = BinanceClient.mapTimeframeToInterval(timeframe);
            List<BinanceKline> klines = binanceClient.fetchKlines(symbol, interval, limit);

            if (klines.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("NO_DATA", 
                              "No data returned from Binance for " + symbol));
            }

            Symbol symbolEntity = symbolRepository.findByCode(symbol)
                    .orElseGet(() -> createSymbol(symbol));

            List<Candle> candles = klines.stream()
                    .map(kline -> Candle.builder()
                            .symbol(symbolEntity)
                            .timeframe(timeframe)
                            .timestamp(Instant.ofEpochMilli(kline.getOpenTime()))
                            .open(kline.getOpen())
                            .high(kline.getHigh())
                            .low(kline.getLow())
                            .close(kline.getClose())
                            .volume(kline.getVolume())
                            .build())
                    .toList();

            candleRepository.deleteBySymbolAndTimeframe(symbolEntity, timeframe);
            candleRepository.saveAll(candles);

            Map<String, Object> result = new HashMap<>();
            result.put("symbol", symbol);
            result.put("timeframe", timeframe);
            result.put("importedCount", candles.size());
            result.put("source", "Binance API");
            result.put("message", "Successfully imported real-time data from Binance");

            log.info("Imported {} candles from Binance", candles.size());

            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            log.error("Failed to import from Binance: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("BINANCE_IMPORT_ERROR", 
                          "Failed to import from Binance: " + e.getMessage()));
        }
    }

    /**
     * Helper method to create a new Symbol based on symbol code.
     */
    private Symbol createSymbol(String code) {
        SymbolType type;
        String description;

        if (code.endsWith("USDT")) {
            type = SymbolType.CRYPTO;
            description = code.replace("USDT", "") + " Cryptocurrency vs Tether";
        } else if (code.endsWith("USD")) {
            type = SymbolType.FOREX;
            description = code.replace("USD", "") + " vs US Dollar";
        } else {
            type = SymbolType.COMMODITY;
            description = code + " (Auto-created)";
        }

        Symbol symbol = Symbol.builder()
                .code(code)
                .type(type)
                .description(description)
                .build();

        return symbolRepository.save(symbol);
    }
}
