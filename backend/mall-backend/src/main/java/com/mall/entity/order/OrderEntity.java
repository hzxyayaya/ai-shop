package com.mall.entity.order;

import io.r2dbc.postgresql.codec.Json;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
@Table("order_info")
public class OrderEntity {

    @Id
    private Long id;

    @Column("order_no")
    private String orderNo;

    @Column("user_id")
    private Long userId;

    @Column("items_json")
    private Json itemsJson;

    @Column("total_amount")
    private BigDecimal totalAmount;

    private String status;

    @Column("pay_status")
    private String payStatus;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("updated_at")
    private OffsetDateTime updatedAt;
}
