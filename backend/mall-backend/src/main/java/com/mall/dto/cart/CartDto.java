package com.mall.dto.cart;

import java.math.BigDecimal;

public record CartDto(
        Long id,
        Long productId,
        String title,
        String category,
        BigDecimal price,
        String sales,
        String imageUrl,
        String shopName,
        Integer quantity,
        Boolean checked
) {
}
