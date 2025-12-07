package fpt.wongun.trading_ai.exception;

import org.springframework.http.HttpStatus;

public class ResourceAlreadyExistsException extends TradingException {

    public ResourceAlreadyExistsException(String message) {
        super(message, "RESOURCE_ALREADY_EXISTS", HttpStatus.CONFLICT);
    }

    public ResourceAlreadyExistsException(String resource, String field, Object value) {
        super(String.format("%s already exists with %s: %s", resource, field, value), 
              "RESOURCE_ALREADY_EXISTS", 
              HttpStatus.CONFLICT);
    }
}
