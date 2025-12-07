package fpt.wongun.trading_ai.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends TradingException {

    public ForbiddenException(String message) {
        super("FORBIDDEN", message, HttpStatus.FORBIDDEN);
    }

    public ForbiddenException(String code, String message) {
        super(code, message, HttpStatus.FORBIDDEN);
    }

    public static ForbiddenException ownershipViolation(String resourceType) {
        return new ForbiddenException(
            "OWNERSHIP_VIOLATION",
            "You can only manage your own " + resourceType + ". " +
            "This resource belongs to another user."
        );
    }

    public static ForbiddenException insufficientRole(String requiredRole) {
        return new ForbiddenException(
            "INSUFFICIENT_ROLE",
            "This action requires " + requiredRole + " role"
        );
    }
}
