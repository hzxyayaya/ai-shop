package com.mall.service;

import com.mall.dto.chat.ChatResponse;
import com.mall.entity.chat.ChatMessageHistoryEntity;
import com.mall.model.AuthenticatedUser;
import com.mall.repository.chat.ChatMessageHistoryRepository;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ChatMessageHistoryService {

    private static final int DEFAULT_MEMORY_WINDOW = 20;
    private static final String ROLE_USER = "USER";
    private static final String ROLE_AI = "AI";

    private final ChatMessageHistoryRepository repository;

    public ChatMessageHistoryService(ChatMessageHistoryRepository repository) {
        this.repository = repository;
    }

    public Mono<Void> saveTurn(
            AuthenticatedUser currentUser,
            String sessionId,
            String userMessage,
            ChatResponse response
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        ChatMessageHistoryEntity userEntry = new ChatMessageHistoryEntity(
                null,
                currentUser.id(),
                sessionId,
                ROLE_USER,
                defaultString(userMessage),
                now
        );
        ChatMessageHistoryEntity aiEntry = new ChatMessageHistoryEntity(
                null,
                currentUser.id(),
                sessionId,
                ROLE_AI,
                response == null ? "" : defaultString(response.message()),
                now.plusNanos(1)
        );
        return repository.save(userEntry)
                .then(repository.save(aiEntry))
                .then();
    }

    public List<ChatMessage> loadRecentMessagesBlocking(Long userId, String sessionId) {
        return repository.findTop20ByUserIdAndSessionIdOrderByCreatedAtDesc(userId, sessionId)
                .collectList()
                .map(list -> list.stream()
                        .sorted(Comparator.comparing(ChatMessageHistoryEntity::getCreatedAt))
                        .map(this::toChatMessage)
                        .toList())
                .defaultIfEmpty(List.of())
                .blockOptional()
                .orElse(List.of());
    }

    public Mono<Void> clearSessionHistory(Long userId, String sessionId) {
        return repository.deleteBySessionIdAndUserId(sessionId, userId);
    }

    private ChatMessage toChatMessage(ChatMessageHistoryEntity entity) {
        if (ROLE_AI.equalsIgnoreCase(entity.getMessageRole())) {
            return AiMessage.from(defaultString(entity.getContent()));
        }
        return UserMessage.from(defaultString(entity.getContent()));
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
