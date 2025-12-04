package fpt.wongun.trading_ai.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when user attempts to access a resource they don't have permission for.
 * Returns HTTP 403 Forbidden.
 * 
 * Examples:
 * - Accessing another user's private data
 * - Modifying resources without required role
 * - Ownership violations
 */
public class ForbiddenException extends TradingException {

    public ForbiddenException(String message) {
        super("FORBIDDEN", message, HttpStatus.FORBIDDEN);
    }

    public ForbiddenException(String code, String message) {
        super(code, message, HttpStatus.FORBIDDEN);
    }

    /**
     * Create exception for ownership violation.
     * Used when user tries to modify resources they don't own.
     * 
     * @param resourceType the type of resource (e.g., "positions", "signals")
     * @return ForbiddenException with ownership message
     */
    public static ForbiddenException ownershipViolation(String resourceType) {
        return new ForbiddenException(
            "OWNERSHIP_VIOLATION",
            "You can only manage your own " + resourceType + ". " +
            "This resource belongs to another user."
        );
    }

    /**
     * Create exception for insufficient role.
     * 
     * @param requiredRole the role required to perform the action
     * @return ForbiddenException with role requirement message
     */
    public static ForbiddenException insufficientRole(String requiredRole) {
        return new ForbiddenException(
            "INSUFFICIENT_ROLE",
            "This action requires " + requiredRole + " role"
        );
    }
}
