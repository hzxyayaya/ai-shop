package com.mall.repository.cart;

import com.mall.model.cart.CartItemView;

import java.math.BigDecimal;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public class CartRepositoryImpl implements CartRepositoryCustom {

    private final DatabaseClient databaseClient;

    public CartRepositoryImpl(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Flux<CartItemView> findCartItemsByUserId(Long userId) {
        String sql = """
                SELECT c.id,
                       c.product_id,
                       p.title,
                       p.category,
                       p.price,
                       p.sales,
                       p.image_url,
                       p.shop_name,
                       c.quantity,
                       c.checked
                FROM cart_item c
                JOIN product p ON p.id = c.product_id
                WHERE c.user_id = :userId
                ORDER BY c.updated_at DESC, c.id DESC
                """;

        return databaseClient.sql(sql)
                .bind("userId", userId)
                .map((row, metadata) -> CartItemView.builder()
                        .id(row.get("id", Long.class))
                        .productId(row.get("product_id", Long.class))
                        .title(row.get("title", String.class))
                        .category(row.get("category", String.class))
                        .price(row.get("price", BigDecimal.class))
                        .sales(row.get("sales", String.class))
                        .imageUrl(row.get("image_url", String.class))
                        .shopName(row.get("shop_name", String.class))
                        .quantity(row.get("quantity", Integer.class))
                        .checked(row.get("checked", Boolean.class))
                        .build())
                .all();
    }
}
