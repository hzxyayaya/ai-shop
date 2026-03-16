package com.mall.service.chat.intent;

import com.mall.service.chat.llm.ChatAiAnalysis;
import org.springframework.stereotype.Component;

@Component
public class IntentCoordinator {

    private final RuleBasedChatIntentDetector ruleBasedChatIntentDetector;

    public IntentCoordinator(RuleBasedChatIntentDetector ruleBasedChatIntentDetector) {
        this.ruleBasedChatIntentDetector = ruleBasedChatIntentDetector;
    }

    public ResolvedIntentDecision resolve(String message, ChatAiAnalysis aiAnalysis) {
        ChatIntent ruleIntent = ruleBasedChatIntentDetector.detect(message);
        ChatIntent aiIntent = resolveAiIntent(aiAnalysis);

        if (aiIntent == null) {
            return new ResolvedIntentDecision(ruleIntent, IntentSource.RULE_FALLBACK, "llm unavailable or invalid", true);
        }

        if (isHighRisk(aiIntent) && aiIntent != ruleIntent) {
            return new ResolvedIntentDecision(ruleIntent, IntentSource.RULE_FALLBACK, "rule overrides high-risk intent", true);
        }

        return new ResolvedIntentDecision(aiIntent, IntentSource.LLM, "llm intent accepted", false);
    }

    private ChatIntent resolveAiIntent(ChatAiAnalysis aiAnalysis) {
        if (aiAnalysis == null || aiAnalysis.intent() == null || aiAnalysis.intent().isBlank()) {
            return null;
        }
        try {
            return ChatIntent.valueOf(aiAnalysis.intent());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isHighRisk(ChatIntent intent) {
        return intent == ChatIntent.ADD_TO_CART
                || intent == ChatIntent.BUY_NOW
                || intent == ChatIntent.PAY_GUIDE;
    }
}
