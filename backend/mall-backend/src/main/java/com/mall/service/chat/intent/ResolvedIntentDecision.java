package com.mall.service.chat.intent;

public record ResolvedIntentDecision(
        ChatIntent intent,
        IntentSource source,
        String reason,
        boolean usedFallback
) {
}
