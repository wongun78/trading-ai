package fpt.wongun.trading_ai.service;

import fpt.wongun.trading_ai.domain.enums.PositionStatus;
import fpt.wongun.trading_ai.dto.*;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Service interface for Position (trading position) operations.
 * Handles position lifecycle: open, execute, close, cancel.
 */
public interface IPositionService {

    /**
     * Open a new trading position.
     * Initial status is PENDING until executed.
     * 
     * @param request position details (symbol, direction, quantity, etc.)
     * @return created position DTO
     */
    PositionResponseDto openPosition(OpenPositionRequestDto request);

    /**
     * Execute a pending position (fill order at actual market price).
     * Changes status from PENDING to OPEN.
     * 
     * @param positionId the position ID
     * @param request execution details (actual entry price)
     * @return updated position DTO
     */
    PositionResponseDto executePosition(Long positionId, ExecutePositionRequestDto request);

    /**
     * Close an open position.
     * Changes status from OPEN to CLOSED.
     * Calculates realized P&L.
     * 
     * @param positionId the position ID
     * @param request closing details (exit price, reason)
     * @return updated position DTO
     */
    PositionResponseDto closePosition(Long positionId, ClosePositionRequestDto request);

    /**
     * Cancel a pending position.
     * Changes status from PENDING to CANCELLED.
     * 
     * @param positionId the position ID
     * @return updated position DTO
     */
    PositionResponseDto cancelPosition(Long positionId);

    /**
     * Get position by ID.
     * Users can only view their own positions unless admin.
     * 
     * @param positionId the position ID
     * @return position DTO
     */
    PositionResponseDto getPosition(Long positionId);

    /**
     * Get all positions with pagination and filtering.
     * - Non-admin users: see only own positions
     * - Admin users: see all positions
     * 
     * @param symbolCode optional filter by symbol
     * @param status optional filter by status
     * @param page page number (0-indexed)
     * @param size page size
     * @return paginated positions
     */
    Page<PositionResponseDto> getPositions(
            String symbolCode,
            PositionStatus status,
            int page,
            int size
    );

    /**
     * Get open positions for a specific user.
     * 
     * @param userId the user ID
     * @return list of open positions
     */
    List<PositionResponseDto> getOpenPositions(String userId);

    /**
     * Get portfolio statistics for a user.
     * Includes total P&L, win rate, best/worst trades, etc.
     * 
     * @param userId the user ID
     * @return portfolio statistics
     */
    PortfolioStatsDto getPortfolioStats(String userId);
}
