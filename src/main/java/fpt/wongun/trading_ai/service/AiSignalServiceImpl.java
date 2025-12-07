package fpt.wongun.trading_ai.service;

import fpt.wongun.trading_ai.domain.entity.AiSignal;
import fpt.wongun.trading_ai.domain.entity.Symbol;
import fpt.wongun.trading_ai.domain.enums.Direction;
import fpt.wongun.trading_ai.dto.AiSignalResponseDto;
import fpt.wongun.trading_ai.dto.AiSuggestRequestDto;
import fpt.wongun.trading_ai.exception.InvalidSignalException;
import fpt.wongun.trading_ai.exception.MarketDataException;
import fpt.wongun.trading_ai.exception.SymbolNotFoundException;
import fpt.wongun.trading_ai.repository.AiSignalRepository;
import fpt.wongun.trading_ai.repository.SymbolRepository;
import fpt.wongun.trading_ai.service.ai.AiClient;
import fpt.wongun.trading_ai.service.ai.TradeSuggestion;
import fpt.wongun.trading_ai.service.analysis.MarketAnalysisService;
import fpt.wongun.trading_ai.service.analysis.TradeAnalysisContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiSignalServiceImpl implements IAiSignalService {

    private final SymbolRepository symbolRepository;
    private final AiSignalRepository aiSignalRepository;
    private final MarketAnalysisService marketAnalysisService;
    private final AiClient aiClient;

    @Cacheable(value = "aiSignals", 
               key = "#request.symbolCode + '_' + #request.timeframe + '_' + #request.mode", 
               unless = "#result.reasoning != null && #result.reasoning.contains('unavailable')")
    @Transactional
    public AiSignalResponseDto generateSignal(AiSuggestRequestDto request) {
        log.info("Generating AI signal for {}/{}/{}", 
                request.getSymbolCode(), request.getTimeframe(), request.getMode());
        
        // 1. Validate symbol exists
        Symbol symbol = symbolRepository.findByCode(request.getSymbolCode())
                .orElseThrow(() -> new SymbolNotFoundException(request.getSymbolCode()));

        // 2. Build market analysis context
        TradeAnalysisContext context = marketAnalysisService.buildContext(symbol, request.getTimeframe());
        
        if (context.getCandles() == null || context.getCandles().isEmpty()) {
            throw new MarketDataException(
                "No market data available for " + request.getSymbolCode() + "/" + request.getTimeframe()
            );
        }

        // 3. Get AI suggestion
        TradeSuggestion suggestion = aiClient.suggestTrade(context, request.getMode().name());

        // 4. Validate AI suggestion
        validateSignal(suggestion);

        // 5. Save to database (auditing fields filled automatically)
        AiSignal entity = AiSignal.builder()
                .symbol(symbol)
                .timeframe(request.getTimeframe())
                .direction(suggestion.getDirection())
                .entryPrice(suggestion.getEntryPrice())
                .stopLoss(suggestion.getStopLoss())
                .takeProfit1(suggestion.getTakeProfit1())
                .takeProfit2(suggestion.getTakeProfit2())
                .takeProfit3(suggestion.getTakeProfit3())
                .riskReward1(suggestion.getRiskReward1())
                .riskReward2(suggestion.getRiskReward2())
                .riskReward3(suggestion.getRiskReward3())
                .reasoning(suggestion.getReasoning())
                .build();

        entity = aiSignalRepository.save(entity);
        
        log.info("Signal generated successfully: {} for {}", 
                entity.getDirection(), request.getSymbolCode());
        
        return mapToDto(entity);
    }

    @Transactional(readOnly = true)
    public Page<AiSignalResponseDto> getSignals(String symbolCode,
                                                String timeframe,
                                                Instant from,
                                                Instant to,
                                                int page,
                                                int size) {
        Symbol symbol = symbolRepository.findByCode(symbolCode)
                .orElseThrow(() -> new SymbolNotFoundException(symbolCode));

        if (from == null) {
            from = Instant.EPOCH;
        }
        if (to == null) {
            to = Instant.now();
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return aiSignalRepository
                .findBySymbolAndTimeframeAndCreatedAtBetween(symbol, timeframe, from, to, pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public AiSignalResponseDto getSignalById(Long signalId) {
        AiSignal signal = aiSignalRepository.findById(signalId)
                .orElseThrow(() -> new InvalidSignalException("Signal not found with id: " + signalId));
        return mapToDto(signal);
    }

    private void validateSignal(TradeSuggestion signal) {
        if (signal.getDirection() == null) {
            throw new InvalidSignalException("AI returned null direction");
        }

        // NEUTRAL signals don't need entry/SL validation
        if (signal.getDirection() == Direction.NEUTRAL) {
            return;
        }

        // LONG/SHORT must have entry and stopLoss
        if (signal.getEntryPrice() == null) {
            throw new InvalidSignalException(
                signal.getDirection() + " signal missing entry price"
            );
        }

        if (signal.getStopLoss() == null) {
            throw new InvalidSignalException(
                signal.getDirection() + " signal missing stop loss"
            );
        }

        // Validate stop loss is in correct direction
        if (signal.getDirection() == Direction.LONG && 
            signal.getStopLoss().compareTo(signal.getEntryPrice()) >= 0) {
            throw new InvalidSignalException(
                "LONG signal: stop loss must be below entry price. Entry=" + 
                signal.getEntryPrice() + ", SL=" + signal.getStopLoss()
            );
        }

        if (signal.getDirection() == Direction.SHORT && 
            signal.getStopLoss().compareTo(signal.getEntryPrice()) <= 0) {
            throw new InvalidSignalException(
                "SHORT signal: stop loss must be above entry price. Entry=" + 
                signal.getEntryPrice() + ", SL=" + signal.getStopLoss()
            );
        }

        // Validate at least TP1 exists
        if (signal.getTakeProfit1() == null) {
            throw new InvalidSignalException("Signal missing take profit level");
        }

        // Validate R:R ratio (must be > 1.0 for valid setups)
        if (signal.getRiskReward1() != null && 
            signal.getRiskReward1().compareTo(BigDecimal.ONE) < 0) {
            log.warn("Low R:R ratio detected: {}. Signal may not be worth taking.", 
                    signal.getRiskReward1());
        }
    }

    private AiSignalResponseDto mapToDto(AiSignal s) {
        return AiSignalResponseDto.builder()
                .id(s.getId())
                .symbolCode(s.getSymbol().getCode())
                .timeframe(s.getTimeframe())
                .direction(s.getDirection())
                .entryPrice(s.getEntryPrice())
                .stopLoss(s.getStopLoss())
                .takeProfit1(s.getTakeProfit1())
                .takeProfit2(s.getTakeProfit2())
                .takeProfit3(s.getTakeProfit3())
                .riskReward1(s.getRiskReward1())
                .riskReward2(s.getRiskReward2())
                .riskReward3(s.getRiskReward3())
                .reasoning(s.getReasoning())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
