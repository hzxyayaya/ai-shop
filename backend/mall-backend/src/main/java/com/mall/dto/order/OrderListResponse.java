package com.mall.dto.order;

import java.util.List;

public record OrderListResponse(
        int page,
        int pageSize,
        long total,
        List<OrderDto> list
) {
}
