package fpt.wongun.trading_ai.controller;

import fpt.wongun.trading_ai.dto.AiSignalResponseDto;
import fpt.wongun.trading_ai.dto.AiSuggestRequestDto;
import fpt.wongun.trading_ai.dto.ApiResponse;
import fpt.wongun.trading_ai.service.IAiSignalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/signals")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "AI Signals", description = "AI trading signal generation and history")
public class AiSignalController {

    private final IAiSignalService aiSignalService;

    @PostMapping("/ai-suggest")
    @PreAuthorize("hasRole('TRADER') or hasRole('ADMIN')")
    @Operation(
        summary = "Generate AI trading signal",
        description = "Analyze market data and generate AI-powered trading signal based on Bob Volman methodology",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<AiSignalResponseDto>> suggest(
            @Valid @RequestBody AiSuggestRequestDto requestDto) {
        
        log.info("Received AI signal request for {}/{}/{}", 
                requestDto.getSymbolCode(), 
                requestDto.getTimeframe(), 
                requestDto.getMode());
        
        AiSignalResponseDto signal = aiSignalService.generateSignal(requestDto);
        
        return ResponseEntity.ok(ApiResponse.success(signal));
    }

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
