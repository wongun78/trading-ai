package fpt.wongun.trading_ai.service.ai;

import fpt.wongun.trading_ai.service.analysis.TradeAnalysisContext;

public interface AiClient {

    TradeSuggestion suggestTrade(TradeAnalysisContext context, String mode);
}
