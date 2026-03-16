package com.mall.dto.product;

import java.util.List;

public record ProductListResponse(
        int page,
        int pageSize,
        long total,
        List<ProductDto> list
) {
}
