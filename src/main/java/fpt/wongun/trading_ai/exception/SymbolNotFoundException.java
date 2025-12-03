package fpt.wongun.trading_ai.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a trading symbol is not found in the database.
 */
public class SymbolNotFoundException extends TradingException {
    
    public SymbolNotFoundException(String symbolCode) {
        super(
                "Symbol '%s' not found. Please check the symbol code.".formatted(symbolCode),
            "SYMBOL_NOT_FOUND",
            HttpStatus.NOT_FOUND
        );
    }
}
