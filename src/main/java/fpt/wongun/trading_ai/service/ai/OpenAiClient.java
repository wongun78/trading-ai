package fpt.wongun.trading_ai.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fpt.wongun.trading_ai.config.OpenAiProperties;
import fpt.wongun.trading_ai.domain.enums.Direction;
import fpt.wongun.trading_ai.service.analysis.TradeAnalysisContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI-powered AI client for generating trade suggestions.
 * Uses GPT models to analyze market data and provide Bob Volman-style
 * price action trading recommendations.
 * 
 * Only active when groq.enabled=false (fallback to OpenAI).
 * @Primary when active to override MockAiClient.
 */
@Component
@Primary
@ConditionalOnProperty(prefix = "groq", name = "enabled", havingValue = "false", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class OpenAiClient implements AiClient {

    private final OpenAiProperties openAiProperties;
    private final WebClient openAiWebClient;
    private final ObjectMapper objectMapper;

    /**
     * System prompt defining the AI's role and trading philosophy.
     * Based on Bob Volman's price action scalping methodology.
     */
    private static final String SYSTEM_PROMPT = """
        You are an expert intraday price action trader, trading strictly in the style of Bob Volman.
        
        Your core rules:
        - Trade ONLY clean price action.
        - Do NOT use indicators other than EMA21 and EMA25 (dynamic support/resistance).
        - Always evaluate HH/HL or LH/LL swing structure to determine trend quality.
        - Prefer clean pullback entries into the EMA21/EMA25 area, with rejection wicks or strong reaction.
        - Avoid trading after extended moves. Avoid choppy, overlapping candles. Avoid weak trends.
        - If context is unclear, messy, or risky, you must choose NEUTRAL (no trade).
        - You will receive structured market data (candles, EMA21, EMA25, trend) and a trading MODE (SCALPING or INTRADAY).
        - Your job is to output ONE high-probability trade idea or return NEUTRAL if nothing meets the criteria.
        """;

    @Override
    public TradeSuggestion suggestTrade(TradeAnalysisContext context, String mode) {
        try {
            log.info("Requesting trade suggestion from OpenAI for {}/{} in {} mode", 
                    context.getSymbolCode(), context.getTimeframe(), mode);

            // Trim candles based on mode (SCALPING=50, INTRADAY=100)
            TradeAnalysisContext trimmedContext = trimContextByMode(context, mode);

            // Serialize trimmed context to JSON
            String contextJson = objectMapper.writeValueAsString(trimmedContext);

            // Build user prompt with mode-specific instructions
            String userPrompt = buildUserPrompt(mode, contextJson);

            // Call OpenAI API
            String responseJson = callOpenAiApi(userPrompt);

            // Parse response into TradeSuggestion
            TradeSuggestion suggestion = parseTradeSuggestion(responseJson);

            // Apply Volman guards to validate the suggestion
            suggestion = enforceVolmanGuards(suggestion, mode);

            return suggestion;

        } catch (Exception e) {
            log.error("Error calling OpenAI API", e);
            return createFallbackSuggestion(e.getMessage());
        }
    }

    /**
     * Trim context to last N candles based on trading mode.
     * SCALPING: last 50 candles
     * INTRADAY: last 100 candles
     */
    private TradeAnalysisContext trimContextByMode(TradeAnalysisContext context, String mode) {
        int candleLimit = mode.equalsIgnoreCase("SCALPING") ? 50 : 100;

        List<TradeAnalysisContext.CandlePoint> candles = context.getCandles();
        List<BigDecimal> ema21 = context.getEma21();
        List<BigDecimal> ema25 = context.getEma25();

        int candleSize = candles.size();
        if (candleSize <= candleLimit) {
            return context; // Already within limit
        }

        int candleStartIndex = candleSize - candleLimit;
        
        // Calculate start index for EMAs (they may be shorter than candles)
        int ema21StartIndex = ema21 != null && ema21.size() > candleLimit 
                ? ema21.size() - candleLimit 
                : 0;
        int ema25StartIndex = ema25 != null && ema25.size() > candleLimit 
                ? ema25.size() - candleLimit 
                : 0;

        return TradeAnalysisContext.builder()
                .symbolCode(context.getSymbolCode())
                .timeframe(context.getTimeframe())
                .higherTimeframeTrend(context.getHigherTimeframeTrend())
                .candles(new ArrayList<>(candles.subList(candleStartIndex, candleSize)))
                .ema21(ema21 != null && ema21.size() > candleLimit 
                        ? new ArrayList<>(ema21.subList(ema21StartIndex, ema21.size())) 
                        : ema21)
                .ema25(ema25 != null && ema25.size() > candleLimit 
                        ? new ArrayList<>(ema25.subList(ema25StartIndex, ema25.size())) 
                        : ema25)
                .build();
    }

    /**
     * Build the user prompt with trading mode instructions and market context.
     */
    private String buildUserPrompt(String mode, String contextJson) {
        return """
            MODE: %s
            
            Here is the market context as JSON (trimmed to recent candles):
            %s
            
            Bob Volman trading rules to apply:
            
            1) Trend & Structure
            - Use higherTimeframeTrend ("UP", "DOWN", "SIDEWAYS") only as reference.
            - Confirm trend by checking HH/HL or LH/LL sequences.
            - Longs only in clean HH/HL uptrend pullbacks.
            - Shorts only in clean LH/LL downtrend pullbacks.
            - SIDEWAYS → be extremely conservative, usually NEUTRAL.
            
            2) EMA21 & EMA25 Usage
            - EMAs act like dynamic S/R.
            - A valid entry requires a pullback into EMA21/25 followed by a strong rejection candle or momentum push-away.
            
            3) MODE Behavior
            - SCALPING:
                • Use last ~50 candles
                • Tight stop-loss
                • TP1 ≈ 1.2R–1.8R
                • Reject chop immediately
            - INTRADAY:
                • Use last ~100 candles
                • TP1 ≈ 1.5R, TP2 ≈ 2.5R–3.0R
                • Avoid extreme or unrealistic R:R
            
            4) When to choose NEUTRAL
            - Trend is unclear or mixed HH/LL
            - No clean EMA reaction
            - Last price swing too extended
            - Overlapping / indecision candles
            - Market feels "messy" or unsafe
            - Any doubt → NEUTRAL
            
            5) RESPONSE FORMAT (STRICT)
            Reply with ONLY valid JSON matching EXACTLY this schema:
            
            {
              "direction": "LONG" | "SHORT" | "NEUTRAL",
              "entryPrice": number or null,
              "stopLoss": number or null,
              "takeProfit1": number or null,
              "takeProfit2": number or null,
              "takeProfit3": number or null,
              "riskReward1": number or null,
              "riskReward2": number or null,
              "riskReward3": number or null,
              "reasoning": "1-3 sentence explanation"
            }
            
            - No text before or after.
            - No comments.
            - No markdown.
            - Only JSON.
            """.formatted(mode, contextJson);
    }

    /**
     * Call OpenAI Chat Completions API.
     * 
     * Prompt engineering tips:
     * - Adjust temperature in application.yml for creativity vs consistency
     * - System prompt defines the "personality" and expertise
     * - User prompt provides specific task and context
     * - Request JSON-only output to simplify parsing
     */
    private String callOpenAiApi(String userPrompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", openAiProperties.getModel());
        requestBody.put("temperature", openAiProperties.getTemperature());
        
        // Build messages array
        List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", SYSTEM_PROMPT),
            Map.of("role", "user", "content", userPrompt)
        );
        requestBody.put("messages", messages);

        // Call API
        Mono<Map> responseMono = openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class);

        Map<String, Object> response = responseMono.block();

        if (response == null || !response.containsKey("choices")) {
            throw new RuntimeException("Invalid response from OpenAI API");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        
        if (choices.isEmpty()) {
            throw new RuntimeException("No choices in OpenAI response");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.getFirst().get("message");
        
        String content = (String) message.get("content");
        
        log.debug("OpenAI response: {}", content);
        
        return content.trim();
    }

    /**
     * Parse OpenAI JSON response into TradeSuggestion object.
     * 
     * This is where the JSON string from OpenAI gets converted to our domain model.
     * If parsing fails, we log the error and return a fallback NEUTRAL suggestion.
     */
    private TradeSuggestion parseTradeSuggestion(String responseJson) {
        try {
            // Clean up response (remove markdown code blocks if present)
            String cleanJson = responseJson
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            log.debug("Cleaned JSON for parsing: {}", cleanJson);

            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(cleanJson, Map.class);

            return TradeSuggestion.builder()
                    .direction(Direction.valueOf((String) responseMap.get("direction")))
                    .entryPrice(parseDecimal(responseMap.get("entryPrice")))
                    .stopLoss(parseDecimal(responseMap.get("stopLoss")))
                    .takeProfit1(parseDecimal(responseMap.get("takeProfit1")))
                    .takeProfit2(parseDecimal(responseMap.get("takeProfit2")))
                    .takeProfit3(parseDecimal(responseMap.get("takeProfit3")))
                    .riskReward1(parseDecimal(responseMap.get("riskReward1")))
                    .riskReward2(parseDecimal(responseMap.get("riskReward2")))
                    .riskReward3(parseDecimal(responseMap.get("riskReward3")))
                    .reasoning((String) responseMap.get("reasoning"))
                    .build();

        } catch (JsonProcessingException e) {
            log.error("Failed to parse OpenAI response as JSON. Original: {}", responseJson, e);
            
            // Try to clean and re-parse one more time with more aggressive cleaning
            try {
                String aggressiveClean = responseJson
                        .replaceAll("(?s)```json\\s*", "")
                        .replaceAll("(?s)```\\s*", "")
                        .replaceAll("\\n", " ")
                        .replaceAll("\\s+", " ")
                        .trim();
                
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(aggressiveClean, Map.class);
                
                log.info("Successfully parsed with aggressive cleaning");
                
                return TradeSuggestion.builder()
                        .direction(Direction.valueOf((String) responseMap.get("direction")))
                        .entryPrice(parseDecimal(responseMap.get("entryPrice")))
                        .stopLoss(parseDecimal(responseMap.get("stopLoss")))
                        .takeProfit1(parseDecimal(responseMap.get("takeProfit1")))
                        .takeProfit2(parseDecimal(responseMap.get("takeProfit2")))
                        .takeProfit3(parseDecimal(responseMap.get("takeProfit3")))
                        .riskReward1(parseDecimal(responseMap.get("riskReward1")))
                        .riskReward2(parseDecimal(responseMap.get("riskReward2")))
                        .riskReward3(parseDecimal(responseMap.get("riskReward3")))
                        .reasoning((String) responseMap.get("reasoning"))
                        .build();
                        
            } catch (Exception ex) {
                log.error("Aggressive cleaning also failed", ex);
                return createFallbackSuggestion("⚠️ AI response format error. Please try again.");
            }
        }
    }

    /**
     * Helper to parse numeric values from OpenAI response.
     */
    private java.math.BigDecimal parseDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return new java.math.BigDecimal(value.toString());
        }
        return null;
    }

    /**
     * Create a fallback NEUTRAL suggestion when API call fails.
     */
    private TradeSuggestion createFallbackSuggestion(String errorMessage) {
        return TradeSuggestion.builder()
                .direction(Direction.NEUTRAL)
                .reasoning("AI service unavailable: " + errorMessage)
                .build();
    }

    /**
     * Enforce Bob Volman trading guards on AI-generated suggestions.
     * Validates stop-loss distance and risk/reward ratios.
     * Returns NEUTRAL if validation fails.
     */
    private TradeSuggestion enforceVolmanGuards(TradeSuggestion s, String mode) {
        if (s == null) {
            return neutral("Invalid AI response");
        }

        if (s.getDirection() == Direction.NEUTRAL) {
            return s; // Already neutral, no validation needed
        }

        if (s.getEntryPrice() == null || s.getStopLoss() == null) {
            return neutral("Missing entry/SL — rejected by Volman guard");
        }

        BigDecimal entry = s.getEntryPrice();
        BigDecimal sl = s.getStopLoss();
        BigDecimal distancePct = sl.subtract(entry).abs()
                .divide(entry, MathContext.DECIMAL64)
                .multiply(BigDecimal.valueOf(100));

        // SL distance rules
        if (mode.equals("SCALPING") && distancePct.compareTo(BigDecimal.valueOf(0.4)) > 0) {
            return neutral("SL too wide for scalping — rejected by Volman guard");
        }

        if (mode.equals("INTRADAY") && distancePct.compareTo(BigDecimal.valueOf(1.0)) > 0) {
            return neutral("SL too wide for intraday — rejected by Volman guard");
        }

        // RR sanity rule
        if (s.getRiskReward1() != null) {
            if (s.getRiskReward1().compareTo(BigDecimal.valueOf(1.0)) < 0 ||
                s.getRiskReward1().compareTo(BigDecimal.valueOf(4.0)) > 0) {
                return neutral("RR1 out of range — rejected by Volman guard");
            }
        }

        return s; // Passed validation
    }

    /**
     * Helper method to create NEUTRAL suggestion with reasoning.
     */
    private TradeSuggestion neutral(String reason) {
        return TradeSuggestion.builder()
                .direction(Direction.NEUTRAL)
                .entryPrice(null)
                .stopLoss(null)
                .takeProfit1(null)
                .takeProfit2(null)
                .takeProfit3(null)
                .riskReward1(null)
                .riskReward2(null)
                .riskReward3(null)
                .reasoning(reason)
                .build();
    }
}
