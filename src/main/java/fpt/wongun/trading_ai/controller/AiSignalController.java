package fpt.wongun.trading_ai.controller;

import fpt.wongun.trading_ai.dto.AiSignalResponseDto;
import fpt.wongun.trading_ai.dto.AiSuggestRequestDto;
import fpt.wongun.trading_ai.service.AiSignalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/signals")
@RequiredArgsConstructor
public class AiSignalController {

    private final AiSignalService aiSignalService;

    @PostMapping("/ai-suggest")
    public AiSignalResponseDto suggest(@Valid @RequestBody AiSuggestRequestDto requestDto) {
        return aiSignalService.generateSignal(requestDto);
    }

    @GetMapping
    public Page<AiSignalResponseDto> list(
            @RequestParam String symbolCode,
            @RequestParam String timeframe,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return aiSignalService.getSignals(symbolCode, timeframe, from, to, page, size);
    }
}
