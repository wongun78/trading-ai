package fpt.wongun.trading_ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TradingAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradingAiApplication.class, args);
    }
}
