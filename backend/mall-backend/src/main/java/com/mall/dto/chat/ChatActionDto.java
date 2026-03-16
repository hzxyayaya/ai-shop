package com.mall.dto.chat;

public record ChatActionDto(
        String type,
        String label,
        String targetId
) {
}
