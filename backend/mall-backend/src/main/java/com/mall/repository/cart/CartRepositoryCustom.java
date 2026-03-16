package com.mall.repository.cart;

import com.mall.model.cart.CartItemView;

import reactor.core.publisher.Flux;

public interface CartRepositoryCustom {

    Flux<CartItemView> findCartItemsByUserId(Long userId);
}
