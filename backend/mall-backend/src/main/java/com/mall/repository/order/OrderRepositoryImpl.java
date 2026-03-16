package com.mall.repository.order;

import com.mall.entity.order.OrderEntity;

import io.r2dbc.postgresql.codec.Json;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class OrderRepositoryImpl implements OrderRepositoryCustom {

    private static final String ORDER_COLUMNS = """
            SELECT id, order_no, user_id, items_json, total_amount, status, pay_status, created_at, updated_at
            FROM order_info
            WHERE user_id = :userId
            """;

    private final DatabaseClient databaseClient;

    public OrderRepositoryImpl(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Flux<OrderEntity> findOrderPage(Long userId, String status, String payStatus, int limit, long offset) {
        StringBuilder sql = new StringBuilder(ORDER_COLUMNS);
        if (StringUtils.hasText(status)) {
            sql.append(" AND status = :status");
        }
        if (StringUtils.hasText(payStatus)) {
            sql.append(" AND pay_status = :payStatus");
        }
        sql.append(" ORDER BY created_at DESC, id DESC LIMIT :limit OFFSET :offset");

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString())
                .bind("userId", userId)
                .bind("limit", limit)
                .bind("offset", offset);
        if (StringUtils.hasText(status)) {
            spec = spec.bind("status", status);
        }
        if (StringUtils.hasText(payStatus)) {
            spec = spec.bind("payStatus", payStatus);
        }
        return spec.map((row, metadata) -> mapOrder(row)).all();
    }

    @Override
    public Mono<Long> countOrders(Long userId, String status, String payStatus) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(1) AS total FROM order_info WHERE user_id = :userId");
        if (StringUtils.hasText(status)) {
            sql.append(" AND status = :status");
        }
        if (StringUtils.hasText(payStatus)) {
            sql.append(" AND pay_status = :payStatus");
        }

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString()).bind("userId", userId);
        if (StringUtils.hasText(status)) {
            spec = spec.bind("status", status);
        }
        if (StringUtils.hasText(payStatus)) {
            spec = spec.bind("payStatus", payStatus);
        }
        return spec.map((row, metadata) -> row.get("total", Long.class)).one().defaultIfEmpty(0L);
    }

    private OrderEntity mapOrder(io.r2dbc.spi.Readable row) {
        return OrderEntity.builder()
                .id(row.get("id", Long.class))
                .orderNo(row.get("order_no", String.class))
                .userId(row.get("user_id", Long.class))
                .itemsJson(row.get("items_json", Json.class))
                .totalAmount(row.get("total_amount", BigDecimal.class))
                .status(row.get("status", String.class))
                .payStatus(row.get("pay_status", String.class))
                .createdAt(row.get("created_at", OffsetDateTime.class))
                .updatedAt(row.get("updated_at", OffsetDateTime.class))
                .build();
    }
}
