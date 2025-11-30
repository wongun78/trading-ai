package fpt.wongun.trading_ai.service;

import fpt.wongun.trading_ai.domain.entity.AiSignal;
import fpt.wongun.trading_ai.domain.entity.Symbol;
import fpt.wongun.trading_ai.domain.enums.Direction;
import fpt.wongun.trading_ai.dto.AiSignalResponseDto;
import fpt.wongun.trading_ai.dto.AiSuggestRequestDto;
import fpt.wongun.trading_ai.repository.AiSignalRepository;
import fpt.wongun.trading_ai.repository.SymbolRepository;
import fpt.wongun.trading_ai.service.ai.AiClient;
import fpt.wongun.trading_ai.service.ai.TradeSuggestion;
import fpt.wongun.trading_ai.service.analysis.MarketAnalysisService;
import fpt.wongun.trading_ai.service.analysis.TradeAnalysisContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AiSignalService {

    private final SymbolRepository symbolRepository;
    private final AiSignalRepository aiSignalRepository;
    private final MarketAnalysisService marketAnalysisService;
    private final AiClient aiClient;

    public AiSignalResponseDto generateSignal(AiSuggestRequestDto request) {
        Symbol symbol = symbolRepository.findByCode(request.getSymbolCode())
                .orElseThrow(() -> new EntityNotFoundException("Symbol not found: " + request.getSymbolCode()));

        TradeAnalysisContext context = marketAnalysisService.buildContext(symbol, request.getTimeframe());

        TradeSuggestion suggestion = aiClient.suggestTrade(context, request.getMode());

        // Only save to database if we have a valid trade signal (not NEUTRAL fallback)
        if (suggestion.getDirection() != Direction.NEUTRAL && suggestion.getEntryPrice() != null) {
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
                    .createdAt(Instant.now())
                    .createdBy("system") // sau này map user
                    .build();

            entity = aiSignalRepository.save(entity);
            return mapToDto(entity);
        }

        // For NEUTRAL fallback (AI error), return DTO without saving to DB
        return AiSignalResponseDto.builder()
                .id(null)
                .symbolCode(symbol.getCode())
                .timeframe(request.getTimeframe())
                .direction(suggestion.getDirection())
                .reasoning(suggestion.getReasoning())
                .createdAt(Instant.now())
                .build();
    }

    public Page<AiSignalResponseDto> getSignals(String symbolCode,
                                                String timeframe,
                                                Instant from,
                                                Instant to,
                                                int page,
                                                int size) {
        Symbol symbol = symbolRepository.findByCode(symbolCode)
                .orElseThrow(() -> new EntityNotFoundException("Symbol not found: " + symbolCode));

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
