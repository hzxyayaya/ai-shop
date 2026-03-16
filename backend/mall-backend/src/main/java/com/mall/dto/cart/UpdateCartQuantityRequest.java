package com.mall.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCartQuantityRequest(
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be greater than or equal to 1")
        Integer quantity
) {
}
