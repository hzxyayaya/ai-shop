package com.mall.service.chat.handler;

import com.mall.dto.cart.AddCartItemRequest;
import com.mall.dto.chat.ChatResponse;
import com.mall.model.AuthenticatedUser;
import com.mall.service.CartService;
import com.mall.service.chat.intent.ChatIntent;
import com.mall.service.chat.memory.ChatMemoryService;
import com.mall.service.chat.retrieval.TargetProductResolver;
import com.mall.service.chat.support.ChatResponseFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class CartChatHandler {

    private final CartService cartService;
    private final TargetProductResolver targetProductResolver;
    private final ChatMemoryService chatMemoryService;
    private final ChatResponseFactory chatResponseFactory;

    public CartChatHandler(
            CartService cartService,
            TargetProductResolver targetProductResolver,
            ChatMemoryService chatMemoryService,
            ChatResponseFactory chatResponseFactory
    ) {
        this.cartService = cartService;
        this.targetProductResolver = targetProductResolver;
        this.chatMemoryService = chatMemoryService;
        this.chatResponseFactory = chatResponseFactory;
    }

    public Mono<ChatResponse> handleAddToCart(AuthenticatedUser currentUser, String sessionId, String message) {
        return targetProductResolver.resolve(currentUser, sessionId, message)
                .flatMap(product -> cartService.addToCart(currentUser, new AddCartItemRequest(product.id(), 1))
                        .then(chatMemoryService.rememberPreservingRecentProducts(
                                currentUser,
                                sessionId,
                                message,
                                ChatIntent.ADD_TO_CART,
                                chatResponseFactory.addToCart(product),
                                java.util.List.of(product.id()),
                                null
                        )));
    }

    public Mono<ChatResponse> handleViewCart(AuthenticatedUser currentUser, String sessionId, String message) {
        return chatMemoryService.rememberPreservingRecentProducts(
                currentUser,
                sessionId,
                message,
                ChatIntent.VIEW_CART,
                chatResponseFactory.viewCart(),
                java.util.List.of(),
                null
        );
    }
}
