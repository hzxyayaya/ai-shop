package com.mall.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "username must not be blank")
        String username,
        @NotBlank(message = "email must not be blank")
        @Email(message = "email format is invalid")
        String email,
        @NotBlank(message = "password must not be blank")
        @Size(min = 8, message = "password must be at least 8 characters")
        String password
) {
}
