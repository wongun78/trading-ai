package fpt.wongun.trading_ai.controller;

import fpt.wongun.trading_ai.dto.AiSignalResponseDto;
import fpt.wongun.trading_ai.dto.AiSuggestRequestDto;
import fpt.wongun.trading_ai.dto.ApiResponse;
import fpt.wongun.trading_ai.service.AiSignalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * REST Controller for AI trading signal generation and retrieval.
 * Provides endpoints for generating signals and querying signal history.
 */
@RestController
@RequestMapping("/api/signals")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AiSignalController {

    private final AiSignalService aiSignalService;

    /**
     * Generate AI trading signal.
     * 
     * POST /api/signals/ai-suggest
     * {
     *   "symbolCode": "BTCUSDT",
     *   "timeframe": "M5",
     *   "mode": "SCALPING"
     * }
     */
    @PostMapping("/ai-suggest")
    public ResponseEntity<ApiResponse<AiSignalResponseDto>> suggest(
            @Valid @RequestBody AiSuggestRequestDto requestDto) {
        
        log.info("Received AI signal request for {}/{}/{}", 
                requestDto.getSymbolCode(), 
                requestDto.getTimeframe(), 
                requestDto.getMode());
        
        AiSignalResponseDto signal = aiSignalService.generateSignal(requestDto);
        
        return ResponseEntity.ok(ApiResponse.success(signal));
    }

    /**
     * Retrieve paginated signal history.
     * 
     * GET /api/signals?symbolCode=BTCUSDT&timeframe=M5&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AiSignalResponseDto>>> list(
            @RequestParam String symbolCode,
            @RequestParam String timeframe,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        log.debug("Fetching signals for {}/{} - page={}, size={}", 
                symbolCode, timeframe, page, size);
        
        Page<AiSignalResponseDto> signals = aiSignalService.getSignals(
                symbolCode, timeframe, from, to, page, size
        );
        
        return ResponseEntity.ok(ApiResponse.success(signals));
    }
}
