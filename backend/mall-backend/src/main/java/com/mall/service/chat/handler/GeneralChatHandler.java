package com.mall.service.chat.handler;

import com.mall.dto.chat.ChatResponse;
import com.mall.model.AuthenticatedUser;
import com.mall.service.chat.intent.ChatIntent;
import com.mall.service.chat.llm.ChatAiAnalysis;
import com.mall.service.chat.memory.ChatMemoryService;
import com.mall.service.chat.support.ChatResponseFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class GeneralChatHandler {

    private final ChatMemoryService chatMemoryService;
    private final ChatResponseFactory chatResponseFactory;

    public GeneralChatHandler(ChatMemoryService chatMemoryService, ChatResponseFactory chatResponseFactory) {
        this.chatMemoryService = chatMemoryService;
        this.chatResponseFactory = chatResponseFactory;
    }

    public Mono<ChatResponse> handleGeneralQa(
            AuthenticatedUser currentUser,
            String sessionId,
            String message,
            ChatAiAnalysis aiAnalysis
    ) {
        return chatMemoryService.remember(
                currentUser,
                sessionId,
                message,
                ChatIntent.GENERAL_QA,
                chatResponseFactory.generalQa(aiAnalysis == null ? null : aiAnalysis.replyMessage()),
                java.util.List.of(),
                null
        );
    }
}
