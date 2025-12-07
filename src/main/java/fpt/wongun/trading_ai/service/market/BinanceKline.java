package fpt.wongun.trading_ai.service.market;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BinanceKline {
    private Long openTime;        // 0 - Kline open time (milliseconds)
    private BigDecimal open;      // 1 - Open price
    private BigDecimal high;      // 2 - High price
    private BigDecimal low;       // 3 - Low price
    private BigDecimal close;     // 4 - Close price
    private BigDecimal volume;    // 5 - Volume
    private Long closeTime;       // 6 - Kline close time
    private BigDecimal quoteVolume; // 7 - Quote asset volume
    private Integer trades;       // 8 - Number of trades
    private BigDecimal takerBuyBaseVolume; // 9
    private BigDecimal takerBuyQuoteVolume; // 10
    private String ignore;        // 11

    public static BinanceKline fromArray(Object[] arr) {
        BinanceKline kline = new BinanceKline();
        kline.setOpenTime(Long.parseLong(arr[0].toString()));
        kline.setOpen(new BigDecimal(arr[1].toString()));
        kline.setHigh(new BigDecimal(arr[2].toString()));
        kline.setLow(new BigDecimal(arr[3].toString()));
        kline.setClose(new BigDecimal(arr[4].toString()));
        kline.setVolume(new BigDecimal(arr[5].toString()));
        kline.setCloseTime(Long.parseLong(arr[6].toString()));
        kline.setQuoteVolume(new BigDecimal(arr[7].toString()));
        kline.setTrades(Integer.parseInt(arr[8].toString()));
        kline.setTakerBuyBaseVolume(new BigDecimal(arr[9].toString()));
        kline.setTakerBuyQuoteVolume(new BigDecimal(arr[10].toString()));
        kline.setIgnore(arr[11].toString());
        return kline;
    }
}
