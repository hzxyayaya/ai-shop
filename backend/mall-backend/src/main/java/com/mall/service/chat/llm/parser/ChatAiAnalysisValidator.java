package com.mall.service.chat.llm.parser;

import com.mall.service.chat.intent.ChatIntent;
import com.mall.service.chat.llm.ChatAiAnalysis;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ChatAiAnalysisValidator {

    private final Set<String> allowedIntents = Arrays.stream(ChatIntent.values())
            .map(ChatIntent::code)
            .collect(Collectors.toSet());

    public ChatAiAnalysis normalize(ChatAiAnalysis analysis) {
        if (analysis == null) {
            return null;
        }
        String intent = normalizeIntent(analysis.intent());
        if (intent == null) {
            return null;
        }
        return new ChatAiAnalysis(
                intent,
                defaultString(analysis.searchQuery()),
                defaultString(analysis.replyMessage()),
                defaultString(analysis.recommendationReason())
        );
    }

    private String normalizeIntent(String intent) {
        if (!StringUtils.hasText(intent)) {
            return null;
        }
        String normalized = intent.trim().toUpperCase();
        return allowedIntents.contains(normalized) ? normalized : null;
    }

    private String defaultString(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
