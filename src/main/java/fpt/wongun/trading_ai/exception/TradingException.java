package fpt.wongun.trading_ai.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception for all trading-related errors.
 * Includes error code and HTTP status for consistent API responses.
 */
@Getter
public class TradingException extends RuntimeException {
    
    private final String errorCode;
    private final HttpStatus httpStatus;

    public TradingException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public TradingException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
