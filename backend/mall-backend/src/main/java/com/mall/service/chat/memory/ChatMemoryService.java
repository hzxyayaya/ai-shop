package com.mall.service.chat.memory;

import com.mall.common.exception.BusinessException;
import com.mall.dto.chat.ChatResponse;
import com.mall.dto.product.ProductDto;
import com.mall.model.AuthenticatedUser;
import com.mall.service.ChatSessionContextService;
import com.mall.service.chat.intent.ChatIntent;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ChatMemoryService {

    private final ChatSessionContextService chatSessionContextService;

    public ChatMemoryService(ChatSessionContextService chatSessionContextService) {
        this.chatSessionContextService = chatSessionContextService;
    }

    public Mono<Long> resolveProductIdByOrdinal(AuthenticatedUser currentUser, String sessionId, int ordinal) {
        Long userId = requireUserId(currentUser);
        return chatSessionContextService.getContext(userId, sessionId)
                .switchIfEmpty(Mono.error(new BusinessException(404, "resource not found")))
                .flatMap(context -> {
                    List<Long> productIds = new ArrayList<>(chatSessionContextService.readProductIds(context));
                    if (ordinal < 0 || ordinal >= productIds.size()) {
                        return Mono.error(new BusinessException(404, "resource not found"));
                    }
                    return Mono.just(productIds.get(ordinal));
                });
    }

    public Mono<ChatResponse> remember(
            AuthenticatedUser currentUser,
            String sessionId,
            String message,
            ChatIntent intent,
            ChatResponse response,
            List<Long> productIds,
            String orderNo
    ) {
        Long userId = requireUserId(currentUser);
        return chatSessionContextService.saveContext(userId, sessionId, intent.code(), message, productIds, orderNo)
                .thenReturn(response);
    }

    public Mono<ChatResponse> rememberPreservingRecentProducts(
            AuthenticatedUser currentUser,
            String sessionId,
            String message,
            ChatIntent intent,
            ChatResponse response,
            List<Long> fallbackProductIds,
            String orderNo
    ) {
        Long userId = requireUserId(currentUser);
        return chatSessionContextService.getContext(userId, sessionId)
                .map(chatSessionContextService::readProductIds)
                .defaultIfEmpty(List.of())
                .flatMap(existingProductIds -> remember(
                        currentUser,
                        sessionId,
                        message,
                        intent,
                        response,
                        existingProductIds.isEmpty() ? fallbackProductIds : existingProductIds,
                        orderNo
                ));
    }

    public Mono<ChatResponse> rememberProducts(
            AuthenticatedUser currentUser,
            String sessionId,
            String message,
            ChatIntent intent,
            ChatResponse response,
            List<ProductDto> products
    ) {
        return remember(
                currentUser,
                sessionId,
                message,
                intent,
                response,
                products.stream().map(ProductDto::id).toList(),
                null
        );
    }

    private Long requireUserId(AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.id() == null) {
            throw new BusinessException(401, "unauthorized");
        }
        return currentUser.id();
    }
}
