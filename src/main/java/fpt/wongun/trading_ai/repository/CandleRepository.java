package fpt.wongun.trading_ai.repository;

import fpt.wongun.trading_ai.domain.entity.Candle;
import fpt.wongun.trading_ai.domain.entity.Symbol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CandleRepository extends JpaRepository<Candle, Long> {

    List<Candle> findTop200BySymbolAndTimeframeOrderByTimestampDesc(Symbol symbol, String timeframe);
    
    List<Candle> findBySymbolAndTimeframe(Symbol symbol, String timeframe);
    
    List<Candle> findTop1BySymbolOrderByTimestampDesc(Symbol symbol);
    
    long deleteBySymbolAndTimeframe(Symbol symbol, String timeframe);
    
    long deleteBySymbol(Symbol symbol);
    
    long countBySymbolAndTimeframe(Symbol symbol, String timeframe);
    
    /**
     * Hard delete candles (ignoring soft delete) to avoid unique constraint violations.
     * Used by BinanceSyncScheduler before inserting fresh data.
     */
    @Modifying
    @Query(value = "DELETE FROM candles WHERE symbol_id = :#{#symbol.id} AND timeframe = :timeframe", nativeQuery = true)
    int hardDeleteBySymbolAndTimeframe(@Param("symbol") Symbol symbol, @Param("timeframe") String timeframe);
}
