package com.mall.dto.order;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CheckoutOrderRequest(
        @NotEmpty(message = "cartItemIds must not be empty")
        List<Long> cartItemIds
) {
}
