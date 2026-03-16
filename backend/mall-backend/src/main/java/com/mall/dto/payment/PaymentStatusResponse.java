package com.mall.dto.payment;

public record PaymentStatusResponse(
        String orderNo,
        String payStatus
) {
}
