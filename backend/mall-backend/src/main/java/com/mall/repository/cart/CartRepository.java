package com.mall.repository.cart;

import com.mall.entity.cart.CartItemEntity;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface CartRepository extends ReactiveCrudRepository<CartItemEntity, Long>, CartRepositoryCustom {

    Mono<CartItemEntity> findByUserIdAndProductId(Long userId, Long productId);

    Mono<CartItemEntity> findByIdAndUserId(Long id, Long userId);
}
