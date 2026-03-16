package com.mall.repository.product;

import com.mall.entity.product.ProductEntity;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private static final String PRODUCT_COLUMNS = """
            SELECT id, category, title, price, sales, image_url, shop_name
            FROM product
            """;

    private final DatabaseClient databaseClient;

    public ProductRepositoryImpl(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Flux<ProductEntity> findProductPage(String category, int limit, long offset, boolean sortAscending) {
        StringBuilder sql = new StringBuilder(PRODUCT_COLUMNS);
        if (StringUtils.hasText(category)) {
            sql.append(" WHERE category = :category");
        }
        sql.append(" ORDER BY price ").append(sortAscending ? "ASC" : "DESC");
        sql.append(" LIMIT :limit OFFSET :offset");

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString())
                .bind("limit", limit)
                .bind("offset", offset);
        if (StringUtils.hasText(category)) {
            spec = spec.bind("category", category);
        }
        return spec.map((row, metadata) -> ProductEntity.builder()
                        .id(row.get("id", Long.class))
                        .category(row.get("category", String.class))
                        .title(row.get("title", String.class))
                        .price(row.get("price", java.math.BigDecimal.class))
                        .sales(row.get("sales", String.class))
                        .imageUrl(row.get("image_url", String.class))
                        .shopName(row.get("shop_name", String.class))
                        .build())
                .all();
    }

    @Override
    public Mono<Long> countByCategory(String category) {
        String sql = StringUtils.hasText(category)
                ? "SELECT COUNT(1) AS total FROM product WHERE category = :category"
                : "SELECT COUNT(1) AS total FROM product";
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql);
        if (StringUtils.hasText(category)) {
            spec = spec.bind("category", category);
        }
        return spec.map((row, metadata) -> row.get("total", Long.class)).one().defaultIfEmpty(0L);
    }

    @Override
    public Flux<ProductEntity> searchProductPage(String keyword, int limit, long offset, boolean sortAscending) {
        String sql = PRODUCT_COLUMNS
                + " WHERE title ILIKE :keyword"
                + " ORDER BY price " + (sortAscending ? "ASC" : "DESC")
                + " LIMIT :limit OFFSET :offset";

        return databaseClient.sql(sql)
                .bind("keyword", wrapKeyword(keyword))
                .bind("limit", limit)
                .bind("offset", offset)
                .map((row, metadata) -> ProductEntity.builder()
                        .id(row.get("id", Long.class))
                        .category(row.get("category", String.class))
                        .title(row.get("title", String.class))
                        .price(row.get("price", java.math.BigDecimal.class))
                        .sales(row.get("sales", String.class))
                        .imageUrl(row.get("image_url", String.class))
                        .shopName(row.get("shop_name", String.class))
                        .build())
                .all();
    }

    @Override
    public Mono<Long> countByKeyword(String keyword) {
        return databaseClient.sql("SELECT COUNT(1) AS total FROM product WHERE title ILIKE :keyword")
                .bind("keyword", wrapKeyword(keyword))
                .map((row, metadata) -> row.get("total", Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    @Override
        public Flux<ProductEntity> recommendProductPageByQueryEmbedding(String queryEmbedding, int limit) {
        String sql = """
                SELECT p.id, p.category, p.title, p.price, p.sales, p.image_url, p.shop_name
                FROM product p
                WHERE p.embedding IS NOT NULL
                                ORDER BY p.embedding <=> CAST(:queryEmbedding AS vector)
                LIMIT :limit
                """;

        return databaseClient.sql(sql)
                                .bind("queryEmbedding", queryEmbedding)
                .bind("limit", limit)
                .map((row, metadata) -> ProductEntity.builder()
                        .id(row.get("id", Long.class))
                        .category(row.get("category", String.class))
                        .title(row.get("title", String.class))
                        .price(row.get("price", java.math.BigDecimal.class))
                        .sales(row.get("sales", String.class))
                        .imageUrl(row.get("image_url", String.class))
                        .shopName(row.get("shop_name", String.class))
                        .build())
                .all();
    }

    private String wrapKeyword(String keyword) {
        return "%" + keyword.trim() + "%";
    }
}
