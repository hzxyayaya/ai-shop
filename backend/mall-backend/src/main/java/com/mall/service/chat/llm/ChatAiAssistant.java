package com.mall.service.chat.llm;

import com.mall.service.chat.llm.parser.ChatAiAnalysisValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class ChatAiAssistant {

    private static final Logger log = LoggerFactory.getLogger(ChatAiAssistant.class);

    private final ObjectProvider<ChatAnalysisAiService> chatAnalysisAiServiceProvider;
    private final ChatAiAnalysisValidator analysisValidator;

    public ChatAiAssistant(
            ObjectProvider<ChatAnalysisAiService> chatAnalysisAiServiceProvider,
            ChatAiAnalysisValidator analysisValidator
    ) {
        this.chatAnalysisAiServiceProvider = chatAnalysisAiServiceProvider;
        this.analysisValidator = analysisValidator;
    }

    public Mono<ChatAiAnalysis> analyze(String memoryId, String message) {
        ChatAnalysisAiService aiService = chatAnalysisAiServiceProvider.getIfAvailable();
        if (aiService == null) {
            return Mono.empty();
        }

        return Mono.fromCallable(() -> aiService.analyze(memoryId, message))
                .subscribeOn(Schedulers.boundedElastic())
                .map(analysisValidator::normalize)
                .flatMap(analysis -> analysis == null ? Mono.empty() : Mono.just(analysis))
                .onErrorResume(ex -> {
                    log.warn("LangChain4j analysis failed: {}", ex.getMessage());
                    return Mono.empty();
                });
    }
}
