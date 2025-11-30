package fpt.wongun.trading_ai.controller;

import fpt.wongun.trading_ai.domain.entity.Candle;
import fpt.wongun.trading_ai.domain.entity.Symbol;
import fpt.wongun.trading_ai.domain.enums.SymbolType;
import fpt.wongun.trading_ai.dto.CandleImportDto;
import fpt.wongun.trading_ai.repository.CandleRepository;
import fpt.wongun.trading_ai.repository.SymbolRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin controller for managing candle data import and deletion.
 */
@RestController
@RequestMapping("/api/admin/candles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CandleAdminController {

    private final CandleRepository candleRepository;
    private final SymbolRepository symbolRepository;

    /**
     * Bulk import candles from JSON array.
     */
    @PostMapping("/bulk-import")
    @Transactional
    public ResponseEntity<Map<String, Object>> bulkImport(@Valid @RequestBody List<CandleImportDto> candleDtos) {
        
        Set<String> uniqueSymbols = new HashSet<>();
        Set<String> timeframes = new HashSet<>();
        List<Candle> candles = new ArrayList<>();

        // Cache symbols to avoid repeated database lookups
        Map<String, Symbol> symbolCache = new HashMap<>();

        for (CandleImportDto dto : candleDtos) {
            // Get or create symbol
            Symbol symbol = symbolCache.computeIfAbsent(dto.getSymbolCode(), code -> {
                return symbolRepository.findByCode(code)
                        .orElseGet(() -> createSymbol(code));
            });

            uniqueSymbols.add(dto.getSymbolCode());
            timeframes.add(dto.getTimeframe());

            // Map DTO to Candle entity
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

        // Batch save all candles
        candleRepository.saveAll(candles);

        // Build response summary
        Map<String, Object> response = new HashMap<>();
        response.put("importedCount", candles.size());
        response.put("uniqueSymbols", uniqueSymbols.size());
        response.put("timeframes", new ArrayList<>(timeframes));
        response.put("message", "Bulk import completed successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Delete candles by symbol and/or timeframe.
     */
    @DeleteMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteCandles(
            @RequestParam(required = false) String symbolCode,
            @RequestParam(required = false) String timeframe) {

        long deletedCount;

        if (symbolCode != null && timeframe != null) {
            // Delete by symbol and timeframe
            Symbol symbol = symbolRepository.findByCode(symbolCode)
                    .orElseThrow(() -> new RuntimeException("Symbol not found: " + symbolCode));
            deletedCount = candleRepository.deleteBySymbolAndTimeframe(symbol, timeframe);

        } else if (symbolCode != null) {
            // Delete by symbol only
            Symbol symbol = symbolRepository.findByCode(symbolCode)
                    .orElseThrow(() -> new RuntimeException("Symbol not found: " + symbolCode));
            deletedCount = candleRepository.deleteBySymbol(symbol);

        } else {
            // Reject if no parameters provided
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "At least symbolCode must be provided"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("deletedCount", deletedCount);
        response.put("message", "Deletion completed successfully");

        return ResponseEntity.ok(response);
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
