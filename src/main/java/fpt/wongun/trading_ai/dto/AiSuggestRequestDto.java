package fpt.wongun.trading_ai.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiSuggestRequestDto {

    @NotBlank
    private String symbolCode;

    @NotBlank
    private String timeframe; // e.g. M5

    @NotBlank
    private String mode; // SCALPING, INTRADAY...

    @DecimalMin("0.0")
    private BigDecimal maxRiskPerTrade; // percent or R, up to you
}
