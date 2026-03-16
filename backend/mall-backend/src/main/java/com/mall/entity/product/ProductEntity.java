package com.mall.entity.product;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("product")
public class ProductEntity {

    @Id
    private Long id;

    private String category;

    private String title;

    private BigDecimal price;

    private String sales;

    @Column("image_url")
    private String imageUrl;

    @Column("shop_name")
    private String shopName;
}
