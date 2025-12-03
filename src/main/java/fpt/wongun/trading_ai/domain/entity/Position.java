package fpt.wongun.trading_ai.domain.entity;

import fpt.wongun.trading_ai.domain.enums.Direction;
import fpt.wongun.trading_ai.domain.enums.ExitReason;
import fpt.wongun.trading_ai.domain.enums.PositionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Trading position entity.
 * Tracks actual trade execution and P&L for signals that were traded.
 */
@Entity
@Table(name = "positions",
       indexes = {
           @Index(name = "idx_position_status", columnList = "status"),
           @Index(name = "idx_position_symbol", columnList = "symbol_id"),
           @Index(name = "idx_position_opened_at", columnList = "opened_at"),
           @Index(name = "idx_position_user", columnList = "created_by,status")
       })
@SQLDelete(sql = "UPDATE positions SET deleted = true, deleted_at = NOW(), deleted_by = 'SYSTEM' WHERE id = ? AND version = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the AI signal that triggered this position (optional - can be manual trade)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signal_id")
    private AiSignal signal;

    /**
     * Symbol being traded
     */
    @NotNull(message = "Symbol is required")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "symbol_id", nullable = false)
    private Symbol symbol;

    /**
     * Position status
     */
    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PositionStatus status;

    /**
     * Trade direction
     */
    @NotNull(message = "Direction is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Direction direction;

    /**
     * Planned entry price (from signal or manual)
     */
    @NotNull(message = "Planned entry price is required")
    @Column(nullable = false, precision = 28, scale = 18)
    private BigDecimal plannedEntryPrice;

    /**
     * Actual entry price (filled price)
     */
    @Column(precision = 28, scale = 18)
    private BigDecimal actualEntryPrice;

    /**
     * Stop loss price
     */
    @NotNull(message = "Stop loss is required")
    @Column(nullable = false, precision = 28, scale = 18)
    private BigDecimal stopLoss;

    /**
     * Take profit 1 price
     */
    @Column(precision = 28, scale = 18)
    private BigDecimal takeProfit1;

    /**
     * Take profit 2 price
     */
    @Column(precision = 28, scale = 18)
    private BigDecimal takeProfit2;

    /**
     * Take profit 3 price
     */
    @Column(precision = 28, scale = 18)
    private BigDecimal takeProfit3;

    /**
     * Exit price (actual close price)
     */
    @Column(precision = 28, scale = 18)
    private BigDecimal exitPrice;

    /**
     * Position size (number of contracts/shares/units)
     */
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    @Column(nullable = false, precision = 28, scale = 8)
    private BigDecimal quantity;

    /**
     * Realized profit/loss in quote currency (e.g., USDT)
     * Positive = profit, Negative = loss
     */
    @Column(precision = 28, scale = 8)
    private BigDecimal realizedPnL;

    /**
     * Realized P&L as percentage of entry
     */
    @Column(precision = 10, scale = 4)
    private BigDecimal realizedPnLPercent;

    /**
     * Risk/Reward ratio achieved (actual)
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal actualRiskReward;

    /**
     * Reason for closing position
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ExitReason exitReason;

    /**
     * When position was opened
     */
    private Instant openedAt;

    /**
     * When position was closed
     */
    private Instant closedAt;

    /**
     * Trading fees paid (entry + exit)
     */
    @Column(precision = 28, scale = 8)
    @Builder.Default
    private BigDecimal fees = BigDecimal.ZERO;

    /**
     * User notes about this trade
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Slippage in percentage
     */
    @Column(precision = 10, scale = 4)
    private BigDecimal slippage;

    /**
     * Duration of trade in milliseconds
     */
    private Long durationMs;

    /**
     * Pre-persist: Validate position logic
     */
    @PrePersist
    @PreUpdate
    private void validatePosition() {
        // Validate direction vs SL
        if (direction == Direction.LONG && stopLoss != null && plannedEntryPrice != null) {
            if (stopLoss.compareTo(plannedEntryPrice) >= 0) {
                throw new IllegalStateException("LONG position must have SL < entry price");
            }
        }
        if (direction == Direction.SHORT && stopLoss != null && plannedEntryPrice != null) {
            if (stopLoss.compareTo(plannedEntryPrice) <= 0) {
                throw new IllegalStateException("SHORT position must have SL > entry price");
            }
        }

        // Calculate duration if closed
        if (status == PositionStatus.CLOSED && openedAt != null && closedAt != null) {
            durationMs = closedAt.toEpochMilli() - openedAt.toEpochMilli();
        }

        // Calculate slippage if both planned and actual entry exist
        if (actualEntryPrice != null && plannedEntryPrice != null && plannedEntryPrice.compareTo(BigDecimal.ZERO) > 0) {
            slippage = actualEntryPrice.subtract(plannedEntryPrice)
                    .divide(plannedEntryPrice, 6, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .abs();
        }
    }

    /**
     * Calculate realized P&L after position is closed
     */
    public void calculateRealizedPnL() {
        if (actualEntryPrice == null || exitPrice == null || quantity == null) {
            return;
        }

        BigDecimal priceDiff;
        if (direction == Direction.LONG) {
            priceDiff = exitPrice.subtract(actualEntryPrice);
        } else { // SHORT
            priceDiff = actualEntryPrice.subtract(exitPrice);
        }

        // P&L = priceDiff * quantity - fees
        realizedPnL = priceDiff.multiply(quantity).subtract(fees != null ? fees : BigDecimal.ZERO);

        // P&L percentage
        if (actualEntryPrice.compareTo(BigDecimal.ZERO) > 0) {
            realizedPnLPercent = priceDiff
                    .divide(actualEntryPrice, 6, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // Actual R:R
        if (stopLoss != null && actualEntryPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal risk = actualEntryPrice.subtract(stopLoss).abs();
            BigDecimal reward = priceDiff.abs();
            if (risk.compareTo(BigDecimal.ZERO) > 0) {
                actualRiskReward = reward.divide(risk, 2, java.math.RoundingMode.HALF_UP);
            }
        }
    }
}
