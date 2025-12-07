package fpt.wongun.trading_ai.domain.entity;

import fpt.wongun.trading_ai.domain.enums.Direction;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Table(name = "ai_signals",
       indexes = {
           @Index(name = "idx_signal_symbol_tf_time", columnList = "symbol_id,timeframe,created_at"),
           @Index(name = "idx_signal_direction", columnList = "direction,created_at")
       })
@SQLDelete(sql = "UPDATE ai_signals SET deleted = true, deleted_at = NOW(), deleted_by = 'SYSTEM' WHERE id = ? AND version = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiSignal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Symbol is required")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "symbol_id", nullable = false)
    private Symbol symbol;

    @NotNull(message = "Timeframe is required")
    @Column(nullable = false, length = 10)
    private String timeframe;

    @NotNull(message = "Direction is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Direction direction;

    @DecimalMin(value = "0.0", inclusive = false, message = "Entry price must be positive")
    @Column(precision = 28, scale = 18, nullable = true)
    private BigDecimal entryPrice;

    @DecimalMin(value = "0.0", inclusive = false, message = "Stop loss must be positive")
    @Column(precision = 28, scale = 18, nullable = true)
    private BigDecimal stopLoss;

    @Column(precision = 28, scale = 18)
    private BigDecimal takeProfit1;

    @Column(precision = 28, scale = 18)
    private BigDecimal takeProfit2;

    @Column(precision = 28, scale = 18)
    private BigDecimal takeProfit3;

    @Column(precision = 10, scale = 2)
    private BigDecimal riskReward1;

    @Column(precision = 10, scale = 2)
    private BigDecimal riskReward2;

    @Column(precision = 10, scale = 2)
    private BigDecimal riskReward3;

    @Column(columnDefinition = "TEXT")
    private String reasoning; // JSON or long text

    @PrePersist
    @PreUpdate
    private void validateSignal() {
        if (direction != Direction.NEUTRAL) {
            if (entryPrice == null || stopLoss == null) {
                throw new IllegalStateException(
                    direction + " signals must have both entryPrice and stopLoss. Got entryPrice=" 
                    + entryPrice + ", stopLoss=" + stopLoss
                );
            }
        }
    }
}
