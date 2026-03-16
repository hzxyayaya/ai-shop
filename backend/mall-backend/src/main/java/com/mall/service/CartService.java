package com.mall.service;

import com.mall.model.AuthenticatedUser;
import com.mall.dto.cart.AddCartItemRequest;
import com.mall.dto.cart.CartDto;
import com.mall.dto.cart.UpdateCartCheckedRequest;
import com.mall.dto.cart.UpdateCartQuantityRequest;
import com.mall.entity.cart.CartItemEntity;
import com.mall.model.cart.CartItemView;
import com.mall.repository.cart.CartRepository;
import com.mall.common.exception.BusinessException;
import com.mall.common.exception.UnauthorizedException;
import com.mall.repository.product.ProductRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    public Mono<Long> addToCart(AuthenticatedUser currentUser, AddCartItemRequest request) {
        Long userId = requireUserId(currentUser);
        validateProductId(request.productId());
        validateQuantity(request.quantity());

        return productRepository.findProjectedById(request.productId())
                .switchIfEmpty(Mono.error(new BusinessException(404, "resource not found")))
                .flatMap(product -> cartRepository.findByUserIdAndProductId(userId, request.productId())
                        .flatMap(existing -> {
                            existing.setQuantity(existing.getQuantity() + request.quantity());
                            existing.setUpdatedAt(OffsetDateTime.now());
                            return cartRepository.save(existing);
                        })
                        .switchIfEmpty(Mono.defer(() -> {
                            OffsetDateTime now = OffsetDateTime.now();
                            CartItemEntity cartItem = CartItemEntity.builder()
                                    .userId(userId)
                                    .productId(request.productId())
                                    .quantity(request.quantity())
                                    .checked(Boolean.TRUE)
                                    .createdAt(now)
                                    .updatedAt(now)
                                    .build();
                            return cartRepository.save(cartItem);
                        })))
                .map(CartItemEntity::getId);
    }

    public Flux<CartDto> getCartItems(AuthenticatedUser currentUser) {
        Long userId = requireUserId(currentUser);
        return cartRepository.findCartItemsByUserId(userId).map(this::toDto);
    }

    public Mono<Void> updateQuantity(AuthenticatedUser currentUser, Long cartItemId, UpdateCartQuantityRequest request) {
        Long userId = requireUserId(currentUser);
        validateCartItemId(cartItemId);
        validateQuantity(request.quantity());

        return cartRepository.findByIdAndUserId(cartItemId, userId)
                .switchIfEmpty(Mono.error(new BusinessException(404, "resource not found")))
                .flatMap(entity -> {
                    entity.setQuantity(request.quantity());
                    entity.setUpdatedAt(OffsetDateTime.now());
                    return cartRepository.save(entity);
                })
                .then();
    }

    public Mono<Void> updateChecked(AuthenticatedUser currentUser, Long cartItemId, UpdateCartCheckedRequest request) {
        Long userId = requireUserId(currentUser);
        validateCartItemId(cartItemId);
        if (request.checked() == null) {
            return Mono.error(new BusinessException(400, "checked is required"));
        }

        return cartRepository.findByIdAndUserId(cartItemId, userId)
                .switchIfEmpty(Mono.error(new BusinessException(404, "resource not found")))
                .flatMap(entity -> {
                    entity.setChecked(request.checked());
                    entity.setUpdatedAt(OffsetDateTime.now());
                    return cartRepository.save(entity);
                })
                .then();
    }

    public Mono<Void> deleteCartItem(AuthenticatedUser currentUser, Long cartItemId) {
        Long userId = requireUserId(currentUser);
        validateCartItemId(cartItemId);

        return cartRepository.findByIdAndUserId(cartItemId, userId)
                .switchIfEmpty(Mono.error(new BusinessException(404, "resource not found")))
                .flatMap(cartRepository::delete);
    }

    private Long requireUserId(AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.id() == null) {
            throw new UnauthorizedException("unauthorized");
        }
        return currentUser.id();
    }

    private void validateProductId(Long productId) {
        if (productId == null || productId <= 0) {
            throw new BusinessException(400, "productId must be greater than 0");
        }
    }

    private void validateCartItemId(Long cartItemId) {
        if (cartItemId == null || cartItemId <= 0) {
            throw new BusinessException(400, "id must be greater than 0");
        }
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new BusinessException(400, "quantity must be greater than or equal to 1");
        }
    }

    private CartDto toDto(CartItemView itemView) {
        return new CartDto(
                itemView.getId(),
                itemView.getProductId(),
                itemView.getTitle(),
                itemView.getCategory(),
                itemView.getPrice(),
                itemView.getSales(),
                itemView.getImageUrl(),
                itemView.getShopName(),
                itemView.getQuantity(),
                Boolean.TRUE.equals(itemView.getChecked())
        );
    }
}
