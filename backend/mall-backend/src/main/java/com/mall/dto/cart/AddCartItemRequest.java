package com.mall.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddCartItemRequest(
        @NotNull(message = "productId is required")
        Long productId,
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be greater than or equal to 1")
        Integer quantity
) {
}
