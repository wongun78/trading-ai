package fpt.wongun.trading_ai.domain.entity;

import fpt.wongun.trading_ai.domain.enums.Direction;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ai_signals",
       indexes = {
           @Index(name = "idx_signal_symbol_tf_time", columnList = "symbol_id,timeframe,createdAt")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "symbol_id")
    private Symbol symbol;

    @Column(nullable = false, length = 10)
    private String timeframe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Direction direction;

    @Column(precision = 18, scale = 6, nullable = true)
    private BigDecimal entryPrice;

    @Column(precision = 18, scale = 6, nullable = true)
    private BigDecimal stopLoss;

    @Column(precision = 18, scale = 6)
    private BigDecimal takeProfit1;

    @Column(precision = 18, scale = 6)
    private BigDecimal takeProfit2;

    @Column(precision = 18, scale = 6)
    private BigDecimal takeProfit3;

    @Column(precision = 18, scale = 6)
    private BigDecimal riskReward1;

    @Column(precision = 18, scale = 6)
    private BigDecimal riskReward2;

    @Column(precision = 18, scale = 6)
    private BigDecimal riskReward3;

    @Column(columnDefinition = "TEXT")
    private String reasoning; // JSON or long text

    @Column(nullable = false)
    private Instant createdAt;

    @Column(length = 100)
    private String createdBy;
}
