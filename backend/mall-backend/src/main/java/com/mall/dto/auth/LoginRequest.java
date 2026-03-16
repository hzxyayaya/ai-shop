package com.mall.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "account must not be blank")
        String account,
        @NotBlank(message = "password must not be blank")
        String password
) {
}
