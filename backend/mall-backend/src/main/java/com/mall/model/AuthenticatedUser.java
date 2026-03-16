package com.mall.model;

public record AuthenticatedUser(
        Long id,
        String username,
        String email,
        String nickname
) {
}
