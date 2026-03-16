package com.mall.dto.order;

import java.math.BigDecimal;

public record OrderItemDto(
        Long productId,
        String title,
        String category,
        BigDecimal price,
        Integer quantity,
        BigDecimal amount,
        String imageUrl,
        String shopName
) {
}
