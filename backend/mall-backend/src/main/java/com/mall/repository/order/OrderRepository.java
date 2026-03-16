package com.mall.repository.order;

import com.mall.entity.order.OrderEntity;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface OrderRepository extends ReactiveCrudRepository<OrderEntity, Long>, OrderRepositoryCustom {

    Mono<OrderEntity> findByOrderNo(String orderNo);

    Mono<OrderEntity> findByOrderNoAndUserId(String orderNo, Long userId);
}
