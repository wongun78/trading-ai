package fpt.wongun.trading_ai.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "candles",
       uniqueConstraints = {
           @UniqueConstraint(
               name = "uk_candle_symbol_timeframe_timestamp",
               columnNames = {"symbol_id", "timeframe", "timestamp"}
           )
       },
       indexes = {
           @Index(name = "idx_candle_symbol_tf_time", columnList = "symbol_id,timeframe,timestamp")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candle extends BaseEntity {

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

    @NotNull(message = "Timestamp is required")
    @Column(nullable = false)
    private Instant timestamp;

    @NotNull(message = "Open price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Open price must be positive")
    @Column(nullable = false, precision = 28, scale = 18)
    private BigDecimal open;

    @NotNull(message = "High price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "High price must be positive")
    @Column(nullable = false, precision = 28, scale = 18)
    private BigDecimal high;

    @NotNull(message = "Low price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Low price must be positive")
    @Column(nullable = false, precision = 28, scale = 18)
    private BigDecimal low;

    @NotNull(message = "Close price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Close price must be positive")
    @Column(nullable = false, precision = 28, scale = 18)
    private BigDecimal close;

    @NotNull(message = "Volume is required")
    @DecimalMin(value = "0.0", message = "Volume cannot be negative")
    @Column(nullable = false, precision = 28, scale = 18)
    private BigDecimal volume;
}
