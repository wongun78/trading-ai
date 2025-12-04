package fpt.wongun.trading_ai.service;

import fpt.wongun.trading_ai.dto.AiSignalResponseDto;
import fpt.wongun.trading_ai.dto.AiSuggestRequestDto;
import org.springframework.data.domain.Page;

import java.time.Instant;

/**
 * Service interface for AI trading signal operations.
 * Generates and manages AI-powered trading signals.
 */
public interface IAiSignalService {

    /**
     * Generate AI trading signal for a symbol.
     * Uses AI models (Groq or OpenAI) to analyze market data
     * and generate trading recommendations.
     * 
     * @param request signal generation parameters (symbol, timeframe, mode)
     * @return generated signal with entry/exit points
     */
    AiSignalResponseDto generateSignal(AiSuggestRequestDto request);

    /**
     * Retrieve paginated signal history with filters.
     * 
     * @param symbolCode filter by symbol code
     * @param timeframe filter by timeframe
     * @param from filter by created date from (null = beginning)
     * @param to filter by created date to (null = now)
     * @param page page number (0-indexed)
     * @param size page size
     * @return paginated signals
     */
    Page<AiSignalResponseDto> getSignals(
            String symbolCode,
            String timeframe,
            Instant from,
            Instant to,
            int page,
            int size
    );

    /**
     * Get signal by ID.
     * 
     * @param signalId the signal ID
     * @return signal details
     */
    AiSignalResponseDto getSignalById(Long signalId);
}
