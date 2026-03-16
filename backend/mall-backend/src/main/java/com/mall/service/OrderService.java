package com.mall.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.model.AuthenticatedUser;
import com.mall.model.cart.CartItemView;
import com.mall.repository.cart.CartRepository;
import com.mall.common.exception.BusinessException;
import com.mall.common.exception.UnauthorizedException;
import com.mall.dto.order.BuyNowOrderRequest;
import com.mall.dto.order.CheckoutOrderRequest;
import com.mall.dto.order.OrderDto;
import com.mall.dto.order.OrderItemDto;
import com.mall.dto.order.OrderListResponse;
import com.mall.entity.order.OrderEntity;
import com.mall.repository.order.OrderRepository;
import com.mall.entity.product.ProductEntity;
import com.mall.repository.payment.PaymentRecordRepository;
import com.mall.repository.product.ProductRepository;
import io.r2dbc.postgresql.codec.Json;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderService {

    private static final TypeReference<List<OrderItemDto>> ORDER_ITEM_LIST_TYPE = new TypeReference<>() {};

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final ObjectMapper objectMapper;

    public OrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            CartRepository cartRepository,
            PaymentRecordRepository paymentRecordRepository,
            ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.objectMapper = objectMapper;
    }

    public Mono<OrderDto> createBuyNowOrder(AuthenticatedUser currentUser, BuyNowOrderRequest request) {
        Long userId = requireUserId(currentUser);
        validateProductId(request.productId());
        validateQuantity(request.quantity());

        return productRepository.findProjectedById(request.productId())
                .switchIfEmpty(Mono.error(new BusinessException(404, "resource not found")))
                .map(product -> buildOrderItem(product, request.quantity()))
                .flatMap(items -> saveOrder(userId, List.of(items)));
    }

    public Mono<OrderDto> createCheckoutOrder(AuthenticatedUser currentUser, CheckoutOrderRequest request) {
        Long userId = requireUserId(currentUser);
        if (request.cartItemIds() == null || request.cartItemIds().isEmpty()) {
            return Mono.error(new BusinessException(400, "cartItemIds must not be empty"));
        }

        Set<Long> requestedIds = new HashSet<>(request.cartItemIds());
        return cartRepository.findCartItemsByUserId(userId)
                .filter(item -> requestedIds.contains(item.getId()))
                .collectList()
                .flatMap(items -> {
                    if (items.isEmpty() || items.size() != requestedIds.size()) {
                        return Mono.error(new BusinessException(404, "resource not found"));
                    }

                    List<OrderItemDto> orderItems = items.stream()
                            .map(this::buildOrderItem)
                            .toList();
                    return saveOrder(userId, orderItems)
                            .flatMap(savedOrder -> Flux.fromIterable(items)
                                    .flatMap(cartItem -> cartRepository.deleteById(cartItem.getId()))
                                    .then(Mono.just(savedOrder)));
                });
    }

    public Mono<OrderListResponse> getOrders(AuthenticatedUser currentUser, Integer page, Integer pageSize, String status, String payStatus) {
        Long userId = requireUserId(currentUser);
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        long offset = (long) (normalizedPage - 1) * normalizedPageSize;

        Mono<Long> totalMono = orderRepository.countOrders(userId, status, payStatus);
        Mono<List<OrderDto>> listMono = orderRepository.findOrderPage(userId, status, payStatus, normalizedPageSize, offset)
                .map(this::toOrderDto)
                .collectList();

        return Mono.zip(totalMono, listMono)
                .map(tuple -> new OrderListResponse(normalizedPage, normalizedPageSize, tuple.getT1(), tuple.getT2()));
    }

    public Mono<OrderDto> getOrderDetail(AuthenticatedUser currentUser, String orderNo) {
        Long userId = requireUserId(currentUser);
        if (orderNo == null || orderNo.isBlank()) {
            return Mono.error(new BusinessException(400, "orderNo is required"));
        }
        return orderRepository.findByOrderNoAndUserId(orderNo, userId)
                .switchIfEmpty(Mono.error(new BusinessException(404, "resource not found")))
                .map(this::toOrderDto);
    }

    public Mono<Void> deleteOrder(AuthenticatedUser currentUser, String orderNo) {
        Long userId = requireUserId(currentUser);
        if (orderNo == null || orderNo.isBlank()) {
            return Mono.error(new BusinessException(400, "orderNo is required"));
        }
        return orderRepository.findByOrderNoAndUserId(orderNo, userId)
                .flatMap(order -> paymentRecordRepository.deleteByOrderNoAndUserId(orderNo, userId)
                        .onErrorResume(ex -> Mono.empty())
                        .then(orderRepository.delete(order)))
                .switchIfEmpty(Mono.empty());
    }

    private Mono<OrderDto> saveOrder(Long userId, List<OrderItemDto> items) {
        OffsetDateTime now = OffsetDateTime.now();
        OrderEntity order = OrderEntity.builder()
                .orderNo(generateOrderNo())
                .userId(userId)
                .itemsJson(Json.of(writeItemsJson(items).getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .totalAmount(sumAmount(items))
                .status("CREATED")
                .payStatus("UNPAID")
                .createdAt(now)
                .updatedAt(now)
                .build();
        return orderRepository.save(order).map(saved -> toOrderDto(saved, items));
    }

    private OrderItemDto buildOrderItem(ProductEntity product, Integer quantity) {
        BigDecimal amount = product.getPrice()
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);
        return new OrderItemDto(
                product.getId(),
                product.getTitle(),
                product.getCategory(),
                product.getPrice(),
                quantity,
                amount,
                product.getImageUrl(),
                product.getShopName()
        );
    }

    private OrderItemDto buildOrderItem(CartItemView cartItemView) {
        BigDecimal amount = cartItemView.getPrice()
                .multiply(BigDecimal.valueOf(cartItemView.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
        return new OrderItemDto(
                cartItemView.getProductId(),
                cartItemView.getTitle(),
                cartItemView.getCategory(),
                cartItemView.getPrice(),
                cartItemView.getQuantity(),
                amount,
                cartItemView.getImageUrl(),
                cartItemView.getShopName()
        );
    }

    private BigDecimal sumAmount(List<OrderItemDto> items) {
        return items.stream()
                .map(OrderItemDto::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String writeItemsJson(List<OrderItemDto> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500, "internal server error");
        }
    }

    private List<OrderItemDto> readItemsJson(String itemsJson) {
        try {
            return objectMapper.readValue(itemsJson, ORDER_ITEM_LIST_TYPE);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500, "internal server error");
        }
    }

    private OrderDto toOrderDto(OrderEntity order) {
        String jsonStr = "[]";
        if (order.getItemsJson() != null) {
            jsonStr = new String(order.getItemsJson().asArray(), java.nio.charset.StandardCharsets.UTF_8);
        }
        return toOrderDto(order, readItemsJson(jsonStr));
    }

    private OrderDto toOrderDto(OrderEntity order, List<OrderItemDto> items) {
        return new OrderDto(
                order.getOrderNo(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getPayStatus(),
                order.getCreatedAt(),
                items
        );
    }

    private Long requireUserId(AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.id() == null) {
            throw new UnauthorizedException("unauthorized");
        }
        return currentUser.id();
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return 1;
        }
        if (page < 1) {
            throw new BusinessException(400, "page must be greater than or equal to 1");
        }
        return page;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return 10;
        }
        if (pageSize < 1) {
            throw new BusinessException(400, "pageSize must be greater than or equal to 1");
        }
        if (pageSize > 100) {
            throw new BusinessException(400, "pageSize must be less than or equal to 100");
        }
        return pageSize;
    }

    private void validateProductId(Long productId) {
        if (productId == null || productId <= 0) {
            throw new BusinessException(400, "productId must be greater than 0");
        }
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new BusinessException(400, "quantity must be greater than or equal to 1");
        }
    }

    private String generateOrderNo() {
        String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        int random = ThreadLocalRandom.current().nextInt(100, 1000);
        return "ORD" + timestamp + random;
    }
}
