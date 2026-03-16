package com.mall.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.entity.chat.ChatSessionContextEntity;
import com.mall.repository.chat.ChatSessionContextRepository;
import com.mall.common.exception.BusinessException;
import io.r2dbc.postgresql.codec.Json;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ChatSessionContextService {

    private static final TypeReference<List<Long>> LONG_LIST_TYPE = new TypeReference<>() {};

    private final ChatSessionContextRepository repository;
    private final ObjectMapper objectMapper;

    public ChatSessionContextService(ChatSessionContextRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public Mono<ChatSessionContextEntity> getContext(Long userId, String sessionId) {
        return repository.findBySessionIdAndUserId(sessionId, userId);
    }

    public Mono<Void> saveContext(
            Long userId,
            String sessionId,
            String intent,
            String userMessage,
            List<Long> productIds,
            String orderNo
    ) {
        return repository.findBySessionIdAndUserId(sessionId, userId)
                .defaultIfEmpty(ChatSessionContextEntity.builder()
                        .userId(userId)
                        .sessionId(sessionId)
                        .build())
                .flatMap(context -> {
                    context.setLastIntent(intent);
                    context.setLastUserMessage(userMessage);
                    context.setLastProductIdsJson(productIds == null ? null : Json.of(writeProductIds(productIds)));
                    context.setLastOrderNo(orderNo);
                    context.setUpdatedAt(OffsetDateTime.now());
                    return repository.save(context);
                })
                .then();
    }

    public List<Long> readProductIds(ChatSessionContextEntity context) {
        if (context == null || context.getLastProductIdsJson() == null) {
            return List.of();
        }
        try {
            return objectMapper.readValue(context.getLastProductIdsJson().asString(), LONG_LIST_TYPE);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500, "internal server error");
        }
    }

    public Mono<String> summarizeContext(Long userId, String sessionId) {
        return repository.findBySessionIdAndUserId(sessionId, userId)
                .map(context -> "lastIntent=" + defaultString(context.getLastIntent())
                        + ", lastUserMessage=" + defaultString(context.getLastUserMessage())
                        + ", lastProductIds=" + readProductIds(context)
                        + ", lastOrderNo=" + defaultString(context.getLastOrderNo()))
                .defaultIfEmpty("lastIntent=, lastUserMessage=, lastProductIds=[], lastOrderNo=");
    }

    public Mono<Void> clearSession(Long userId, String sessionId) {
        return repository.deleteBySessionIdAndUserId(sessionId, userId);
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String writeProductIds(List<Long> productIds) {
        try {
            return objectMapper.writeValueAsString(productIds);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500, "internal server error");
        }
    }
}
