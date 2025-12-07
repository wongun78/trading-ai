package fpt.wongun.trading_ai.exception;

import org.springframework.http.HttpStatus;

public class PositionNotFoundException extends TradingException {
    
    public PositionNotFoundException(Long positionId) {
        super("Position with ID " + positionId + " not found", 
              "POSITION_NOT_FOUND", 
              HttpStatus.NOT_FOUND);
    }
}
