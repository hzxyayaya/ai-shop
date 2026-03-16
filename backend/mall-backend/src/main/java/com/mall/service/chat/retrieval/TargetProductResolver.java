package com.mall.service.chat.retrieval;

import com.mall.common.exception.BusinessException;
import com.mall.dto.product.ProductDto;
import com.mall.model.AuthenticatedUser;
import com.mall.service.ProductService;
import com.mall.service.chat.intent.ChatMessageParser;
import com.mall.service.chat.memory.ChatMemoryService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class TargetProductResolver {

    private final ProductService productService;
    private final ChatMessageParser messageParser;
    private final ChatMemoryService chatMemoryService;
    private final ChatQueryPlanner chatQueryPlanner;

    public TargetProductResolver(
            ProductService productService,
            ChatMessageParser messageParser,
            ChatMemoryService chatMemoryService,
            ChatQueryPlanner chatQueryPlanner
    ) {
        this.productService = productService;
        this.messageParser = messageParser;
        this.chatMemoryService = chatMemoryService;
        this.chatQueryPlanner = chatQueryPlanner;
    }

    public Mono<ProductDto> resolve(AuthenticatedUser currentUser, String sessionId, String message) {
        Long productId = messageParser.extractProductId(message);
        if (productId != null) {
            return productService.getProductDetail(productId);
        }

        Integer ordinal = messageParser.extractOrdinal(message);
        if (ordinal != null) {
            return chatMemoryService.resolveProductIdByOrdinal(currentUser, sessionId, ordinal)
                    .flatMap(productService::getProductDetail);
        }

        ProductQueryPlan queryPlan = chatQueryPlanner.plan(message, null);
        return productService.searchProducts(queryPlan.query(), 1, 1, null, null)
                .map(com.mall.dto.product.ProductListResponse::list)
                .flatMap(products -> products.isEmpty()
                        ? Mono.error(new BusinessException(404, "resource not found"))
                        : Mono.just(products.get(0)));
    }
}
