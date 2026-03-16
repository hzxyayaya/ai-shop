package com.mall.repository.product;

import com.mall.entity.product.ProductEntity;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ProductRepository extends ReactiveCrudRepository<ProductEntity, Long>, ProductRepositoryCustom {

    @Query("""
            SELECT id, category, title, price, sales, image_url, shop_name
            FROM product
            WHERE id = :id
            """)
    Mono<ProductEntity> findProjectedById(Long id);
}
