package fpt.wongun.trading_ai.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fpt.wongun.trading_ai.config.OpenAiProperties;
import fpt.wongun.trading_ai.domain.enums.Direction;
import fpt.wongun.trading_ai.service.analysis.TradeAnalysisContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI-powered AI client for generating trade suggestions.
 * Uses GPT models to analyze market data and provide Bob Volman-style
 * price action trading recommendations.
 * 
 * @Primary annotation makes this the default AiClient implementation,
 * overriding MockAiClient for production use.
 */
@Component
@Primary
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
        You are an expert price action trader, trading in the style of Bob Volman.
        
        Your trading philosophy focuses on:
        - Pure price action analysis with minimal indicators
        - Market structure: Higher Highs/Higher Lows (HH/HL) for uptrends, Lower Highs/Lower Lows (LH/LL) for downtrends
        - EMA21 and EMA25 as dynamic support/resistance levels
        - Clean pullbacks in the direction of the dominant trend
        - Reading individual candle behavior: rejection wicks, inside bars, engulfing patterns
        - Supply and demand zones at key price levels
        - Never chasing extended moves or late breakouts
        - Being highly selective and conservative with trade setups
        
        You specialize in identifying high-probability setups where:
        1. The trend is clear and well-established
        2. Price pulls back to a logical support/resistance (often EMA21/25)
        3. There's clear rejection or confirmation via price action
        4. Risk/reward ratio is favorable (minimum 1:1.5 for scalping)
        
        When there's no clear setup, you have NO hesitation returning NEUTRAL.
        Quality over quantity is your mantra.
        """;

    @Override
    public TradeSuggestion suggestTrade(TradeAnalysisContext context, String mode) {
        try {
            log.info("Requesting trade suggestion from OpenAI for {}/{} in {} mode", 
                    context.getSymbolCode(), context.getTimeframe(), mode);

            // Serialize context to JSON
            String contextJson = objectMapper.writeValueAsString(context);

            // Build user prompt with mode-specific instructions
            String userPrompt = buildUserPrompt(mode, contextJson);

            // Call OpenAI API
            String responseJson = callOpenAiApi(userPrompt);

            // Parse response into TradeSuggestion
            return parseTradeSuggestion(responseJson);

        } catch (Exception e) {
            log.error("Error calling OpenAI API", e);
            return createFallbackSuggestion(e.getMessage());
        }
    }

    /**
     * Build the user prompt with trading mode instructions and market context.
     * 
     * Temperature can be adjusted in application.yml (openai.temperature).
     * Lower temperature (0.2-0.4) = more consistent, focused analysis.
     * Higher temperature (0.6-0.8) = more creative, varied suggestions.
     */
    private String buildUserPrompt(String mode, String contextJson) {
        String modeInstructions = mode.equalsIgnoreCase("SCALPING") 
            ? """
              MODE: SCALPING
              - Focus on tight, quick trades
              - Prefer tight stop losses (10-20 pips typically)
              - Target TP1 around 1:1.5 risk/reward minimum
              - TP2 optional at 1:2 to 1:2.5
              - Exit quickly if price action doesn't confirm immediately
              - Be extremely selective - only take A+ setups
              """
            : """
              MODE: INTRADAY
              - Can hold positions longer (minutes to hours)
              - Stop losses can be wider to account for normal volatility
              - Target TP1 at 1:2 risk/reward
              - TP2 at 1:3 to 1:4 if trend is strong
              - TP3 optional for runners at 1:5+
              - Still selective, but can give trade more room to breathe
              """;

        return String.format("""
            %s
            
            MARKET CONTEXT (JSON):
            %s
            
            ANALYSIS INSTRUCTIONS:
            1. Focus heavily on the last 30-50 candles for immediate price action context
            2. Check the higher timeframe trend (provided in context)
            3. Look for pullbacks to EMA21 or EMA25 in the trend direction
            4. Identify clear market structure (HH/HL for uptrend, LH/LL for downtrend)
            5. Look for confirmation: rejection wicks, pin bars, engulfing patterns
            6. Avoid choppy, sideways, or conflicting price action
            7. If no clear setup exists, return NEUTRAL without forcing a trade
            
            CRITICAL RULES:
            - DO NOT chase extended moves away from EMAs
            - DO NOT trade against the higher timeframe trend
            - DO NOT trade in messy, choppy conditions
            - When in doubt, return NEUTRAL
            
            RESPONSE FORMAT:
            Return ONLY a valid JSON object with this exact structure:
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
              "reasoning": "Brief explanation of why this trade setup is valid or why you're staying out (NEUTRAL)"
            }
            
            - If direction is NEUTRAL, set all price fields to null
            - If direction is LONG or SHORT, calculate realistic entry/stop/targets based on recent price action
            - riskReward values should be the ratio (e.g., 1.5 for 1:1.5R)
            - reasoning should be concise but mention key factors (trend, structure, EMA position, candle patterns)
            
            Return ONLY the JSON object, no markdown formatting, no code blocks, no extra text.
            """, modeInstructions, contextJson);
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
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        
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
            log.error("Failed to parse OpenAI response as JSON: {}", responseJson, e);
            return createFallbackSuggestion("Failed to parse AI response: " + responseJson);
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
}
