package com.mall.repository.product;

import com.mall.entity.product.ProductEntity;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductRepositoryCustom {

    Flux<ProductEntity> findProductPage(String category, int limit, long offset, boolean sortAscending);

    Mono<Long> countByCategory(String category);

    Flux<ProductEntity> searchProductPage(String keyword, int limit, long offset, boolean sortAscending);

    Mono<Long> countByKeyword(String keyword);

    Flux<ProductEntity> recommendProductPageByQueryEmbedding(String queryEmbedding, int limit);
}
