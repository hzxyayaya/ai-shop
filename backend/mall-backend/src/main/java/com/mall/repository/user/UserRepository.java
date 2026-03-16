package com.mall.repository.user;

import com.mall.entity.user.UserEntity;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<UserEntity, Long> {

    Mono<Boolean> existsByUsername(String username);

    Mono<Boolean> existsByEmail(String email);

    Mono<UserEntity> findByUsername(String username);

    Mono<UserEntity> findByEmail(String email);

    @Query("""
            SELECT id, username, email, password_hash, nickname, status, created_at, updated_at
            FROM user_account
            WHERE username = :account OR email = :email
            LIMIT 1
            """)
    Mono<UserEntity> findByAccount(String account, String email);
}
