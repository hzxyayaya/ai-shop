package com.mall.service;

import com.mall.dto.auth.AuthResponse;
import com.mall.dto.auth.LoginRequest;
import com.mall.dto.auth.RegisterRequest;
import com.mall.dto.auth.UserProfileDto;
import com.mall.model.AuthenticatedUser;
import com.mall.security.JWTUtil;
import com.mall.common.exception.BusinessException;
import com.mall.common.exception.UnauthorizedException;
import com.mall.entity.user.UserEntity;
import com.mall.repository.user.UserRepository;
import java.time.OffsetDateTime;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JWTUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, JWTUtil jwtUtil, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    public Mono<UserProfileDto> register(RegisterRequest request) {
        return validateRegisterRequest(request)
                .then(checkDuplicateUsername(request.username()))
                .then(checkDuplicateEmail(request.email()))
                .then(Mono.defer(() -> {
                    OffsetDateTime now = OffsetDateTime.now();
                    UserEntity user = UserEntity.builder()
                            .username(request.username().trim())
                            .email(request.email().trim().toLowerCase())
                            .passwordHash(passwordEncoder.encode(request.password()))
                            .nickname(resolveNickname(request.username()))
                            .status("ACTIVE")
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    return userRepository.save(user).map(this::toUserProfile);
                }));
    }

    public Mono<AuthResponse> login(LoginRequest request) {
        if (!StringUtils.hasText(request.account()) || !StringUtils.hasText(request.password())) {
            return Mono.error(new BusinessException(400, "account and password are required"));
        }

        String normalizedAccount = request.account().trim();
        return userRepository.findByAccount(normalizedAccount, normalizedAccount.toLowerCase())
                .switchIfEmpty(Mono.error(new UnauthorizedException("unauthorized")))
                .flatMap(user -> {
                    if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                        return Mono.error(new UnauthorizedException("unauthorized"));
                    }
                    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                        return Mono.error(new UnauthorizedException("unauthorized"));
                    }
                    String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getEmail());
                    return Mono.just(new AuthResponse(token, toUserProfile(user)));
                });
    }

    public Mono<UserProfileDto> getCurrentUser(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.id() == null) {
            return Mono.error(new UnauthorizedException("unauthorized"));
        }
        return userRepository.findById(authenticatedUser.id())
                .switchIfEmpty(Mono.error(new UnauthorizedException("unauthorized")))
                .map(this::toUserProfile);
    }

    private Mono<Void> validateRegisterRequest(RegisterRequest request) {
        if (!StringUtils.hasText(request.username())
                || !StringUtils.hasText(request.email())
                || !StringUtils.hasText(request.password())) {
            return Mono.error(new BusinessException(400, "username, email and password are required"));
        }
        if (request.password().length() < 8) {
            return Mono.error(new BusinessException(400, "password must be at least 8 characters"));
        }
        return Mono.empty();
    }

    private Mono<Void> checkDuplicateUsername(String username) {
        return userRepository.existsByUsername(username.trim())
                .flatMap(exists -> exists
                        ? Mono.error(new BusinessException(400, "username already exists"))
                        : Mono.empty());
    }

    private Mono<Void> checkDuplicateEmail(String email) {
        return userRepository.existsByEmail(email.trim().toLowerCase())
                .flatMap(exists -> exists
                        ? Mono.error(new BusinessException(400, "email already exists"))
                        : Mono.empty());
    }

    private String resolveNickname(String username) {
        return username.trim();
    }

    private UserProfileDto toUserProfile(UserEntity user) {
        return new UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getNickname() == null ? "" : user.getNickname()
        );
    }
}
