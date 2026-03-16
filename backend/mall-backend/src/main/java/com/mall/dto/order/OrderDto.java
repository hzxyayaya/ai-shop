package com.mall.dto.order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OrderDto(
        String orderNo,
        BigDecimal totalAmount,
        String status,
        String payStatus,
        OffsetDateTime createdAt,
        List<OrderItemDto> items
) {
}
