package com.mall.service.chat.retrieval;

import com.mall.dto.product.ProductListResponse;
import com.mall.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Service
public class ChatProductResolver {

    private final ProductService productService;
    private final ChatEmbeddingService chatEmbeddingService;

    public ChatProductResolver(ProductService productService, ChatEmbeddingService chatEmbeddingService) {
        this.productService = productService;
        this.chatEmbeddingService = chatEmbeddingService;
    }

    public Mono<ProductListResponse> searchProductsByQuery(String query, int pageSize) {
        if (!StringUtils.hasText(query)) {
            return productService.getProductList(1, pageSize, null, null, null);
        }

        return chatEmbeddingService.embedToPgVectorLiteral(query)
                .flatMap(vector -> productService.recommendProductsByQueryEmbedding(vector, pageSize))
                .flatMap(result -> {
                    if (result.list() != null && !result.list().isEmpty()) {
                        return Mono.just(result);
                    }
                    return productService.searchProducts(query, 1, pageSize, null, null);
                })
                .switchIfEmpty(productService.searchProducts(query, 1, pageSize, null, null))
                .onErrorResume(ex -> productService.searchProducts(query, 1, pageSize, null, null));
    }

    public Mono<ProductListResponse> recommendProductsByQuery(String query, int pageSize) {
        if (!StringUtils.hasText(query)) {
            return productService.getProductList(1, pageSize, null, null, null);
        }

        return chatEmbeddingService.embedToPgVectorLiteral(query)
                .flatMap(vector -> productService.recommendProductsByQueryEmbedding(vector, pageSize))
                .flatMap(result -> {
                    if (result.list() != null && !result.list().isEmpty()) {
                        return Mono.just(result);
                    }
                    return productService.searchProducts(query, 1, pageSize, null, null);
                })
                .onErrorResume(ex -> productService.searchProducts(query, 1, pageSize, null, null));
    }
}
