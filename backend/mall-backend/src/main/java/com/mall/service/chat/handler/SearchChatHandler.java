package com.mall.service.chat.handler;

import com.mall.dto.chat.ChatResponse;
import com.mall.model.AuthenticatedUser;
import com.mall.service.chat.intent.ChatIntent;
import com.mall.service.chat.llm.ChatAiAnalysis;
import com.mall.service.chat.memory.ChatMemoryService;
import com.mall.service.chat.retrieval.ChatProductResolver;
import com.mall.service.chat.retrieval.ChatQueryPlanner;
import com.mall.service.chat.retrieval.ProductQueryPlan;
import com.mall.service.chat.support.ChatResponseFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class SearchChatHandler {

    private final ChatQueryPlanner chatQueryPlanner;
    private final ChatProductResolver chatProductResolver;
    private final ChatMemoryService chatMemoryService;
    private final ChatResponseFactory chatResponseFactory;

    public SearchChatHandler(
            ChatQueryPlanner chatQueryPlanner,
            ChatProductResolver chatProductResolver,
            ChatMemoryService chatMemoryService,
            ChatResponseFactory chatResponseFactory
    ) {
        this.chatQueryPlanner = chatQueryPlanner;
        this.chatProductResolver = chatProductResolver;
        this.chatMemoryService = chatMemoryService;
        this.chatResponseFactory = chatResponseFactory;
    }

    public Mono<ChatResponse> handleSearch(AuthenticatedUser currentUser, String sessionId, String message, ChatAiAnalysis aiAnalysis) {
        ProductQueryPlan queryPlan = chatQueryPlanner.plan(message, aiAnalysis);
        return chatProductResolver.searchProductsByQuery(queryPlan.query(), 6)
                .map(com.mall.dto.product.ProductListResponse::list)
                .flatMap(products -> {
                    if (products.isEmpty()) {
                        return chatMemoryService.remember(
                                currentUser,
                                sessionId,
                                message,
                                ChatIntent.SEARCH_PRODUCT,
                                chatResponseFactory.emptySearch(),
                                java.util.List.of(),
                                null
                        );
                    }
                    String reply = aiAnalysis != null && aiAnalysis.replyMessage() != null && !aiAnalysis.replyMessage().isBlank()
                            ? aiAnalysis.replyMessage()
                            : "我帮你找到了 " + products.size() + " 个相关商品。";
                    return chatMemoryService.rememberProducts(
                            currentUser,
                            sessionId,
                            message,
                            ChatIntent.SEARCH_PRODUCT,
                            chatResponseFactory.searchResult(products, reply),
                            products
                    );
                });
    }

    public Mono<ChatResponse> handleRecommend(AuthenticatedUser currentUser, String sessionId, String message, ChatAiAnalysis aiAnalysis) {
        ProductQueryPlan queryPlan = chatQueryPlanner.plan(message, aiAnalysis);
                return chatProductResolver.recommendProductsByQuery(queryPlan.query(), 6)
                .map(com.mall.dto.product.ProductListResponse::list)
                .flatMap(products -> {
                    if (products.isEmpty()) {
                        return chatMemoryService.remember(
                                currentUser,
                                sessionId,
                                message,
                                ChatIntent.RECOMMEND_PRODUCT,
                                chatResponseFactory.emptyRecommend(),
                                java.util.List.of(),
                                null
                        );
                    }
                    String reply = aiAnalysis != null && aiAnalysis.recommendationReason() != null && !aiAnalysis.recommendationReason().isBlank()
                            ? aiAnalysis.recommendationReason()
                            : (aiAnalysis != null && aiAnalysis.replyMessage() != null && !aiAnalysis.replyMessage().isBlank()
                            ? aiAnalysis.replyMessage()
                            : "我先帮你挑了几款更值得优先看的商品。");
                    return chatMemoryService.rememberProducts(
                            currentUser,
                            sessionId,
                            message,
                            ChatIntent.RECOMMEND_PRODUCT,
                            chatResponseFactory.recommendResult(products, reply),
                            products
                    );
                });
    }
}
