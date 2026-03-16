package com.mall.repository.chat;

import com.mall.entity.chat.ChatSessionContextEntity;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ChatSessionContextRepository extends ReactiveCrudRepository<ChatSessionContextEntity, Long> {

    Mono<ChatSessionContextEntity> findBySessionIdAndUserId(String sessionId, Long userId);

    Mono<Void> deleteBySessionIdAndUserId(String sessionId, Long userId);
}
