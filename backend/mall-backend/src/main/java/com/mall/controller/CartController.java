package com.mall.controller;

import com.mall.model.AuthenticatedUser;
import com.mall.security.WebFluxJwtFilter;
import com.mall.dto.cart.AddCartItemRequest;
import com.mall.dto.cart.CartDto;
import com.mall.dto.cart.UpdateCartCheckedRequest;
import com.mall.dto.cart.UpdateCartQuantityRequest;
import com.mall.service.CartService;
import com.mall.common.exception.UnauthorizedException;
import com.mall.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/add")
    public Mono<ApiResponse<Map<String, Long>>> addToCart(
            @Valid @RequestBody AddCartItemRequest request,
            ServerWebExchange exchange
    ) {
        return cartService.addToCart(requireUser(exchange), request)
                .map(cartItemId -> ApiResponse.success("add to cart success", Map.of("cartItemId", cartItemId)));
    }

    @GetMapping
    public Mono<ApiResponse<java.util.List<CartDto>>> getCart(ServerWebExchange exchange) {
        Flux<CartDto> items = cartService.getCartItems(requireUser(exchange));
        return items.collectList().map(ApiResponse::success);
    }

    @PutMapping("/{id}/quantity")
    public Mono<ApiResponse<Void>> updateQuantity(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCartQuantityRequest request,
            ServerWebExchange exchange
    ) {
        return cartService.updateQuantity(requireUser(exchange), id, request)
                .thenReturn(ApiResponse.success("cart quantity updated", null));
    }

    @PutMapping("/{id}/checked")
    public Mono<ApiResponse<Void>> updateChecked(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCartCheckedRequest request,
            ServerWebExchange exchange
    ) {
        return cartService.updateChecked(requireUser(exchange), id, request)
                .thenReturn(ApiResponse.success("cart checked status updated", null));
    }

    @DeleteMapping("/{id}")
    public Mono<ApiResponse<Void>> deleteCartItem(@PathVariable Long id, ServerWebExchange exchange) {
        return cartService.deleteCartItem(requireUser(exchange), id)
                .thenReturn(ApiResponse.success("cart item deleted", null));
    }

    private AuthenticatedUser requireUser(ServerWebExchange exchange) {
        AuthenticatedUser authenticatedUser = exchange.getAttribute(WebFluxJwtFilter.AUTHENTICATED_USER_ATTR);
        if (authenticatedUser == null) {
            throw new UnauthorizedException("unauthorized");
        }
        return authenticatedUser;
    }
}
