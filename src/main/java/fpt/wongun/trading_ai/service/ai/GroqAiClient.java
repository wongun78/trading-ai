package fpt.wongun.trading_ai.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fpt.wongun.trading_ai.config.GroqProperties;
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
 * Groq-powered AI client for generating trade suggestions.
 * Uses Llama 3.3 70B or other open-source models via Groq's fast inference API.
 * 
 * FREE API: https://console.groq.com
 * Models: llama-3.3-70b-versatile, llama-3.1-70b-versatile, mixtral-8x7b-32768
 * 
 * @Primary when groq.enabled=true, overriding OpenAI client
 * @ConditionalOnProperty enables this bean only when groq.enabled=true
 */
@Component
@Primary
@ConditionalOnProperty(prefix = "groq", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class GroqAiClient implements AiClient {

    private final GroqProperties groqProperties;
    private final ObjectMapper objectMapper;

    /**
     * Lazy-initialized WebClient for Groq API.
     * Created on first use to avoid initialization if Groq is disabled.
     */
    private WebClient groqWebClient;

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

    /**
     * Get or create WebClient for Groq API.
     */
    private WebClient getGroqWebClient() {
        if (groqWebClient == null) {
            groqWebClient = WebClient.builder()
                    .baseUrl(groqProperties.getBaseUrl())
                    .defaultHeader("Authorization", "Bearer " + groqProperties.getApiKey())
                    .defaultHeader("Content-Type", "application/json")
                    .build();
        }
        return groqWebClient;
    }

    @Override
    public TradeSuggestion suggestTrade(TradeAnalysisContext context, String mode) {
        try {
            log.info("Requesting trade suggestion from Groq (model: {}) for {}/{} in {} mode",
                    groqProperties.getModel(), context.getSymbolCode(), context.getTimeframe(), mode);

            // Trim candles based on mode (SCALPING=50, INTRADAY=100)
            TradeAnalysisContext trimmedContext = trimContextByMode(context, mode);

            // Serialize trimmed context to JSON
            String contextJson = objectMapper.writeValueAsString(trimmedContext);

            // Build user prompt with mode-specific instructions
            String userPrompt = buildUserPrompt(mode, contextJson);

            // Call Groq API
            String responseJson = callGroqApi(userPrompt);

            // Parse response into TradeSuggestion
            TradeSuggestion suggestion = parseTradeSuggestion(responseJson);

            // Apply Volman guards to validate the suggestion
            suggestion = enforceVolmanGuards(suggestion, mode);

            return suggestion;

        } catch (Exception e) {
            log.error("Error calling Groq API", e);
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

        int size = candles.size();
        if (size <= candleLimit) {
            return context; // Already within limit
        }

        int startIndex = size - candleLimit;

        return TradeAnalysisContext.builder()
                .symbolCode(context.getSymbolCode())
                .timeframe(context.getTimeframe())
                .higherTimeframeTrend(context.getHigherTimeframeTrend())
                .candles(new ArrayList<>(candles.subList(startIndex, size)))
                .ema21(ema21 != null && ema21.size() > candleLimit
                        ? new ArrayList<>(ema21.subList(startIndex, size))
                        : ema21)
                .ema25(ema25 != null && ema25.size() > candleLimit
                        ? new ArrayList<>(ema25.subList(startIndex, size))
                        : ema25)
                .build();
    }

    /**
     * Build the user prompt with trading mode instructions and market context.
     */
    private String buildUserPrompt(String mode, String contextJson) {
        return String.format("""
            MODE: %s
            
            Here is the market context as JSON (trimmed to recent candles):
            %s
            
            Bob Volman trading rules to apply:
            
            1) MANDATORY DETAILED ANALYSIS - You MUST analyze these elements:
            
            a) SWING STRUCTURE ANALYSIS:
               - Identify the last 3-5 swing highs and swing lows with EXACT PRICES
               - Determine if structure is HH/HL (uptrend), LH/LL (downtrend), or mixed
               - Calculate the distance between swings to assess trend strength
               - Example: "Last swings: H1=95420, L1=94850, H2=95680, L2=95100 → HH/HL confirmed uptrend"
            
            b) EMA INTERACTION:
               - Note EXACT current price vs EMA21 vs EMA25
               - Count how many candles since last EMA touch/rejection
               - Measure distance from current price to EMAs (in points and %)
               - Example: "Price at 95420, EMA21 at 95180 (240pts/0.25% above), last touched 3 candles ago with strong rejection wick"
            
            c) RECENT CANDLE PATTERNS:
               - Analyze the last 5-10 candles in detail
               - Identify pin bars, engulfing, inside bars with EXACT OHLC values
               - Measure wick sizes vs body sizes (ratio)
               - Example: "Candle #47: H=95420, L=94950, C=95380, O=95050 → 470pt bullish body with 40pt upper wick = strong momentum"
            
            d) TREND MOMENTUM:
               - Calculate average candle body size over last 10 candles
               - Count consecutive bullish/bearish candles
               - Identify acceleration or deceleration
               - Example: "Last 8 candles: 6 bullish, avg body 180pts, accelerating from 120pts → strong momentum"
            
            e) RISK ZONES:
               - Identify the EXACT most recent swing high/low for SL placement
               - Calculate distance from proposed entry to SL
               - Explain WHY this specific level (structure break, EMA, round number)
               - Example: "SL at 94850 (last swing low) = 570pts risk from entry 95420 = 0.60%% risk"
            
            2) TRADING MODE RULES:
            
            - SCALPING:
                • Use last ~50 candles for context
                • SL must be < 0.4%% of entry price
                • TP1 should be 1.2R-1.8R (scalp target)
                • Reject any setup with overlapping candles or unclear micro-structure
            
            - INTRADAY:
                • Use last ~100 candles for context
                • SL must be < 1.0%% of entry price
                • TP1 = 1.5R, TP2 = 2.5R, TP3 = 3.0R (intraday targets)
                • Accept slightly wider consolidation if macro trend is clear
            
            3) WHEN TO CHOOSE NEUTRAL:
            - Mixed swing structure (both HH and LL present)
            - Price extended >1%% from both EMAs with no pullback
            - Last 10 candles show overlapping ranges (choppy)
            - No clear rejection pattern or momentum
            - Any doubt → NEUTRAL with specific reason
            
            4) RESPONSE FORMAT (STRICT JSON):
            
            You MUST reply with ONLY valid JSON. The "reasoning" field MUST contain:
            - Line 1: Swing structure with exact prices
            - Line 2: EMA position and interaction
            - Line 3: Pattern identification
            - Line 4: Entry/SL/TP logic with calculations
            - Line 5: Risk assessment and final decision
            
            Schema:
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
              "reasoning": "DETAILED multi-line analysis with specific prices, measurements, and calculations as described above"
            }
            
            Example reasoning format:
            "Structure: HH/HL uptrend - Last swings H1=95680, L1=95100, H2=95420, L2=94850 confirming HH/HL. EMA: Price 95420 is 0.25%% above EMA21(95180), rejected 3 candles ago with 300pt wick. Pattern: Last 5 candles show bullish momentum - avg body 180pts, 4/5 green. Entry: 95400 on pullback to EMA21 with tight SL at 94850 (last swing low, 550pts = 0.58%% risk). TP1=96100 (1.27R), TP2=96650 (2.5R). Risk: Clean structure, strong rejection, tight SL = HIGH probability LONG."
            
            - No text before or after JSON.
            - No markdown code blocks.
            - Only pure JSON.
            """, mode, contextJson);
    }

    /**
     * Call Groq Chat Completions API.
     * Groq API is OpenAI-compatible, so we use the same endpoint structure.
     */
    private String callGroqApi(String userPrompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", groqProperties.getModel());
        requestBody.put("temperature", groqProperties.getTemperature());

        // Build messages array
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", userPrompt)
        );
        requestBody.put("messages", messages);

        // Call API
        Mono<Map> responseMono = getGroqWebClient().post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class);

        Map<String, Object> response = responseMono.block();

        if (response == null || !response.containsKey("choices")) {
            throw new RuntimeException("Invalid response from Groq API");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");

        if (choices.isEmpty()) {
            throw new RuntimeException("No choices in Groq response");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");

        String content = (String) message.get("content");

        log.debug("Groq response: {}", content);

        return content.trim();
    }

    /**
     * Parse Groq JSON response into TradeSuggestion object.
     */
    private TradeSuggestion parseTradeSuggestion(String responseJson) {
        try {
            // Clean up response (remove markdown code blocks if present)
            String cleanJson = responseJson
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

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
            log.error("Failed to parse Groq response as JSON: {}", responseJson, e);
            return createFallbackSuggestion("Failed to parse AI response: " + responseJson);
        }
    }

    /**
     * Helper to parse numeric values from Groq response.
     */
    private BigDecimal parseDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        return null;
    }

    /**
     * Create a fallback NEUTRAL suggestion when API call fails.
     */
    private TradeSuggestion createFallbackSuggestion(String errorMessage) {
        return TradeSuggestion.builder()
                .direction(Direction.NEUTRAL)
                .reasoning("Groq AI service unavailable: " + errorMessage)
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
