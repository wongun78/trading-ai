package fpt.wongun.trading_ai.domain.entity;

import fpt.wongun.trading_ai.domain.enums.Direction;
import fpt.wongun.trading_ai.domain.enums.ExitReason;
import fpt.wongun.trading_ai.domain.enums.PositionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "positions",
       indexes = {
           @Index(name = "idx_position_status", columnList = "status"),
           @Index(name = "idx_position_symbol", columnList = "symbol_id"),
           @Index(name = "idx_position_opened_at", columnList = "opened_at"),
           @Index(name = "idx_position_user", columnList = "created_by,status")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signal_id")
    private AiSignal signal;

    @NotNull(message = "Symbol is required")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "symbol_id", nullable = false)
    private Symbol symbol;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PositionStatus status;

    @NotNull(message = "Direction is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Direction direction;

    @NotNull(message = "Planned entry price is required")
    @Column(nullable = false, precision = 28, scale = 18)
    private BigDecimal plannedEntryPrice;

    @Column(precision = 28, scale = 18)
    private BigDecimal actualEntryPrice;

    @NotNull(message = "Stop loss is required")
    @Column(nullable = false, precision = 28, scale = 18)
    private BigDecimal stopLoss;

    @Column(precision = 28, scale = 18)
    private BigDecimal takeProfit;

    @Column(precision = 28, scale = 18)
    private BigDecimal exitPrice;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    @Column(nullable = false, precision = 28, scale = 8)
    private BigDecimal quantity;

    @Column(precision = 28, scale = 8)
    private BigDecimal realizedPnL;

    @Column(precision = 10, scale = 4)
    private BigDecimal realizedPnLPercent;

    @Column(precision = 10, scale = 2)
    private BigDecimal actualRiskReward;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ExitReason exitReason;

    private Instant openedAt;

    private Instant closedAt;

    @Column(precision = 28, scale = 8)
    @Builder.Default
    private BigDecimal fees = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(precision = 10, scale = 4)
    private BigDecimal slippage;

    private Long durationMs;

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
