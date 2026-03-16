package com.mall.dto.payment;

public record PaymentCreateResponse(
        String orderNo,
        String payType,
        String payForm
) {
}
