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

@Component
@Primary
@ConditionalOnProperty(prefix = "groq", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class GroqAiClient implements AiClient {

    private final GroqProperties groqProperties;
    private final ObjectMapper objectMapper;

    private WebClient groqWebClient;

    private static final String SYSTEM_PROMPT = """
        You are Bob Volman, world-renowned price action scalper and author of "Forex Price Action Scalping".
        
        YOUR TRADING PHILOSOPHY:
        - Price action is the ONLY truth. Patterns tell you what the market is doing RIGHT NOW.
        - You trade VERY selectively - only the cleanest, highest probability setups.
        - You use EMA21 and EMA25 ONLY as dynamic support/resistance zones, NOT as signals.
        - You demand CONTEXT: clear trend + clean pullback + strong rejection = tradeable setup.
        - When in doubt, you WAIT. Better to miss a trade than force a bad one.
        
        YOUR FAVORITE SETUPS (in order of preference):
        
        1. **RBR (Range Break Range)** - Your bread and butter
           - Market ranges briefly
           - Sharp break with momentum
           - Quick pullback to breakout level (now support/resistance)
           - Rejection candle or strong continuation
           
        2. **DBD/DBS (Double Bottom/Top During Trend)**
           - Clean uptrend with HH/HL structure
           - Price pulls back twice to same EMA level
           - Second touch shows rejection (long lower wick in uptrend)
           - Continuation candle confirms
           
        3. **CPB/CPD (Classic Pullback During trend)**
           - Strong trend with clear HH/HL or LH/LL
           - Price pulls back to EMA21/25 zone
           - Rejection candle shows buyers/sellers stepping in
           - No overlapping candles, no mess
           
        4. **BB (Bracket Break)**
           - Tight consolidation (3-5 candles)
           - Clean break with momentum
           - No immediate pullback = strength
           - Entry on first minor retrace
        
        YOUR RED FLAGS (NEVER TRADE):
        - Overlapping candles (indecision)
        - Extended moves (>20 candles from EMA without pullback)
        - Mixed swing structure (both HH and LL present)
        - Wicks on both sides (fighting, no clear winner)
        - First pushes after long consolidation (wait for pullback)
        - News time or low liquidity periods
        
        YOUR ANALYSIS STYLE:
        - You describe EXACTLY what you see, candle by candle
        - You identify the specific pattern name (RBR, DBD, CPB, BB)
        - You explain WHY this setup has edge
        - You point out what could go wrong
        - You NEVER force a trade - if it's not textbook, it's NEUTRAL
        """;

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

            TradeAnalysisContext trimmedContext = trimContextByMode(context, mode);

            String contextJson = objectMapper.writeValueAsString(trimmedContext);

            String userPrompt = buildUserPrompt(mode, contextJson);

            String responseJson = callGroqApi(userPrompt);

            TradeSuggestion suggestion = parseTradeSuggestion(responseJson);

            suggestion = enforceVolmanGuards(suggestion, mode);

            return suggestion;

        } catch (Exception e) {
            log.error("Error calling Groq API", e);
            return createFallbackSuggestion(e.getMessage());
        }
    }

    private TradeAnalysisContext trimContextByMode(TradeAnalysisContext context, String mode) {
        int candleLimit = mode.equalsIgnoreCase("SCALPING") ? 50 : 100;

        List<TradeAnalysisContext.CandlePoint> candles = context.getCandles();
        List<BigDecimal> ema21 = context.getEma21();
        List<BigDecimal> ema25 = context.getEma25();

        int candleSize = candles.size();
        if (candleSize <= candleLimit) {
            return context; 
        }

        int candleStartIndex = candleSize - candleLimit;
        
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

    private String buildUserPrompt(String mode, String contextJson) {
        return """
            MODE: %s
            
            Market data (recent candles trimmed based on mode):
            %s
            
            ═══════════════════════════════════════════════════════════════
            BOB VOLMAN ANALYSIS FRAMEWORK - STEP BY STEP
            ═══════════════════════════════════════════════════════════════
            
            STEP 1: READ THE RECENT PRICE ACTION (Last 10-15 candles)
            ─────────────────────────────────────────────────────────────
            Describe what you SEE, Bob Volman style:
            
            - "I see 8 consecutive bullish candles climbing from 95100 to 95680"
            - "Price made a sharp push, then pulled back in 3 clean candles to 95350"
            - "There's a tight 5-candle range between 95400-95450"
            - "Notice the long lower wick on candle #48 at EMA21 - buyers stepped in hard"
            
            Be SPECIFIC with candle numbers, prices, and what happened.

            STEP 2: IDENTIFY THE SWING STRUCTURE
            ─────────────────────────────────────────────────────────────
            Mark the last 3-4 swing highs and lows with EXACT prices:
            
            Example uptrend:
            "Swing structure: H1=95200 → L1=94850 → H2=95420 → L2=95100 → H3=95680"
            "This is HH/HL structure = confirmed uptrend"
            
            Example downtrend:
            "Swing structure: L1=95800 → H1=96050 → L2=95600 → H2=95850 → L3=95400"
            "This is LH/LL structure = confirmed downtrend"
            
            If structure is MIXED (both HH and LL), say so and lean toward NEUTRAL.

            STEP 3: CHECK EMA INTERACTION
            ─────────────────────────────────────────────────────────────
            Answer these questions:
            
            1. Where is price NOW vs EMA21 vs EMA25?
               Example: "Price at 95420, EMA21 at 95180 (+240pts/+0.25%%), EMA25 at 95050"
            
            2. Did price recently touch or cross an EMA?
               Example: "3 candles ago, price pulled back to EMA21 and rejected with 180pt lower wick"
            
            3. Is price extended or near EMA?
               - Extended = >0.5%% away from both EMAs → WAIT for pullback
               - Near = within 0.2%% → Look for entry pattern

            STEP 4: PATTERN RECOGNITION (Bob Volman Setups)
            ─────────────────────────────────────────────────────────────
            Identify if you see one of these CLASSIC setups:
            
            ✓ RBR (Range Break Range):
              - Tight range → sharp break → pullback to breakout level → rejection
            
            ✓ DBD/DBS (Double Bottom/Top During trend):
              - Trend → pullback to EMA → bounce → pullback again to SAME level → stronger rejection
            
            ✓ CPB/CPD (Classic Pullback During trend):
              - Clear trend → single clean pullback to EMA21/25 → rejection candle
            
            ✓ BB (Bracket Break):
              - Very tight consolidation (3-5 candles) → clean break with momentum
            
            ✗ If NONE of these are present, say "No clear Volman pattern" → NEUTRAL

            STEP 5: ENTRY TRIGGER & RISK PLACEMENT
            ─────────────────────────────────────────────────────────────
            If pattern exists:
            
            Entry:
            - Where exactly would Bob enter? (usually just after rejection candle close)
            - Example: "Entry at 95420 (close of rejection candle)"
            
            Stop Loss:
            - Where is the NEAREST swing low/high that invalidates the setup?
            - Example: "SL at 94850 (last swing low, just below EMA25)"
            - Calculate: "Risk = 570pts = 0.60%% of entry"
            
            Take Profit:
            - SCALPING: 1.2R-1.8R (quick targets, tight R:R)
            - INTRADAY: 1.5R-3.0R (swing targets, wider R:R)
            - Example: "TP1 at 96100 (+680pts = 1.19R), TP2 at 96550 (+1130pts = 1.98R)"

            STEP 6: FINAL DECISION
            ─────────────────────────────────────────────────────────────
            Ask yourself (Bob Volman style):
            
            1. "Is this a TEXTBOOK setup I've traded 1000 times before?"
               → If NO = NEUTRAL
            
            2. "Am I forcing this trade because I want action?"
               → If YES = NEUTRAL
            
            3. "Can I explain this setup in 2 sentences to a beginner?"
               → If NO = NEUTRAL
            
            4. "What's my confidence level: 80%%+ or below?"
               → If below 80%% = NEUTRAL
            
            If all checks pass → LONG or SHORT
            If ANY doubt → NEUTRAL with specific reason

            ═══════════════════════════════════════════════════════════════
            MODE-SPECIFIC RULES
            ═══════════════════════════════════════════════════════════════
            
            SCALPING MODE:
            • Context: last 50 candles
            • SL must be <0.4%% of entry (very tight)
            • TP1 target: 1.2R-1.8R (quick exit)
            • Reject ANY overlapping candles or unclear micro-structure
            • Example: "Tight DBD at EMA21, SL 20pts below, TP 25pts above = 1.25R"
            
            INTRADAY MODE:
            • Context: last 100 candles
            • SL must be <1.0%% of entry (reasonable swing room)
            • TP1/TP2/TP3 targets: 1.5R / 2.5R / 3.0R
            • Allow slightly messier consolidation if macro trend is pristine
            • Example: "CPB after strong rally, SL at last swing, TP at structure resistance"

            ═══════════════════════════════════════════════════════════════
            RESPONSE FORMAT (STRICT JSON)
            ═══════════════════════════════════════════════════════════════
            
            Your "reasoning" field MUST follow this structure:
            
            Line 1: Recent Price Action - describe last 10-15 candles
            Line 2: Swing Structure - identify HH/HL or LH/LL with exact prices
            Line 3: EMA Position - current price vs EMAs with measurements
            Line 4: Pattern ID - name the Volman setup (RBR/DBD/CPB/BB) or "No pattern"
            Line 5: Entry Logic - where to enter, SL placement, TP targets with calculations
            Line 6: Confidence - final decision with conviction level
            
            JSON Schema:
            {
              "direction": "LONG" | "SHORT" | "NEUTRAL",
              "entryPrice": number or null,
              "stopLoss": number or null,
              "takeProfit": number or null,
              "riskReward": number or null,
              "reasoning": "Multi-line Bob Volman style analysis as described above"
            }
            
            Example reasoning (LONG signal):
            "Recent Action: Price rallied from 95100 to 95680 in 8 strong bullish candles, then pulled back cleanly to 95350 in 3 candles without overlap.
            Structure: HH/HL confirmed - H1=95200, L1=94850, H2=95420, L2=95100, H3=95680, L3=95350. Clear uptrend intact.
            EMA: Price pulled back to EMA21(95320), now sitting +30pts above. Previous touch 5 candles ago showed 180pt rejection wick.
            Pattern: Classic DBD (Double Bottom During trend) - second touch of EMA21 with stronger rejection than first. Textbook Bob Volman setup.
            Entry: 95400 on close above rejection candle. SL: 94850 (last major swing low) = 550pts = 0.58%% risk. TP: 96060 (1.2R).
            Confidence: 85%% - Clean trend, textbook pattern, tight SL. Strong LONG."
            
            Example reasoning (NEUTRAL):
            "Recent Action: Last 12 candles show overlapping ranges between 95300-95500, no clear direction.
            Structure: Mixed - H1=95480, L1=95280, H2=95520, L2=95350 - neither clear HH/HL nor LH/LL.
            EMA: Price chopping around both EMAs, multiple crosses with no sustained move.
            Pattern: No Volman setup - this is bracketing/consolidation, not a pullback during trend.
            Entry: N/A - no clean entry exists in this mess.
            Confidence: 0%% - Bob would never trade this. Wait for clean break and pullback. NEUTRAL."
            
            - NO text before or after JSON
            - NO markdown code blocks
            - ONLY pure JSON
            """.formatted(mode, contextJson);
    }

    private String callGroqApi(String userPrompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", groqProperties.getModel());
        requestBody.put("temperature", groqProperties.getTemperature());

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", userPrompt)
        );
        requestBody.put("messages", messages);

        int maxRetries = 3;
        int retryDelayMs = 1000;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.debug("Groq API call attempt {}/{}", attempt, maxRetries);
                
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
                Map<String, Object> message = (Map<String, Object>) choices.getFirst().get("message");

                String content = (String) message.get("content");

                log.info("Groq API call successful on attempt {}", attempt);
                log.debug("Groq response: {}", content);

                return content.trim();
                
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                boolean isRateLimitError = errorMsg != null && (
                    errorMsg.contains("429") || 
                    errorMsg.contains("Too Many Requests") ||
                    errorMsg.contains("rate limit")
                );
                
                boolean isTransientError = errorMsg != null && (
                    errorMsg.contains("503") ||
                    errorMsg.contains("502") ||
                    errorMsg.contains("timeout")
                );
                
                if ((isRateLimitError || isTransientError) && attempt < maxRetries) {
                    log.warn("Groq API error (attempt {}/{}): {}. Retrying in {}ms...", 
                            attempt, maxRetries, errorMsg, retryDelayMs);
                    
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                    
                    retryDelayMs *= 2;
                    continue;
                }
                
                log.error("Groq API call failed after {} attempts: {}", attempt, errorMsg);
                throw new RuntimeException("Groq API unavailable: " + errorMsg, e);
            }
        }
        
        throw new RuntimeException("Groq API failed after " + maxRetries + " retries");
    }

    private TradeSuggestion parseTradeSuggestion(String responseJson) {
        try {
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
                    .takeProfit(parseDecimal(responseMap.get("takeProfit")))
                    .riskReward(parseDecimal(responseMap.get("riskReward")))
                    .reasoning((String) responseMap.get("reasoning"))
                    .build();

        } catch (JsonProcessingException e) {
            log.error("Failed to parse Groq response as JSON. Original: {}", responseJson, e);
            
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
                        .takeProfit(parseDecimal(responseMap.get("takeProfit")))
                        .riskReward(parseDecimal(responseMap.get("riskReward")))
                        .reasoning((String) responseMap.get("reasoning"))
                        .build();
                        
            } catch (Exception ex) {
                log.error("Aggressive cleaning also failed", ex);
                return createFallbackSuggestion("⚠️ AI response format error. Please try again.");
            }
        }
    }

    private BigDecimal parseDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        return null;
    }

    private TradeSuggestion createFallbackSuggestion(String errorMessage) {
        String userFriendlyMessage;
        
        if (errorMessage.contains("429") || errorMessage.contains("Too Many Requests") || errorMessage.contains("rate limit")) {
            userFriendlyMessage = "⏱️ Groq API rate limit reached. Too many requests in short time. Please wait 30-60 seconds and try again.";
        } else if (errorMessage.contains("401") || errorMessage.contains("Unauthorized")) {
            userFriendlyMessage = "🔑 Groq API key invalid or missing. Please check GROQ_API_KEY in .env file.";
        } else if (errorMessage.contains("503") || errorMessage.contains("502")) {
            userFriendlyMessage = "🔧 Groq service temporarily unavailable. Their servers may be under maintenance. Try again in a few minutes.";
        } else if (errorMessage.contains("timeout")) {
            userFriendlyMessage = "⏰ Request timeout. Groq API took too long to respond. Try again.";
        } else if (errorMessage.contains("network") || errorMessage.contains("connection")) {
            userFriendlyMessage = "🌐 Network connection error. Check your internet connection and try again.";
        } else {
            userFriendlyMessage = "❌ AI analysis unavailable: " + errorMessage;
        }
        
        return TradeSuggestion.builder()
                .direction(Direction.NEUTRAL)
                .reasoning(userFriendlyMessage)
                .build();
    }

    private TradeSuggestion enforceVolmanGuards(TradeSuggestion s, String mode) {
        if (s == null) {
            return neutral("Invalid AI response");
        }

        if (s.getDirection() == Direction.NEUTRAL) {
            return s;
        }

        if (s.getEntryPrice() == null || s.getStopLoss() == null) {
            return neutral("Missing entry/SL — rejected by Volman guard");
        }

        BigDecimal entry = s.getEntryPrice();
        BigDecimal sl = s.getStopLoss();
        BigDecimal distancePct = sl.subtract(entry).abs()
                .divide(entry, MathContext.DECIMAL64)
                .multiply(BigDecimal.valueOf(100));

        if (mode.equals("SCALPING") && distancePct.compareTo(BigDecimal.valueOf(0.4)) > 0) {
            return neutral("SL too wide for scalping — rejected by Volman guard");
        }

        if (mode.equals("INTRADAY") && distancePct.compareTo(BigDecimal.valueOf(1.0)) > 0) {
            return neutral("SL too wide for intraday — rejected by Volman guard");
        }

        if (s.getRiskReward() != null) {
            if (s.getRiskReward().compareTo(BigDecimal.valueOf(1.0)) < 0 ||
                    s.getRiskReward().compareTo(BigDecimal.valueOf(4.0)) > 0) {
                return neutral("RR out of range — rejected by Volman guard");
            }
        }

        return s; 
    }

    private TradeSuggestion neutral(String reason) {
        return TradeSuggestion.builder()
                .direction(Direction.NEUTRAL)
                .entryPrice(null)
                .stopLoss(null)
                .takeProfit(null)
                .riskReward(null)
                .reasoning(reason)
                .build();
    }
}
