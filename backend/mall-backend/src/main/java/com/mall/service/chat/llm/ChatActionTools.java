package com.mall.service.chat.llm;

import com.mall.dto.chat.ChatResponse;
import com.mall.service.chat.handler.CartChatHandler;
import com.mall.service.chat.handler.GeneralChatHandler;
import com.mall.service.chat.handler.OrderChatHandler;
import com.mall.service.chat.handler.SearchChatHandler;
import com.mall.service.chat.intent.ChatIntent;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.springframework.stereotype.Component;

@Component
public class ChatActionTools {

    private final CartChatHandler cartChatHandler;
    private final OrderChatHandler orderChatHandler;
    private final SearchChatHandler searchChatHandler;
    private final GeneralChatHandler generalChatHandler;
    private final ChatActionExecutionContextRegistry contextRegistry;

    public ChatActionTools(
            CartChatHandler cartChatHandler,
            OrderChatHandler orderChatHandler,
            SearchChatHandler searchChatHandler,
            GeneralChatHandler generalChatHandler,
            ChatActionExecutionContextRegistry contextRegistry
    ) {
        this.cartChatHandler = cartChatHandler;
        this.orderChatHandler = orderChatHandler;
        this.searchChatHandler = searchChatHandler;
        this.generalChatHandler = generalChatHandler;
        this.contextRegistry = contextRegistry;
    }

    @Tool("Search products from the real catalog using the saved request context.")
    public String searchProducts(@ToolMemoryId String memoryId) {
        execute(memoryId, ChatIntent.SEARCH_PRODUCT, state -> {
            ChatSessionMemoryKey key = ChatSessionMemoryKey.parse(memoryId);
            return searchChatHandler.handleSearch(
                state.currentUser(),
                key.sessionId(),
                state.message(),
                state.aiAnalysis()
            );
        });
        return "SEARCH_PRODUCT_EXECUTED";
    }

    @Tool("Recommend products from the real catalog using the saved request context.")
    public String recommendProducts(@ToolMemoryId String memoryId) {
        execute(memoryId, ChatIntent.RECOMMEND_PRODUCT, state -> {
            ChatSessionMemoryKey key = ChatSessionMemoryKey.parse(memoryId);
            return searchChatHandler.handleRecommend(
                state.currentUser(),
                key.sessionId(),
                state.message(),
                state.aiAnalysis()
            );
        });
        return "RECOMMEND_PRODUCT_EXECUTED";
    }

    @Tool("Answer a general shopping question using the saved request context.")
    public String generalQa(@ToolMemoryId String memoryId) {
        execute(memoryId, ChatIntent.GENERAL_QA, state -> {
            ChatSessionMemoryKey key = ChatSessionMemoryKey.parse(memoryId);
            return generalChatHandler.handleGeneralQa(
                state.currentUser(),
                key.sessionId(),
                state.message(),
                state.aiAnalysis()
            );
        });
        return "GENERAL_QA_EXECUTED";
    }

    @Tool("Add the selected product to the cart using the saved request context.")
    public String addToCart(@ToolMemoryId String memoryId) {
        execute(memoryId, ChatIntent.ADD_TO_CART, state -> {
            ChatSessionMemoryKey key = ChatSessionMemoryKey.parse(memoryId);
            return cartChatHandler.handleAddToCart(state.currentUser(), key.sessionId(), state.message());
        });
        return "ADD_TO_CART_EXECUTED";
    }

    @Tool("Show the current shopping cart using the saved request context.")
    public String viewCart(@ToolMemoryId String memoryId) {
        execute(memoryId, ChatIntent.VIEW_CART, state -> {
            ChatSessionMemoryKey key = ChatSessionMemoryKey.parse(memoryId);
            return cartChatHandler.handleViewCart(state.currentUser(), key.sessionId(), state.message());
        });
        return "VIEW_CART_EXECUTED";
    }

    @Tool("Create a buy-now order for the selected product using the saved request context.")
    public String buyNow(@ToolMemoryId String memoryId) {
        execute(memoryId, ChatIntent.BUY_NOW, state -> {
            ChatSessionMemoryKey key = ChatSessionMemoryKey.parse(memoryId);
            return orderChatHandler.handleBuyNow(state.currentUser(), key.sessionId(), state.message());
        });
        return "BUY_NOW_EXECUTED";
    }

    @Tool("Show the user's recent orders using the saved request context.")
    public String viewOrder(@ToolMemoryId String memoryId) {
        execute(memoryId, ChatIntent.VIEW_ORDER, state -> {
            ChatSessionMemoryKey key = ChatSessionMemoryKey.parse(memoryId);
            return orderChatHandler.handleViewOrder(state.currentUser(), key.sessionId(), state.message());
        });
        return "VIEW_ORDER_EXECUTED";
    }

    @Tool("Show how to pay for the user's unpaid order using the saved request context.")
    public String payGuide(@ToolMemoryId String memoryId) {
        execute(memoryId, ChatIntent.PAY_GUIDE, state -> {
            ChatSessionMemoryKey key = ChatSessionMemoryKey.parse(memoryId);
            return orderChatHandler.handlePayGuide(state.currentUser(), key.sessionId(), state.message());
        });
        return "PAY_GUIDE_EXECUTED";
    }

    private void execute(
            String memoryId,
            ChatIntent expectedIntent,
            ChatResponseSupplier supplier
    ) {
        ChatActionExecutionContextRegistry.ChatActionExecutionState state = contextRegistry.get(memoryId);
        if (state == null) {
            throw new IllegalStateException("Missing action execution context for memory " + memoryId);
        }
        if (state.intent() != expectedIntent) {
            throw new IllegalStateException("Tool intent mismatch: expected " + expectedIntent + " but was " + state.intent());
        }
        ChatResponse response = supplier.get(state).blockOptional()
                .orElseThrow(() -> new IllegalStateException("Action handler returned empty response"));
        state.storeResponse(response);
    }

    @FunctionalInterface
    private interface ChatResponseSupplier {
        reactor.core.publisher.Mono<ChatResponse> get(ChatActionExecutionContextRegistry.ChatActionExecutionState state);
    }
}
