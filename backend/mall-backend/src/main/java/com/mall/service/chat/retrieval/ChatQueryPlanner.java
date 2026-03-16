package com.mall.service.chat.retrieval;

import com.mall.service.chat.intent.ChatMessageParser;
import com.mall.service.chat.llm.ChatAiAnalysis;
import org.springframework.stereotype.Component;

@Component
public class ChatQueryPlanner {

    private final ChatMessageParser messageParser;

    public ChatQueryPlanner(ChatMessageParser messageParser) {
        this.messageParser = messageParser;
    }

    public ProductQueryPlan plan(String message, ChatAiAnalysis aiAnalysis) {
        if (aiAnalysis != null && aiAnalysis.searchQuery() != null && !aiAnalysis.searchQuery().isBlank()) {
            return new ProductQueryPlan(aiAnalysis.searchQuery(), true);
        }
        return new ProductQueryPlan(messageParser.extractKeyword(message), false);
    }
}
