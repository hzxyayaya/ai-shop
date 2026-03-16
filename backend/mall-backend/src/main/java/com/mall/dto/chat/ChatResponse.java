package com.mall.dto.chat;

import com.mall.dto.order.OrderDto;
import com.mall.dto.product.ProductDto;
import java.util.List;

public record ChatResponse(
        String intent,
        String message,
        List<ProductDto> products,
        List<OrderDto> orders,
        List<ChatActionDto> actions
) {
}
