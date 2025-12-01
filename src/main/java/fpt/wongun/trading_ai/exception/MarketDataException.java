package fpt.wongun.trading_ai.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when market data is unavailable or insufficient.
 */
public class MarketDataException extends TradingException {
    
    public MarketDataException(String message) {
        super(message, "MARKET_DATA_ERROR", HttpStatus.SERVICE_UNAVAILABLE);
    }

    public MarketDataException(String message, Throwable cause) {
        super(message, "MARKET_DATA_ERROR", HttpStatus.SERVICE_UNAVAILABLE, cause);
    }
}
