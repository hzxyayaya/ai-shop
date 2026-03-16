package com.mall.controller;

import com.mall.dto.auth.AuthResponse;
import com.mall.dto.auth.LoginRequest;
import com.mall.dto.auth.RegisterRequest;
import com.mall.dto.auth.UserProfileDto;
import com.mall.model.AuthenticatedUser;
import com.mall.security.WebFluxJwtFilter;
import com.mall.service.AuthService;
import com.mall.common.exception.UnauthorizedException;
import com.mall.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Mono<ApiResponse<UserProfileDto>> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request)
                .map(data -> ApiResponse.success("register success", data));
    }

    @PostMapping("/login")
    public Mono<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request)
                .map(data -> ApiResponse.success("login success", data));
    }

    @GetMapping("/me")
    public Mono<ApiResponse<UserProfileDto>> me(ServerWebExchange exchange) {
        AuthenticatedUser authenticatedUser = exchange.getAttribute(WebFluxJwtFilter.AUTHENTICATED_USER_ATTR);
        if (authenticatedUser == null) {
            return Mono.error(new UnauthorizedException("unauthorized"));
        }
        return authService.getCurrentUser(authenticatedUser)
                .map(ApiResponse::success);
    }
}
