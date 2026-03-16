package com.mall.model.cart;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemView {

    private Long id;
    private Long productId;
    private String title;
    private String category;
    private BigDecimal price;
    private String sales;
    private String imageUrl;
    private String shopName;
    private Integer quantity;
    private Boolean checked;
}
