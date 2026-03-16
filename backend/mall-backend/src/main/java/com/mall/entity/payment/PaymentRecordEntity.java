package com.mall.entity.payment;

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
@Table("payment_record")
public class PaymentRecordEntity {

    @Id
    private Long id;

    @Column("order_no")
    private String orderNo;

    @Column("user_id")
    private Long userId;

    @Column("pay_amount")
    private BigDecimal payAmount;

    @Column("pay_status")
    private String payStatus;

    @Column("alipay_trade_no")
    private String alipayTradeNo;

    @Column("callback_content")
    private String callbackContent;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("updated_at")
    private OffsetDateTime updatedAt;
}
