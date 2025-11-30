package fpt.wongun.trading_ai.domain.entity;

import fpt.wongun.trading_ai.domain.enums.SymbolType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "symbols")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Symbol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code; // e.g. BTCUSDT, XAUUSD

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SymbolType type;

    @Column(length = 255)
    private String description;
}
