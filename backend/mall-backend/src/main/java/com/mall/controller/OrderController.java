package com.mall.controller;

import com.mall.model.AuthenticatedUser;
import com.mall.security.WebFluxJwtFilter;
import com.mall.common.exception.UnauthorizedException;
import com.mall.common.response.ApiResponse;
import com.mall.dto.order.BuyNowOrderRequest;
import com.mall.dto.order.CheckoutOrderRequest;
import com.mall.dto.order.OrderDto;
import com.mall.dto.order.OrderListResponse;
import com.mall.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/buy-now")
    public Mono<ApiResponse<OrderDto>> buyNow(
            @Valid @RequestBody BuyNowOrderRequest request,
            ServerWebExchange exchange
    ) {
        return orderService.createBuyNowOrder(requireUser(exchange), request)
                .map(data -> ApiResponse.success("order created", data));
    }

    @PostMapping("/checkout")
    public Mono<ApiResponse<OrderDto>> checkout(
            @Valid @RequestBody CheckoutOrderRequest request,
            ServerWebExchange exchange
    ) {
        return orderService.createCheckoutOrder(requireUser(exchange), request)
                .map(data -> ApiResponse.success("order created", data));
    }

    @GetMapping
    public Mono<ApiResponse<OrderListResponse>> getOrders(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String payStatus,
            ServerWebExchange exchange
    ) {
        return orderService.getOrders(requireUser(exchange), page, pageSize, status, payStatus)
                .map(ApiResponse::success);
    }

    @GetMapping("/{orderNo}")
    public Mono<ApiResponse<OrderDto>> getOrderDetail(@PathVariable String orderNo, ServerWebExchange exchange) {
        return orderService.getOrderDetail(requireUser(exchange), orderNo)
                .map(ApiResponse::success);
    }

    @DeleteMapping("/{orderNo}")
    public Mono<ApiResponse<Void>> deleteOrder(@PathVariable String orderNo, ServerWebExchange exchange) {
        return orderService.deleteOrder(requireUser(exchange), orderNo)
                .thenReturn(ApiResponse.success("order deleted", null));
    }

    private AuthenticatedUser requireUser(ServerWebExchange exchange) {
        AuthenticatedUser authenticatedUser = exchange.getAttribute(WebFluxJwtFilter.AUTHENTICATED_USER_ATTR);
        if (authenticatedUser == null) {
            throw new UnauthorizedException("unauthorized");
        }
        return authenticatedUser;
    }
}
