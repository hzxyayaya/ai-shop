package com.mall.service.chat.llm;

public record ChatAiAnalysis(
        String intent,
        String searchQuery,
        String replyMessage,
        String recommendationReason
) {
}
