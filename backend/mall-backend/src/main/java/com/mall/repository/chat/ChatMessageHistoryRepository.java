package com.mall.repository.chat;

import com.mall.entity.chat.ChatMessageHistoryEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChatMessageHistoryRepository extends ReactiveCrudRepository<ChatMessageHistoryEntity, Long> {

    Flux<ChatMessageHistoryEntity> findTop20ByUserIdAndSessionIdOrderByCreatedAtDesc(Long userId, String sessionId);

    Mono<Void> deleteBySessionIdAndUserId(String sessionId, Long userId);
}
