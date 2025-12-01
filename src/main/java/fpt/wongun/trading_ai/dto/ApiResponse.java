package fpt.wongun.trading_ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standard API response wrapper for consistent response format.
 * 
 * Success response:
 * {
 *   "success": true,
 *   "data": {...},
 *   "timestamp": "2025-12-01T10:00:00Z"
 * }
 * 
 * Error response:
 * {
 *   "success": false,
 *   "error": {
 *     "code": "SYMBOL_NOT_FOUND",
 *     "message": "Symbol 'XYZ' not found"
 *   },
 *   "timestamp": "2025-12-01T10:00:00Z"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private boolean success;
    private T data;
    private ErrorDetail error;
    
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Create a successful response
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .build();
    }

    /**
     * Create an error response
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .error(ErrorDetail.builder()
                .code(code)
                .message(message)
                .build())
            .build();
    }

    /**
     * Error detail nested object
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetail {
        private String code;
        private String message;
        private Object details;
    }
}
