package com.mall.dto.payment;

import jakarta.validation.constraints.NotBlank;

public record CreatePaymentRequest(
        @NotBlank(message = "orderNo is required")
        String orderNo
) {
}
