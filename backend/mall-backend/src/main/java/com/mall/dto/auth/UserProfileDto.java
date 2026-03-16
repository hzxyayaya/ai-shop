package com.mall.dto.auth;

public record UserProfileDto(
        Long id,
        String username,
        String email,
        String nickname
) {
}
