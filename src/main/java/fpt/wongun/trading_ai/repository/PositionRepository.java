package fpt.wongun.trading_ai.repository;

import fpt.wongun.trading_ai.domain.entity.Position;
import fpt.wongun.trading_ai.domain.entity.Symbol;
import fpt.wongun.trading_ai.domain.enums.Direction;
import fpt.wongun.trading_ai.domain.enums.PositionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Repository for Position entity.
 * Provides queries for portfolio management and performance analytics.
 */
public interface PositionRepository extends JpaRepository<Position, Long> {

    /**
     * Find all positions with pagination and filtering
     */
    Page<Position> findByStatusOrderByOpenedAtDesc(PositionStatus status, Pageable pageable);

    /**
     * Find positions by symbol and status
     */
    Page<Position> findBySymbolAndStatusOrderByOpenedAtDesc(
            Symbol symbol, 
            PositionStatus status, 
            Pageable pageable
    );

    /**
     * Find positions by user (createdBy) and status
     */
    Page<Position> findByCreatedByAndStatusOrderByOpenedAtDesc(
            String createdBy, 
            PositionStatus status, 
            Pageable pageable
    );

    /**
     * Find all open positions for a user
     */
    List<Position> findByCreatedByAndStatus(String createdBy, PositionStatus status);

    /**
     * Find positions opened within a date range
     */
    Page<Position> findByOpenedAtBetweenOrderByOpenedAtDesc(
            Instant startDate, 
            Instant endDate, 
            Pageable pageable
    );

    /**
     * Count positions by status for a user
     */
    long countByCreatedByAndStatus(String createdBy, PositionStatus status);

    /**
     * Get total P&L for a user
     */
    @Query("SELECT COALESCE(SUM(p.realizedPnL), 0) FROM Position p " +
           "WHERE p.createdBy = :createdBy AND p.status = 'CLOSED'")
    BigDecimal getTotalPnLByUser(@Param("createdBy") String createdBy);

    /**
     * Get win rate (percentage of profitable closed positions)
     */
    @Query("SELECT " +
           "CAST(COUNT(CASE WHEN p.realizedPnL > 0 THEN 1 END) AS double) / " +
           "CAST(NULLIF(COUNT(*), 0) AS double) * 100.0 " +
           "FROM Position p " +
           "WHERE p.createdBy = :createdBy AND p.status = 'CLOSED'")
    Double getWinRateByUser(@Param("createdBy") String createdBy);

    /**
     * Get win rate by direction
     */
    @Query("SELECT " +
           "CAST(COUNT(CASE WHEN p.realizedPnL > 0 THEN 1 END) AS double) / " +
           "CAST(NULLIF(COUNT(*), 0) AS double) * 100.0 " +
           "FROM Position p " +
           "WHERE p.createdBy = :createdBy AND p.status = 'CLOSED' AND p.direction = :direction")
    Double getWinRateByDirection(
            @Param("createdBy") String createdBy, 
            @Param("direction") Direction direction
    );

    /**
     * Get average P&L per trade
     */
    @Query("SELECT COALESCE(AVG(p.realizedPnL), 0) FROM Position p " +
           "WHERE p.createdBy = :createdBy AND p.status = 'CLOSED'")
    BigDecimal getAveragePnLByUser(@Param("createdBy") String createdBy);

    /**
     * Get best trade (highest P&L)
     */
    @Query("SELECT p FROM Position p " +
           "WHERE p.createdBy = :createdBy AND p.status = 'CLOSED' " +
           "ORDER BY p.realizedPnL DESC")
    List<Position> getBestTrades(@Param("createdBy") String createdBy, Pageable pageable);

    /**
     * Get worst trade (lowest P&L)
     */
    @Query("SELECT p FROM Position p " +
           "WHERE p.createdBy = :createdBy AND p.status = 'CLOSED' " +
           "ORDER BY p.realizedPnL ASC")
    List<Position> getWorstTrades(@Param("createdBy") String createdBy, Pageable pageable);

    /**
     * Get average R:R for closed positions
     */
    @Query("SELECT COALESCE(AVG(p.actualRiskReward), 0) FROM Position p " +
           "WHERE p.createdBy = :createdBy AND p.status = 'CLOSED' " +
           "AND p.actualRiskReward IS NOT NULL")
    BigDecimal getAverageRiskReward(@Param("createdBy") String createdBy);

    /**
     * Get total trades count
     */
    long countByCreatedByAndStatus(String createdBy, PositionStatus... statuses);

    /**
     * Get P&L by symbol
     */
    @Query("SELECT COALESCE(SUM(p.realizedPnL), 0) FROM Position p " +
           "WHERE p.createdBy = :createdBy AND p.symbol = :symbol AND p.status = 'CLOSED'")
    BigDecimal getPnLBySymbol(@Param("createdBy") String createdBy, @Param("symbol") Symbol symbol);

    /**
     * Find positions by signal ID
     */
    List<Position> findBySignalId(Long signalId);
}
