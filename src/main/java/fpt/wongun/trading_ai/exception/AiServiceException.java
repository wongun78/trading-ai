package fpt.wongun.trading_ai.exception;

import org.springframework.http.HttpStatus;

public class AiServiceException extends TradingException {
    
    public AiServiceException(String message) {
        super(message, "AI_SERVICE_ERROR", HttpStatus.SERVICE_UNAVAILABLE);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, "AI_SERVICE_ERROR", HttpStatus.SERVICE_UNAVAILABLE, cause);
    }
}
