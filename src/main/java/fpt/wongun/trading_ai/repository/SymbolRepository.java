package fpt.wongun.trading_ai.repository;

import fpt.wongun.trading_ai.domain.entity.Symbol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SymbolRepository extends JpaRepository<Symbol, Long> {
    Optional<Symbol> findByCode(String code);
}
