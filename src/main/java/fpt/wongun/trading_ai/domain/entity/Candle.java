package fpt.wongun.trading_ai.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "candles",
       indexes = {
           @Index(name = "idx_candle_symbol_tf_time", columnList = "symbol_id,timeframe,timestamp")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "symbol_id")
    private Symbol symbol;

    @Column(nullable = false, length = 10)
    private String timeframe; // M1, M5, M15...

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal open;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal high;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal low;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal close;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal volume;
}
