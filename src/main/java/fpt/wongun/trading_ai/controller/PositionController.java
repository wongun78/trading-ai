package fpt.wongun.trading_ai.controller;

import fpt.wongun.trading_ai.domain.enums.PositionStatus;
import fpt.wongun.trading_ai.dto.*;
import fpt.wongun.trading_ai.service.IPositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/positions")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Positions", description = "Trade execution and portfolio management")
public class PositionController {

    private final IPositionService positionService;

    @PostMapping
    @PreAuthorize("hasRole('TRADER') or hasRole('ADMIN')")
    @Operation(
        summary = "Open new position",
        description = "Execute a new trading position based on signal or manual entry",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<PositionResponseDto>> openPosition(
            @Valid @RequestBody OpenPositionRequestDto request
    ) {
        log.info("Received request to open position: {}/{}", 
                request.getSymbolCode(), request.getDirection());
        
        PositionResponseDto position = positionService.openPosition(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(position));
    }

    @PutMapping("/{id}/execute")
    public ResponseEntity<ApiResponse<PositionResponseDto>> executePosition(
            @PathVariable Long id,
            @Valid @RequestBody ExecutePositionRequestDto request
    ) {
        log.info("Executing position {} at price {}", id, request.getActualEntryPrice());
        
        PositionResponseDto position = positionService.executePosition(id, request);
        return ResponseEntity.ok(ApiResponse.success(position));
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<ApiResponse<PositionResponseDto>> closePosition(
            @PathVariable Long id,
            @Valid @RequestBody ClosePositionRequestDto request
    ) {
        log.info("Closing position {} at price {}, reason: {}", 
                id, request.getExitPrice(), request.getExitReason());
        
        PositionResponseDto position = positionService.closePosition(id, request);
        return ResponseEntity.ok(ApiResponse.success(position));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<PositionResponseDto>> cancelPosition(@PathVariable Long id) {
        log.info("Cancelling position {}", id);
        
        PositionResponseDto position = positionService.cancelPosition(id);
        return ResponseEntity.ok(ApiResponse.success(position));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PositionResponseDto>> getPosition(@PathVariable Long id) {
        log.debug("Fetching position {}", id);
        
        PositionResponseDto position = positionService.getPosition(id);
        return ResponseEntity.ok(ApiResponse.success(position));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PositionResponseDto>>> getPositions(
            @RequestParam(required = false) String symbolCode,
            @RequestParam(required = false) PositionStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        log.debug("Fetching positions - symbol: {}, status: {}, page: {}, size: {}", 
                symbolCode, status, page, size);
        
        Page<PositionResponseDto> positions = positionService.getPositions(
                symbolCode, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(positions));
    }

    @GetMapping("/open")
    public ResponseEntity<ApiResponse<List<PositionResponseDto>>> getOpenPositions(
            @RequestParam(defaultValue = "system") String userId
    ) {
        log.debug("Fetching open positions for user: {}", userId);
        
        List<PositionResponseDto> positions = positionService.getOpenPositions(userId);
        return ResponseEntity.ok(ApiResponse.success(positions));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<PortfolioStatsDto>> getPortfolioStats(
            @RequestParam(defaultValue = "system") String userId
    ) {
        log.info("Fetching portfolio stats for user: {}", userId);
        
        PortfolioStatsDto stats = positionService.getPortfolioStats(userId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
