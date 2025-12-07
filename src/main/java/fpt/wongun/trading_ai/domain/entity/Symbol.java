package fpt.wongun.trading_ai.domain.entity;

import fpt.wongun.trading_ai.domain.enums.SymbolType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "symbols")
@SQLDelete(sql = "UPDATE symbols SET deleted = true, deleted_at = NOW(), deleted_by = 'SYSTEM' WHERE id = ? AND version = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Symbol extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Symbol code is required")
    @Pattern(regexp = "^[A-Z0-9]{3,12}$", message = "Symbol code must be 3-12 uppercase alphanumeric characters")
    @Column(nullable = false, unique = true, length = 50)
    private String code; // e.g. BTCUSDT, XAUUSD

    @NotNull(message = "Symbol type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SymbolType type;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(precision = 10, scale = 8)
    private java.math.BigDecimal tickSize;  // Min price movement (0.01)

    @Column(precision = 10, scale = 8)
    private java.math.BigDecimal lotSize;   // Min order size (0.001 BTC)

    @Column(precision = 18, scale = 6)
    private java.math.BigDecimal minNotional;  // Min order value ($10)
}
