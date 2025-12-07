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

public interface PositionRepository extends JpaRepository<Position, Long> {

    Page<Position> findByStatusOrderByOpenedAtDesc(PositionStatus status, Pageable pageable);

    Page<Position> findBySymbolAndStatusOrderByOpenedAtDesc(
            Symbol symbol, 
            PositionStatus status, 
            Pageable pageable
    );

    Page<Position> findByCreatedByAndStatusOrderByOpenedAtDesc(
            String createdBy, 
            PositionStatus status, 
            Pageable pageable
    );

    Page<Position> findByCreatedByAndSymbolAndStatusOrderByOpenedAtDesc(
            String createdBy,
            Symbol symbol,
            PositionStatus status,
            Pageable pageable
    );

    Page<Position> findByCreatedByAndSymbolOrderByOpenedAtDesc(
            String createdBy,
            Symbol symbol,
            Pageable pageable
    );

    Page<Position> findByCreatedByOrderByOpenedAtDesc(String createdBy, Pageable pageable);

    List<Position> findByCreatedByAndStatus(String createdBy, PositionStatus status);

    Page<Position> findByOpenedAtBetweenOrderByOpenedAtDesc(
            Instant startDate, 
            Instant endDate, 
            Pageable pageable
    );

    long countByCreatedByAndStatus(String createdBy, PositionStatus status);

    @Query("SELECT COALESCE(SUM(p.realizedPnL), 0) FROM Position p " +
           "WHERE p.createdBy = :createdBy AND p.status = 'CLOSED'")
    BigDecimal getTotalPnLByUser(@Param("createdBy") String createdBy);

    @Query("SELECT " +
           "CAST(COUNT(CASE WHEN p.realizedPnL > 0 THEN 1 END) AS double) / " +
           "CAST(NULLIF(COUNT(*), 0) AS double) * 100.0 " +
           "FROM Position p " +
           "WHERE p.createdBy = :createdBy AND p.status = 'CLOSED'")
    Double getWinRateByUser(@Param("createdBy") String createdBy);

    @Query("SELECT " +
           "CAST(COUNT(CASE WHEN p.realizedPnL > 0 THEN 1 END) AS double) / " +
           "CAST(NULLIF(COUNT(*), 0) AS double) * 100.0 " +
           "FROM Position p " +
           "WHERE p.createdBy = :createdBy AND p.status = 'CLOSED' AND p.direction = :direction")
    Double getWinRateByDirection(
            @Param("createdBy") String createdBy, 
            @Param("direction") Direction direction
    );

    @Query("SELECT COALESCE(AVG(p.realizedPnL), 0) FROM Position p " +
           "WHERE p.createdBy = :createdBy AND p.status = 'CLOSED'")
    BigDecimal getAveragePnLByUser(@Param("createdBy") String createdBy);

    @Query("SELECT p FROM Position p " +
           "WHERE p.createdBy = :createdBy AND p.status = 'CLOSED' " +
           "ORDER BY p.realizedPnL DESC")
    List<Position> getBestTrades(@Param("createdBy") String createdBy, Pageable pageable);

    @Query("SELECT p FROM Position p " +
           "WHERE p.createdBy = :createdBy AND p.status = 'CLOSED' " +
           "ORDER BY p.realizedPnL ASC")
    List<Position> getWorstTrades(@Param("createdBy") String createdBy, Pageable pageable);

    @Query("SELECT COALESCE(AVG(p.actualRiskReward), 0) FROM Position p " +
           "WHERE p.createdBy = :createdBy AND p.status = 'CLOSED' " +
           "AND p.actualRiskReward IS NOT NULL")
    BigDecimal getAverageRiskReward(@Param("createdBy") String createdBy);

    long countByCreatedByAndStatus(String createdBy, PositionStatus... statuses);

    @Query("SELECT COALESCE(SUM(p.realizedPnL), 0) FROM Position p " +
           "WHERE p.createdBy = :createdBy AND p.symbol = :symbol AND p.status = 'CLOSED'")
    BigDecimal getPnLBySymbol(@Param("createdBy") String createdBy, @Param("symbol") Symbol symbol);

    List<Position> findBySignalId(Long signalId);
}
