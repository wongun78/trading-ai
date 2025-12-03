package fpt.wongun.trading_ai.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an invalid position operation is attempted.
 */
public class InvalidPositionException extends TradingException {
    
    public InvalidPositionException(String message) {
        super(message, "INVALID_POSITION_OPERATION", HttpStatus.BAD_REQUEST);
    }
}
