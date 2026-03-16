package com.mall.service;

import com.mall.dto.chat.ChatRequest;
import com.mall.dto.chat.ChatResponse;
import com.mall.dto.chat.ChatStreamEvent;
import com.mall.model.AuthenticatedUser;
import com.mall.service.chat.flow.ChatFlowCoordinator;
import com.mall.service.chat.llm.ChatAiAnalysis;
import com.mall.service.chat.llm.ChatAiAssistant;
import com.mall.service.chat.llm.ChatSessionMemoryKey;
import com.mall.service.chat.llm.ChatSessionMemoryProvider;
import com.mall.service.chat.intent.ChatMessageParser;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ChatService {

    private final ChatMessageParser messageParser;
    private final ChatAiAssistant chatAiAssistant;
    private final ChatFlowCoordinator chatFlowCoordinator;
    private final ChatMessageHistoryService chatMessageHistoryService;
    private final ChatSessionMemoryProvider chatSessionMemoryProvider;
    private final ChatSessionContextService chatSessionContextService;

    public ChatService(
            ChatMessageParser messageParser,
            ChatAiAssistant chatAiAssistant,
            ChatFlowCoordinator chatFlowCoordinator,
            ChatMessageHistoryService chatMessageHistoryService,
            ChatSessionMemoryProvider chatSessionMemoryProvider,
            ChatSessionContextService chatSessionContextService
    ) {
        this.messageParser = messageParser;
        this.chatAiAssistant = chatAiAssistant;
        this.chatFlowCoordinator = chatFlowCoordinator;
        this.chatMessageHistoryService = chatMessageHistoryService;
        this.chatSessionMemoryProvider = chatSessionMemoryProvider;
        this.chatSessionContextService = chatSessionContextService;
    }

    public Mono<ChatResponse> chat(AuthenticatedUser currentUser, ChatRequest request) {
        String message = messageParser.normalizeMessage(request.message());
        String memoryId = ChatSessionMemoryKey.build(currentUser.id(), request.sessionId());
        return chatAiAssistant.analyze(memoryId, message)
                .defaultIfEmpty(new ChatAiAnalysis(null, "", "", ""))
                .flatMap(aiAnalysis -> chatFlowCoordinator.execute(currentUser, request.sessionId(), message, aiAnalysis))
                .flatMap(response -> persistTurn(currentUser, request.sessionId(), message, response))
            .doFinally(signalType -> chatSessionMemoryProvider.evict(memoryId));
    }

    public Flux<ChatStreamEvent> streamChat(AuthenticatedUser currentUser, ChatRequest request) {
        String message = messageParser.normalizeMessage(request.message());
        String memoryId = ChatSessionMemoryKey.build(currentUser.id(), request.sessionId());
        Mono<ChatAiAnalysis> aiAnalysisMono = chatAiAssistant.analyze(memoryId, message)
                .defaultIfEmpty(new ChatAiAnalysis(null, "", "", ""))
                .cache();

        Mono<ChatResponse> responseMono = aiAnalysisMono
                .flatMap(aiAnalysis -> chatFlowCoordinator.execute(currentUser, request.sessionId(), message, aiAnalysis))
                .flatMap(response -> persistTurn(currentUser, request.sessionId(), message, response))
            .doFinally(signalType -> chatSessionMemoryProvider.evict(memoryId))
                .cache();

        Flux<ChatStreamEvent> progressFlux = Flux.concat(
            Flux.just(ChatStreamEvent.stage("REQUEST_RECEIVED", "请求已接收，开始处理")),
                aiAnalysisMono.flatMapMany(aiAnalysis -> Flux.just(
                ChatStreamEvent.stage("ANALYZING", resolveAnalysisStatus(aiAnalysis)),
                ChatStreamEvent.stage("INTENT_RESOLVED", "意图已确认，准备执行业务操作"),
                ChatStreamEvent.stage("EXECUTING_ACTION", "正在调用后端业务能力"),
                ChatStreamEvent.stage("PERSISTING", "正在保存聊天上下文")
                ))
        );

        Flux<ChatStreamEvent> responseFlux = responseMono.flatMapMany(response -> Flux.concat(
            Flux.just(ChatStreamEvent.stage("RESPONSE_READY", "已生成回复")),
                Flux.just(ChatStreamEvent.complete(response))
        ));

        return Flux.concat(progressFlux, responseFlux)
                .onErrorResume(ex -> Flux.just(ChatStreamEvent.error(
                        ex.getMessage() == null || ex.getMessage().isBlank()
                                ? "处理请求时发生错误"
                                : ex.getMessage()
                )));
    }

    public Mono<Void> deleteSession(AuthenticatedUser currentUser, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Mono.empty();
        }
        String memoryId = ChatSessionMemoryKey.build(currentUser.id(), sessionId);
        return chatMessageHistoryService.clearSessionHistory(currentUser.id(), sessionId)
                .then(chatSessionContextService.clearSession(currentUser.id(), sessionId))
                .doFinally(signalType -> chatSessionMemoryProvider.evict(memoryId));
    }

    private Mono<ChatResponse> persistTurn(
            AuthenticatedUser currentUser,
            String sessionId,
            String userMessage,
            ChatResponse response
    ) {
        return chatMessageHistoryService.saveTurn(currentUser, sessionId, userMessage, response)
                .thenReturn(response);
    }

    private String resolveAnalysisStatus(ChatAiAnalysis aiAnalysis) {
        if (aiAnalysis == null || aiAnalysis.intent() == null || aiAnalysis.intent().isBlank()) {
            return "已完成语义分析";
        }
        return "识别到意图：" + aiAnalysis.intent();
    }

}
