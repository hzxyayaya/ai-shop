package com.mall.service.chat.llm;

import com.mall.dto.chat.ChatResponse;
import com.mall.model.AuthenticatedUser;
import com.mall.service.chat.intent.ChatIntent;
import com.mall.service.chat.llm.ChatAiAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class ChatActionAssistant {

    private static final Logger log = LoggerFactory.getLogger(ChatActionAssistant.class);

    private final ObjectProvider<ChatActionAiService> chatActionAiServiceProvider;
    private final ChatActionExecutionContextRegistry contextRegistry;

    public ChatActionAssistant(
            ObjectProvider<ChatActionAiService> chatActionAiServiceProvider,
            ChatActionExecutionContextRegistry contextRegistry
    ) {
        this.chatActionAiServiceProvider = chatActionAiServiceProvider;
        this.contextRegistry = contextRegistry;
    }

    public Mono<ChatResponse> execute(
            AuthenticatedUser currentUser,
            String sessionId,
            String message,
            ChatIntent intent,
            ChatAiAnalysis aiAnalysis
    ) {
        ChatActionAiService aiService = chatActionAiServiceProvider.getIfAvailable();
        if (aiService == null) {
            return Mono.empty();
        }

        String memoryId = ChatSessionMemoryKey.build(currentUser.id(), sessionId);

                return Mono.fromCallable(() -> {
                    ChatActionExecutionContextRegistry.ChatActionExecutionState state =
                            contextRegistry.register(memoryId, currentUser, message, intent, aiAnalysis);
                    try {
                        aiService.execute(memoryId, intent.name(), message);
                        return state.response();
                    } finally {
                        contextRegistry.clear(memoryId);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(response -> response == null ? Mono.empty() : Mono.just(response))
                .onErrorResume(ex -> {
                    log.warn("LangChain4j action execution failed for {}: {}", intent, ex.getMessage());
                    return Mono.empty();
                });
    }
}
