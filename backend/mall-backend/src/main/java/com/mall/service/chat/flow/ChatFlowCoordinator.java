package com.mall.service.chat.flow;

import com.mall.dto.chat.ChatResponse;
import com.mall.model.AuthenticatedUser;
import com.mall.service.chat.handler.CartChatHandler;
import com.mall.service.chat.handler.GeneralChatHandler;
import com.mall.service.chat.handler.OrderChatHandler;
import com.mall.service.chat.handler.SearchChatHandler;
import com.mall.service.chat.intent.ChatIntent;
import com.mall.service.chat.intent.IntentCoordinator;
import com.mall.service.chat.intent.ResolvedIntentDecision;
import com.mall.service.chat.llm.ChatActionAssistant;
import com.mall.service.chat.llm.ChatAiAnalysis;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ChatFlowCoordinator {

    private final IntentCoordinator intentCoordinator;
    private final SearchChatHandler searchChatHandler;
    private final CartChatHandler cartChatHandler;
    private final OrderChatHandler orderChatHandler;
    private final GeneralChatHandler generalChatHandler;
    private final ChatActionAssistant chatActionAssistant;

    public ChatFlowCoordinator(
            IntentCoordinator intentCoordinator,
            SearchChatHandler searchChatHandler,
            CartChatHandler cartChatHandler,
            OrderChatHandler orderChatHandler,
            GeneralChatHandler generalChatHandler,
            ChatActionAssistant chatActionAssistant
    ) {
        this.intentCoordinator = intentCoordinator;
        this.searchChatHandler = searchChatHandler;
        this.cartChatHandler = cartChatHandler;
        this.orderChatHandler = orderChatHandler;
        this.generalChatHandler = generalChatHandler;
        this.chatActionAssistant = chatActionAssistant;
    }

    public Mono<ChatResponse> execute(
            AuthenticatedUser currentUser,
            String sessionId,
            String message,
            ChatAiAnalysis aiAnalysis
    ) {
        ResolvedIntentDecision decision = intentCoordinator.resolve(message, aiAnalysis);
        ChatIntent intent = decision.intent();
        return switch (intent) {
            case SEARCH_PRODUCT -> executeActionIntent(intent, currentUser, sessionId, message, aiAnalysis,
                    () -> searchChatHandler.handleSearch(currentUser, sessionId, message, aiAnalysis));
            case RECOMMEND_PRODUCT -> executeActionIntent(intent, currentUser, sessionId, message, aiAnalysis,
                    () -> searchChatHandler.handleRecommend(currentUser, sessionId, message, aiAnalysis));
            case ADD_TO_CART -> executeActionIntent(intent, currentUser, sessionId, message, aiAnalysis,
                    () -> cartChatHandler.handleAddToCart(currentUser, sessionId, message));
            case VIEW_CART -> executeActionIntent(intent, currentUser, sessionId, message, aiAnalysis,
                    () -> cartChatHandler.handleViewCart(currentUser, sessionId, message));
            case BUY_NOW -> executeActionIntent(intent, currentUser, sessionId, message, aiAnalysis,
                    () -> orderChatHandler.handleBuyNow(currentUser, sessionId, message));
            case VIEW_ORDER -> executeActionIntent(intent, currentUser, sessionId, message, aiAnalysis,
                    () -> orderChatHandler.handleViewOrder(currentUser, sessionId, message));
            case PAY_GUIDE -> executeActionIntent(intent, currentUser, sessionId, message, aiAnalysis,
                    () -> orderChatHandler.handlePayGuide(currentUser, sessionId, message));
            default -> executeActionIntent(intent, currentUser, sessionId, message, aiAnalysis,
                    () -> generalChatHandler.handleGeneralQa(currentUser, sessionId, message, aiAnalysis));
        };
    }

    private Mono<ChatResponse> executeActionIntent(
            ChatIntent intent,
            AuthenticatedUser currentUser,
            String sessionId,
            String message,
            ChatAiAnalysis aiAnalysis,
            ResponseSupplier fallback
    ) {
        return chatActionAssistant.execute(currentUser, sessionId, message, intent, aiAnalysis)
                .switchIfEmpty(fallback.get());
    }

    @FunctionalInterface
    private interface ResponseSupplier {
        Mono<ChatResponse> get();
    }
}
