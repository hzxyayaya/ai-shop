package com.mall.dto.product;

import java.math.BigDecimal;

public record ProductDto(
        Long id,
        String category,
        String title,
        BigDecimal price,
        String sales,
        String imageUrl,
        String shopName
) {
}
