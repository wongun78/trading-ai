package fpt.wongun.trading_ai.service;

import fpt.wongun.trading_ai.domain.enums.PositionStatus;
import fpt.wongun.trading_ai.dto.*;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IPositionService {

    PositionResponseDto openPosition(OpenPositionRequestDto request);

    PositionResponseDto executePosition(Long positionId, ExecutePositionRequestDto request);

    PositionResponseDto closePosition(Long positionId, ClosePositionRequestDto request);

    PositionResponseDto cancelPosition(Long positionId);

    PositionResponseDto getPosition(Long positionId);

    Page<PositionResponseDto> getPositions(
            String symbolCode,
            PositionStatus status,
            int page,
            int size
    );

    List<PositionResponseDto> getOpenPositions(String userId);

    PortfolioStatsDto getPortfolioStats(String userId);
}
