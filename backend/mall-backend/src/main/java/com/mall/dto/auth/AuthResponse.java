package com.mall.dto.auth;

public record AuthResponse(
        String token,
        UserProfileDto user
) {
}
