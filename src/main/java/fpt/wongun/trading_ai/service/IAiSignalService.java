package fpt.wongun.trading_ai.service;

import fpt.wongun.trading_ai.dto.AiSignalResponseDto;
import fpt.wongun.trading_ai.dto.AiSuggestRequestDto;
import org.springframework.data.domain.Page;

import java.time.Instant;

public interface IAiSignalService {

    AiSignalResponseDto generateSignal(AiSuggestRequestDto request);

    Page<AiSignalResponseDto> getSignals(
            String symbolCode,
            String timeframe,
            Instant from,
            Instant to,
            int page,
            int size
    );

    AiSignalResponseDto getSignalById(Long signalId);
}
