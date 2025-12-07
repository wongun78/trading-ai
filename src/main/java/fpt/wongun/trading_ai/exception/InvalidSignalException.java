package fpt.wongun.trading_ai.exception;

import org.springframework.http.HttpStatus;

public class InvalidSignalException extends TradingException {
    
    public InvalidSignalException(String message) {
        super(message, "INVALID_SIGNAL", HttpStatus.BAD_REQUEST);
    }

    public InvalidSignalException(String message, Throwable cause) {
        super(message, "INVALID_SIGNAL", HttpStatus.BAD_REQUEST, cause);
    }
}
