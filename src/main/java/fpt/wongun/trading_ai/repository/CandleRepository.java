package fpt.wongun.trading_ai.repository;

import fpt.wongun.trading_ai.domain.entity.Candle;
import fpt.wongun.trading_ai.domain.entity.Symbol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandleRepository extends JpaRepository<Candle, Long> {

    List<Candle> findTop200BySymbolAndTimeframeOrderByTimestampDesc(Symbol symbol, String timeframe);
    
    long deleteBySymbolAndTimeframe(Symbol symbol, String timeframe);
    
    long deleteBySymbol(Symbol symbol);
    
    long countBySymbolAndTimeframe(Symbol symbol, String timeframe);
}
