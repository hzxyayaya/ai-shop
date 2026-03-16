package com.mall.repository.payment;

import com.mall.entity.payment.PaymentRecordEntity;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PaymentRecordRepository extends ReactiveCrudRepository<PaymentRecordEntity, Long> {

    Mono<PaymentRecordEntity> findFirstByOrderNoOrderByUpdatedAtDesc(String orderNo);

    Mono<Void> deleteByOrderNoAndUserId(String orderNo, Long userId);
}
