package com.mall.entity.chat;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("chat_message_history")
public class ChatMessageHistoryEntity {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("session_id")
    private String sessionId;

    @Column("message_role")
    private String messageRole;

    @Column("content")
    private String content;

    @Column("created_at")
    private OffsetDateTime createdAt;
}
