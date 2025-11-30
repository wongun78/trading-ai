package fpt.wongun.trading_ai.domain.entity;

import fpt.wongun.trading_ai.domain.enums.TradeResult;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "user_trade_feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTradeFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_signal_id")
    private AiSignal aiSignal;

    private boolean executed;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private TradeResult result;

    @Column(precision = 18, scale = 6)
    private BigDecimal pnl;

    @Column(length = 500)
    private String note;
}
