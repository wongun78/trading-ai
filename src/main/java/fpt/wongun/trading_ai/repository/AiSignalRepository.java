package fpt.wongun.trading_ai.repository;

import fpt.wongun.trading_ai.domain.entity.AiSignal;
import fpt.wongun.trading_ai.domain.entity.Symbol;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface AiSignalRepository extends JpaRepository<AiSignal, Long> {

    Page<AiSignal> findBySymbolAndTimeframeAndCreatedAtBetween(
            Symbol symbol,
            String timeframe,
            Instant from,
            Instant to,
            Pageable pageable
    );
}
