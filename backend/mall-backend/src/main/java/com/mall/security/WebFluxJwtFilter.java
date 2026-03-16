package com.mall.security;

import com.mall.model.AuthenticatedUser;
import com.mall.common.exception.UnauthorizedException;
import com.mall.common.response.ApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.entity.user.UserEntity;
import com.mall.repository.user.UserRepository;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebFluxJwtFilter implements WebFilter {

    public static final String AUTHENTICATED_USER_ATTR = "authenticatedUser";

    private static final String BEARER_PREFIX = "Bearer ";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final List<String> PUBLIC_PATTERNS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/products",
            "/api/products/search",
            "/api/products/*",
            "/api/payments/callback/alipay"
    );

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public WebFluxJwtFilter(JWTUtil jwtUtil, UserRepository userRepository, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        if (!path.startsWith("/api/") || isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return writeUnauthorizedResponse(exchange);
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            return writeUnauthorizedResponse(exchange);
        }

        Long userId;
        try {
            userId = jwtUtil.parseUserId(token);
        } catch (JwtException | IllegalArgumentException ex) {
            return writeUnauthorizedResponse(exchange);
        }

        return userRepository.findById(userId)
                .switchIfEmpty(Mono.defer(() -> writeUnauthorizedResponse(exchange).then(Mono.empty())))
                .flatMap(user -> {
                    if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                        return writeUnauthorizedResponse(exchange);
                    }
                    exchange.getAttributes().put(AUTHENTICATED_USER_ATTR, toAuthenticatedUser(user));
                    return chain.filter(exchange);
                });
    }

    private boolean isPublicPath(String path) {
        PathContainer.parsePath(path);
        return PUBLIC_PATTERNS.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    private AuthenticatedUser toAuthenticatedUser(UserEntity user) {
        return new AuthenticatedUser(user.getId(), user.getUsername(), user.getEmail(), user.getNickname());
    }

    private Mono<Void> writeUnauthorizedResponse(ServerWebExchange exchange) {
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(ApiResponse.fail(401, "unauthorized"));
        } catch (JsonProcessingException ex) {
            bytes = "{\"code\":401,\"message\":\"unauthorized\",\"data\":null}".getBytes(StandardCharsets.UTF_8);
        }

        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
