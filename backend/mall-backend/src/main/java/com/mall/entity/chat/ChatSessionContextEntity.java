package com.mall.entity.chat;

import io.r2dbc.postgresql.codec.Json;
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
@Table("chat_session_context")
public class ChatSessionContextEntity {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("session_id")
    private String sessionId;

    @Column("last_intent")
    private String lastIntent;

    @Column("last_user_message")
    private String lastUserMessage;

    @Column("last_product_ids_json")
    private Json lastProductIdsJson;

    @Column("last_order_no")
    private String lastOrderNo;

    @Column("updated_at")
    private OffsetDateTime updatedAt;
}
