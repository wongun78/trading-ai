package fpt.wongun.trading_ai.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when AI generates an invalid trading signal.
 * This indicates the signal violates Volman Guards or business rules.
 */
public class InvalidSignalException extends TradingException {
    
    public InvalidSignalException(String message) {
        super(message, "INVALID_SIGNAL", HttpStatus.BAD_REQUEST);
    }

    public InvalidSignalException(String message, Throwable cause) {
        super(message, "INVALID_SIGNAL", HttpStatus.BAD_REQUEST, cause);
    }
}
