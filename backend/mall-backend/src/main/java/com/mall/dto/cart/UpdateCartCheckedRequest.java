package com.mall.dto.cart;

import jakarta.validation.constraints.NotNull;

public record UpdateCartCheckedRequest(
        @NotNull(message = "checked is required")
        Boolean checked
) {
}
