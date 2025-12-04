package fpt.wongun.trading_ai.service;

import fpt.wongun.trading_ai.domain.entity.AiSignal;
import fpt.wongun.trading_ai.domain.entity.Position;
import fpt.wongun.trading_ai.domain.entity.Symbol;
import fpt.wongun.trading_ai.domain.enums.Direction;
import fpt.wongun.trading_ai.domain.enums.PositionStatus;
import fpt.wongun.trading_ai.dto.*;
import fpt.wongun.trading_ai.exception.ForbiddenException;
import fpt.wongun.trading_ai.exception.InvalidPositionException;
import fpt.wongun.trading_ai.exception.PositionNotFoundException;
import fpt.wongun.trading_ai.exception.SymbolNotFoundException;
import fpt.wongun.trading_ai.repository.AiSignalRepository;
import fpt.wongun.trading_ai.repository.PositionRepository;
import fpt.wongun.trading_ai.repository.SymbolRepository;
import fpt.wongun.trading_ai.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of PositionService.
 * Handles position opening, closing, and portfolio analytics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PositionServiceImpl implements IPositionService {

    private final PositionRepository positionRepository;
    private final SymbolRepository symbolRepository;
    private final AiSignalRepository aiSignalRepository;
    private final SecurityUtils securityUtils;

    /**
     * Open a new position (PENDING status initially)
     */
    @Transactional
    public PositionResponseDto openPosition(OpenPositionRequestDto request) {
        log.info("Opening new position for symbol: {}, direction: {}", 
                request.getSymbolCode(), request.getDirection());

        // Validate symbol exists
        Symbol symbol = symbolRepository.findByCode(request.getSymbolCode())
                .orElseThrow(() -> new SymbolNotFoundException(request.getSymbolCode()));

        // Get AI signal if provided
        AiSignal signal = null;
        if (request.getSignalId() != null) {
            signal = aiSignalRepository.findById(request.getSignalId())
                    .orElse(null);
        }

        // Create position
        Position position = Position.builder()
                .signal(signal)
                .symbol(symbol)
                .status(PositionStatus.PENDING)
                .direction(request.getDirection())
                .plannedEntryPrice(request.getPlannedEntryPrice())
                .stopLoss(request.getStopLoss())
                .takeProfit1(request.getTakeProfit1())
                .takeProfit2(request.getTakeProfit2())
                .takeProfit3(request.getTakeProfit3())
                .quantity(request.getQuantity())
                .notes(request.getNotes())
                .fees(BigDecimal.ZERO)
                .build();

        position = positionRepository.save(position);

        log.info("Position created with ID: {}", position.getId());
        return mapToDto(position);
    }

    /**
     * Execute position (change from PENDING to OPEN)
     */
    @Transactional
    public PositionResponseDto executePosition(Long positionId, ExecutePositionRequestDto request) {
        log.info("Executing position ID: {} at price: {}", positionId, request.getActualEntryPrice());

        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new PositionNotFoundException(positionId));
        
        // Check ownership (only owner or admin can execute)
        validateOwnership(position);
        
        // Check ownership (only owner or admin can close)
        validateOwnership(position);

        if (position.getStatus() != PositionStatus.PENDING) {
            throw new InvalidPositionException("Position must be PENDING to execute");
        }

        position.setStatus(PositionStatus.OPEN);
        position.setActualEntryPrice(request.getActualEntryPrice());
        position.setOpenedAt(Instant.now());

        position = positionRepository.save(position);

        log.info("Position {} executed successfully", positionId);
        return mapToDto(position);
    }

    /**
     * Close an open position
     */
    @Transactional
    public PositionResponseDto closePosition(Long positionId, ClosePositionRequestDto request) {
        log.info("Closing position ID: {} at price: {}, reason: {}", 
                positionId, request.getExitPrice(), request.getExitReason());

        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new PositionNotFoundException(positionId));

        if (position.getStatus() != PositionStatus.OPEN) {
            throw new InvalidPositionException("Position must be OPEN to close");
        }

        // Update position
        position.setStatus(PositionStatus.CLOSED);
        position.setExitPrice(request.getExitPrice());
        position.setExitReason(request.getExitReason());
        position.setClosedAt(Instant.now());
        
        if (request.getFees() != null) {
            position.setFees(position.getFees().add(request.getFees()));
        }

        if (request.getNotes() != null) {
            String existingNotes = position.getNotes() != null ? position.getNotes() + "\n" : "";
            position.setNotes(existingNotes + request.getNotes());
        }

        // Calculate P&L
        position.calculateRealizedPnL();

        position = positionRepository.save(position);

        log.info("Position {} closed. P&L: {}", positionId, position.getRealizedPnL());
        return mapToDto(position);
    }

    /**
     * Cancel a pending position
     */
    @Transactional
    public PositionResponseDto cancelPosition(Long positionId) {
        log.info("Cancelling position ID: {}", positionId);

        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new PositionNotFoundException(positionId));
        
        // Check ownership
        validateOwnership(position);

        if (position.getStatus() != PositionStatus.PENDING) {
            throw new InvalidPositionException("Only PENDING positions can be cancelled");
        }

        position.setStatus(PositionStatus.CANCELLED);
        position = positionRepository.save(position);

        log.info("Position {} cancelled", positionId);
        return mapToDto(position);
    }

    /**
     * Get position by ID
     * Users can only see their own positions unless admin
     */
    public PositionResponseDto getPosition(Long positionId) {
        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new PositionNotFoundException(positionId));
        
        // Check ownership
        validateOwnership(position);
        
        return mapToDto(position);
    }

    /**
     * Get all positions with pagination and filtering
     * Users see only their own positions, admins see all
     */
    public Page<PositionResponseDto> getPositions(
            String symbolCode,
            PositionStatus status,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        
        // Determine if we should filter by user
        String currentUsername = null;
        if (!securityUtils.isAdmin()) {
            currentUsername = securityUtils.getCurrentUsername();
            log.debug("Filtering positions for user: {}", currentUsername);
        }

        Page<Position> positions;
        
        if (currentUsername != null) {
            // Non-admin: filter by user
            if (symbolCode != null && status != null) {
                Symbol symbol = symbolRepository.findByCode(symbolCode)
                        .orElseThrow(() -> new SymbolNotFoundException(symbolCode));
                positions = positionRepository.findByCreatedByAndSymbolAndStatusOrderByOpenedAtDesc(
                        currentUsername, symbol, status, pageable);
            } else if (status != null) {
                positions = positionRepository.findByCreatedByAndStatusOrderByOpenedAtDesc(
                        currentUsername, status, pageable);
            } else if (symbolCode != null) {
                Symbol symbol = symbolRepository.findByCode(symbolCode)
                        .orElseThrow(() -> new SymbolNotFoundException(symbolCode));
                positions = positionRepository.findByCreatedByAndSymbolOrderByOpenedAtDesc(
                        currentUsername, symbol, pageable);
            } else {
                positions = positionRepository.findByCreatedByOrderByOpenedAtDesc(
                        currentUsername, pageable);
            }
        } else {
            // Admin: see all
            if (symbolCode != null && status != null) {
                Symbol symbol = symbolRepository.findByCode(symbolCode)
                        .orElseThrow(() -> new SymbolNotFoundException(symbolCode));
                positions = positionRepository.findBySymbolAndStatusOrderByOpenedAtDesc(
                        symbol, status, pageable);
            } else if (status != null) {
                positions = positionRepository.findByStatusOrderByOpenedAtDesc(status, pageable);
            } else {
                positions = positionRepository.findAll(pageable);
            }
        }

        return positions.map(this::mapToDto);
    }

    /**
     * Get open positions for current user
     */
    public List<PositionResponseDto> getOpenPositions(String userId) {
        List<Position> positions = positionRepository.findByCreatedByAndStatus(
                userId, PositionStatus.OPEN);
        return positions.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get portfolio statistics for a user
     */
    public PortfolioStatsDto getPortfolioStats(String userId) {
        log.info("Calculating portfolio stats for user: {}", userId);

        // Position counts
        long totalPositions = positionRepository.countByCreatedByAndStatus(userId, PositionStatus.CLOSED);
        long openPositions = positionRepository.countByCreatedByAndStatus(userId, PositionStatus.OPEN);
        long pendingPositions = positionRepository.countByCreatedByAndStatus(userId, PositionStatus.PENDING);

        // P&L metrics
        BigDecimal totalPnL = positionRepository.getTotalPnLByUser(userId);
        BigDecimal averagePnL = positionRepository.getAveragePnLByUser(userId);

        // Best/Worst trades
        List<Position> bestTrades = positionRepository.getBestTrades(userId, PageRequest.of(0, 1));
        List<Position> worstTrades = positionRepository.getWorstTrades(userId, PageRequest.of(0, 1));

        BigDecimal bestTradePnL = bestTrades.isEmpty() ? BigDecimal.ZERO : bestTrades.getFirst().getRealizedPnL();
        BigDecimal worstTradePnL = worstTrades.isEmpty() ? BigDecimal.ZERO : worstTrades.getFirst().getRealizedPnL();

        // Win rates
        Double winRate = positionRepository.getWinRateByUser(userId);
        Double longWinRate = positionRepository.getWinRateByDirection(userId, Direction.LONG);
        Double shortWinRate = positionRepository.getWinRateByDirection(userId, Direction.SHORT);

        // Average R:R
        BigDecimal averageRiskReward = positionRepository.getAverageRiskReward(userId);

        return PortfolioStatsDto.builder()
                .totalPositions(totalPositions + openPositions + pendingPositions)
                .openPositions(openPositions)
                .closedPositions(totalPositions)
                .pendingPositions(pendingPositions)
                .totalPnL(totalPnL != null ? totalPnL : BigDecimal.ZERO)
                .averagePnL(averagePnL != null ? averagePnL : BigDecimal.ZERO)
                .bestTradePnL(bestTradePnL)
                .worstTradePnL(worstTradePnL)
                .winRate(winRate != null ? winRate : 0.0)
                .longWinRate(longWinRate != null ? longWinRate : 0.0)
                .shortWinRate(shortWinRate != null ? shortWinRate : 0.0)
                .averageRiskReward(averageRiskReward != null ? averageRiskReward : BigDecimal.ZERO)
                .build();
    }

    /**
     * Map Position entity to DTO
     */
    /**
     * Validate that current user owns the position or is admin.
     */
    private void validateOwnership(Position position) {
        if (securityUtils.isAdmin()) {
            return; // Admin can access all positions
        }
        
        String currentUsername = securityUtils.getCurrentUsername();
        String positionOwner = position.getCreatedBy();
        
        if (positionOwner == null || !positionOwner.equals(currentUsername)) {
            log.warn("User {} attempted to access position {} owned by {}", 
                    currentUsername, position.getId(), positionOwner);
            throw ForbiddenException.ownershipViolation("positions");
        }
    }
    
    private PositionResponseDto mapToDto(Position position) {
        return PositionResponseDto.builder()
                .id(position.getId())
                .signalId(position.getSignal() != null ? position.getSignal().getId() : null)
                .symbolCode(position.getSymbol().getCode())
                .status(position.getStatus())
                .direction(position.getDirection())
                .plannedEntryPrice(position.getPlannedEntryPrice())
                .actualEntryPrice(position.getActualEntryPrice())
                .stopLoss(position.getStopLoss())
                .takeProfit1(position.getTakeProfit1())
                .takeProfit2(position.getTakeProfit2())
                .takeProfit3(position.getTakeProfit3())
                .exitPrice(position.getExitPrice())
                .quantity(position.getQuantity())
                .realizedPnL(position.getRealizedPnL())
                .realizedPnLPercent(position.getRealizedPnLPercent())
                .actualRiskReward(position.getActualRiskReward())
                .exitReason(position.getExitReason())
                .openedAt(position.getOpenedAt())
                .closedAt(position.getClosedAt())
                .fees(position.getFees())
                .slippage(position.getSlippage())
                .durationMs(position.getDurationMs())
                .notes(position.getNotes())
                .createdAt(position.getCreatedAt())
                .createdBy(position.getCreatedBy())
                .build();
    }
}
