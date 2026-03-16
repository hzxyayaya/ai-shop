package com.mall.service.chat.handler;

import com.mall.dto.chat.ChatResponse;
import com.mall.dto.order.BuyNowOrderRequest;
import com.mall.dto.order.OrderListResponse;
import com.mall.model.AuthenticatedUser;
import com.mall.service.OrderService;
import com.mall.service.chat.intent.ChatIntent;
import com.mall.service.chat.memory.ChatMemoryService;
import com.mall.service.chat.retrieval.TargetProductResolver;
import com.mall.service.chat.support.ChatResponseFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class OrderChatHandler {

    private final OrderService orderService;
    private final TargetProductResolver targetProductResolver;
    private final ChatMemoryService chatMemoryService;
    private final ChatResponseFactory chatResponseFactory;

    public OrderChatHandler(
            OrderService orderService,
            TargetProductResolver targetProductResolver,
            ChatMemoryService chatMemoryService,
            ChatResponseFactory chatResponseFactory
    ) {
        this.orderService = orderService;
        this.targetProductResolver = targetProductResolver;
        this.chatMemoryService = chatMemoryService;
        this.chatResponseFactory = chatResponseFactory;
    }

    public Mono<ChatResponse> handleBuyNow(AuthenticatedUser currentUser, String sessionId, String message) {
        return targetProductResolver.resolve(currentUser, sessionId, message)
                .flatMap(product -> orderService.createBuyNowOrder(currentUser, new BuyNowOrderRequest(product.id(), 1))
                        .flatMap(order -> chatMemoryService.rememberPreservingRecentProducts(
                                currentUser,
                                sessionId,
                                message,
                                ChatIntent.BUY_NOW,
                                chatResponseFactory.buyNow(order, product),
                                java.util.List.of(product.id()),
                                order.orderNo()
                        )));
    }

    public Mono<ChatResponse> handleViewOrder(AuthenticatedUser currentUser, String sessionId, String message) {
        return orderService.getOrders(currentUser, 1, 10, null, null)
                .map(OrderListResponse::list)
                .flatMap(orders -> {
                    if (orders.isEmpty()) {
                        return chatMemoryService.remember(
                                currentUser,
                                sessionId,
                                message,
                                ChatIntent.VIEW_ORDER,
                                chatResponseFactory.emptyOrders(),
                                java.util.List.of(),
                                null
                        );
                    }
                    return chatMemoryService.remember(
                            currentUser,
                            sessionId,
                            message,
                            ChatIntent.VIEW_ORDER,
                            chatResponseFactory.viewOrders(orders),
                            java.util.List.of(),
                            chatResponseFactory.orderNoForOrderViewMemory(orders)
                    );
                });
    }

    public Mono<ChatResponse> handlePayGuide(AuthenticatedUser currentUser, String sessionId, String message) {
        return orderService.getOrders(currentUser, 1, 10, null, "UNPAID")
                .map(OrderListResponse::list)
                .flatMap(orders -> {
                    if (orders.isEmpty()) {
                        return chatMemoryService.remember(
                                currentUser,
                                sessionId,
                                message,
                                ChatIntent.PAY_GUIDE,
                                chatResponseFactory.emptyPayGuide(),
                                java.util.List.of(),
                                null
                        );
                    }
                    return chatMemoryService.remember(
                            currentUser,
                            sessionId,
                            message,
                            ChatIntent.PAY_GUIDE,
                            chatResponseFactory.payGuide(orders),
                            java.util.List.of(),
                            orders.get(0).orderNo()
                    );
                });
    }
}
