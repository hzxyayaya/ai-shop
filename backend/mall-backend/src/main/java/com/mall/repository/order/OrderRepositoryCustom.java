package com.mall.repository.order;

import com.mall.entity.order.OrderEntity;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderRepositoryCustom {

    Flux<OrderEntity> findOrderPage(Long userId, String status, String payStatus, int limit, long offset);

    Mono<Long> countOrders(Long userId, String status, String payStatus);
}
