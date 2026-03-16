package com.mall.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.model.AuthenticatedUser;
import com.mall.common.exception.BusinessException;
import com.mall.common.exception.UnauthorizedException;
import com.mall.config.AlipayProperties;
import com.mall.entity.order.OrderEntity;
import com.mall.repository.order.OrderRepository;
import com.mall.client.AlipayGatewayClient;
import com.mall.entity.payment.PaymentRecordEntity;
import com.mall.repository.payment.PaymentRecordRepository;
import com.mall.dto.payment.CreatePaymentRequest;
import com.mall.dto.payment.PaymentCreateResponse;
import com.mall.dto.payment.PaymentStatusResponse;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRecordRepository paymentRecordRepository;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final AlipayProperties alipayProperties;
    private final AlipayGatewayClient alipayGatewayClient;

    public PaymentService(
            PaymentRecordRepository paymentRecordRepository,
            OrderRepository orderRepository,
            ObjectMapper objectMapper,
            AlipayProperties alipayProperties,
            AlipayGatewayClient alipayGatewayClient
    ) {
        this.paymentRecordRepository = paymentRecordRepository;
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
        this.alipayProperties = alipayProperties;
        this.alipayGatewayClient = alipayGatewayClient;
    }

    public Mono<PaymentCreateResponse> createPayment(
            AuthenticatedUser currentUser,
            CreatePaymentRequest request,
            ServerWebExchange exchange
    ) {
        Long userId = requireUserId(currentUser);
        if (!StringUtils.hasText(request.orderNo())) {
            return Mono.error(new BusinessException(400, "orderNo is required"));
        }

        String resolvedReturnUrl = resolveReturnUrl(exchange);
        log.info("Creating payment: userId={}, orderNo={}", userId, request.orderNo());
        return orderRepository.findByOrderNoAndUserId(request.orderNo(), userId)
                .switchIfEmpty(Mono.error(new BusinessException(404, "resource not found")))
                .flatMap(order -> {
                    if ("PAID".equalsIgnoreCase(order.getPayStatus())) {
                        return Mono.error(new BusinessException(400, "order already paid"));
                    }
                    log.info(
                            "Using Alipay config for payment: orderNo={}, appId={}, notifyUrl={}, returnUrl={}",
                            order.getOrderNo(),
                            alipayProperties.appId(),
                            alipayProperties.notifyUrl(),
                            resolvedReturnUrl
                    );
                    return Mono.fromCallable(() -> alipayGatewayClient.createPagePayForm(order, resolvedReturnUrl))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(payForm -> upsertPaymentRecord(order)
                            .map(paymentRecord -> new PaymentCreateResponse(
                                    order.getOrderNo(),
                                    "ALIPAY_PC",
                                    payForm
                            )))
                            .doOnSuccess(response -> log.info(
                                    "Payment form created successfully: orderNo={}, totalAmount={}, payStatus={}",
                                    order.getOrderNo(),
                                    order.getTotalAmount(),
                                    order.getPayStatus()
                            ));
                });
    }

    public Mono<Void> handleAlipayCallback(MultiValueMap<String, String> callbackForm) {
        String orderNo = firstValue(callbackForm, "out_trade_no");
        if (!StringUtils.hasText(orderNo)) {
            return Mono.error(new BusinessException(400, "out_trade_no is required"));
        }

        String tradeStatus = firstValue(callbackForm, "trade_status");
        String alipayTradeNo = firstValue(callbackForm, "trade_no");
        String totalAmount = firstValue(callbackForm, "total_amount");
        String payStatus = mapTradeStatus(tradeStatus);
        String orderStatus = "PAID".equals(payStatus) ? "PAID" : "CREATED";

        log.info(
                "Start handling Alipay callback: orderNo={}, tradeNo={}, tradeStatus={}, mappedPayStatus={}, totalAmount={}",
                orderNo,
                alipayTradeNo,
                tradeStatus,
                payStatus,
                totalAmount
        );
        return Mono.fromCallable(() -> alipayGatewayClient.verifyCallback(callbackForm))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(verified -> {
                    if (!verified) {
                        return Mono.error(new BusinessException(400, "alipay callback signature verification failed"));
                    }
                    log.info("Alipay callback signature verified: orderNo={}", orderNo);
                    return orderRepository.findByOrderNo(orderNo);
                })
                .switchIfEmpty(Mono.error(new BusinessException(404, "resource not found")))
                .flatMap(order -> {
                    log.info(
                            "Matched order for callback: orderNo={}, currentOrderStatus={}, currentPayStatus={}, totalAmount={}",
                            order.getOrderNo(),
                            order.getStatus(),
                            order.getPayStatus(),
                            order.getTotalAmount()
                    );
                    validateCallbackMatchesOrder(callbackForm, order, totalAmount);
                    return paymentRecordRepository.findFirstByOrderNoOrderByUpdatedAtDesc(orderNo)
                            .defaultIfEmpty(new PaymentRecordEntity())
                            .flatMap(record -> {
                                OffsetDateTime now = OffsetDateTime.now();
                                if (record.getId() == null) {
                                    record.setOrderNo(orderNo);
                                    record.setUserId(order.getUserId());
                                    record.setCreatedAt(now);
                                }
                                record.setPayAmount(parseAmount(totalAmount, order.getTotalAmount()));
                                record.setPayStatus(payStatus);
                                record.setAlipayTradeNo(alipayTradeNo);
                                record.setCallbackContent(writeCallbackContent(callbackForm));
                                record.setUpdatedAt(now);
                                return paymentRecordRepository.save(record);
                            })
                            .flatMap(savedRecord -> {
                                log.info(
                                        "Payment record updated from callback: orderNo={}, recordId={}, payStatus={}, alipayTradeNo={}",
                                        orderNo,
                                        savedRecord.getId(),
                                        savedRecord.getPayStatus(),
                                        savedRecord.getAlipayTradeNo()
                                );
                                order.setPayStatus(payStatus);
                                order.setStatus(orderStatus);
                                order.setUpdatedAt(OffsetDateTime.now());
                                return orderRepository.save(order)
                                        .doOnSuccess(savedOrder -> log.info(
                                                "Order status updated from callback: orderNo={}, status={}, payStatus={}",
                                                savedOrder.getOrderNo(),
                                                savedOrder.getStatus(),
                                                savedOrder.getPayStatus()
                                        ))
                                        .then();
                            });
                })
                .doOnError(ex -> log.warn("Failed while processing Alipay callback: orderNo={}, reason={}", orderNo, ex.getMessage()));
    }

    private String resolveReturnUrl(ServerWebExchange exchange) {
        if (exchange == null) {
            return alipayProperties.returnUrl();
        }

        String originHeader = exchange.getRequest().getHeaders().getOrigin();
        if (StringUtils.hasText(originHeader)) {
            return appendPaymentResultPath(originHeader);
        }

        String refererHeader = exchange.getRequest().getHeaders().getFirst("Referer");
        if (!StringUtils.hasText(refererHeader)) {
            return alipayProperties.returnUrl();
        }

        try {
            URI refererUri = new URI(refererHeader);
            String origin = refererUri.getScheme() + "://" + refererUri.getAuthority();
            return appendPaymentResultPath(origin);
        } catch (URISyntaxException ex) {
            log.warn("Failed to parse Referer header for payment returnUrl: referer={}", refererHeader);
            return alipayProperties.returnUrl();
        }
    }

    private String appendPaymentResultPath(String origin) {
        String normalizedOrigin = origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
        return normalizedOrigin + "/payment/result";
    }

    public Mono<PaymentStatusResponse> getPaymentStatus(AuthenticatedUser currentUser, String orderNo) {
        Long userId = requireUserId(currentUser);
        if (!StringUtils.hasText(orderNo)) {
            return Mono.error(new BusinessException(400, "orderNo is required"));
        }

        return orderRepository.findByOrderNoAndUserId(orderNo, userId)
                .switchIfEmpty(Mono.error(new BusinessException(404, "resource not found")))
                .flatMap(order -> paymentRecordRepository.findFirstByOrderNoOrderByUpdatedAtDesc(orderNo)
                        .defaultIfEmpty(PaymentRecordEntity.builder().build())
                        .flatMap(record -> resolvePaymentStatus(order, record)
                                .map(payStatus -> new PaymentStatusResponse(orderNo, payStatus))));
    }

    private Mono<PaymentRecordEntity> upsertPaymentRecord(OrderEntity order) {
        return paymentRecordRepository.findFirstByOrderNoOrderByUpdatedAtDesc(order.getOrderNo())
                .defaultIfEmpty(PaymentRecordEntity.builder().build())
                .flatMap(record -> {
                    OffsetDateTime now = OffsetDateTime.now();
                    if (record.getId() == null) {
                        record.setOrderNo(order.getOrderNo());
                        record.setUserId(order.getUserId());
                        record.setCreatedAt(now);
                    }
                    record.setPayAmount(order.getTotalAmount());
                    record.setPayStatus(order.getPayStatus());
                    record.setUpdatedAt(now);
                    return paymentRecordRepository.save(record);
                });
    }

    private String mapTradeStatus(String tradeStatus) {
        if (isPaidTradeStatus(tradeStatus)) {
            return "PAID";
        }
        if (!StringUtils.hasText(tradeStatus)) {
            return "FAILED";
        }
        return "FAILED";
    }

    private boolean isPaidTradeStatus(String tradeStatus) {
        return "TRADE_SUCCESS".equalsIgnoreCase(tradeStatus) || "TRADE_FINISHED".equalsIgnoreCase(tradeStatus);
    }

    private BigDecimal parseAmount(String totalAmount, BigDecimal fallback) {
        if (!StringUtils.hasText(totalAmount)) {
            return fallback;
        }
        try {
            return new BigDecimal(totalAmount);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String writeCallbackContent(MultiValueMap<String, String> callbackForm) {
        try {
            return objectMapper.writeValueAsString(callbackForm.toSingleValueMap());
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500, "internal server error");
        }
    }

    private String writeGatewayQueryContent(AlipayGatewayClient.TradeQueryResult queryResult) {
        try {
            Map<String, String> payload = new LinkedHashMap<>();
            payload.put("trade_status", queryResult.tradeStatus());
            payload.put("trade_no", queryResult.tradeNo());
            payload.put("total_amount", queryResult.totalAmount());
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500, "internal server error");
        }
    }

    private String firstValue(MultiValueMap<String, String> form, String key) {
        return form == null ? null : form.getFirst(key);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private Long requireUserId(AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.id() == null) {
            throw new UnauthorizedException("unauthorized");
        }
        return currentUser.id();
    }

    private void validateCallbackMatchesOrder(
            MultiValueMap<String, String> callbackForm,
            OrderEntity order,
            String totalAmount
    ) {
        BigDecimal callbackAmount = parseAmount(totalAmount, null);
        if (callbackAmount == null || callbackAmount.compareTo(order.getTotalAmount()) != 0) {
            throw new BusinessException(400, "alipay callback amount mismatch");
        }

        String callbackAppId = firstValue(callbackForm, "app_id");
        if (StringUtils.hasText(callbackAppId) && !callbackAppId.equals(alipayProperties.appId())) {
            throw new BusinessException(400, "alipay callback appId mismatch");
        }

        String configuredSellerId = safe(alipayProperties.sellerId());
        String callbackSellerId = firstValue(callbackForm, "seller_id");
        if (StringUtils.hasText(configuredSellerId)
                && StringUtils.hasText(callbackSellerId)
                && !configuredSellerId.equals(callbackSellerId)) {
            throw new BusinessException(400, "alipay callback sellerId mismatch");
        }
    }

    private Mono<String> resolvePaymentStatus(OrderEntity order, PaymentRecordEntity record) {
        String currentPayStatus = StringUtils.hasText(record.getPayStatus()) ? record.getPayStatus() : order.getPayStatus();
        if ("PAID".equalsIgnoreCase(currentPayStatus)) {
            return Mono.just("PAID");
        }

        return Mono.fromCallable(() -> alipayGatewayClient.queryTrade(order.getOrderNo()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(queryResult -> {
                    if (!isPaidTradeStatus(queryResult.tradeStatus())) {
                        return Mono.just(StringUtils.hasText(currentPayStatus) ? currentPayStatus : "UNPAID");
                    }
                    return persistPaidStatusFromGateway(order, record, queryResult).thenReturn("PAID");
                })
                .onErrorResume(ex -> {
                    log.warn("Failed to query Alipay trade status: orderNo={}, reason={}", order.getOrderNo(), ex.getMessage());
                    return Mono.just(StringUtils.hasText(currentPayStatus) ? currentPayStatus : "UNPAID");
                });
    }

    private Mono<Void> persistPaidStatusFromGateway(
            OrderEntity order,
            PaymentRecordEntity record,
            AlipayGatewayClient.TradeQueryResult queryResult
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        if (record.getId() == null) {
            record.setOrderNo(order.getOrderNo());
            record.setUserId(order.getUserId());
            record.setCreatedAt(now);
        }
        record.setPayAmount(parseAmount(queryResult.totalAmount(), order.getTotalAmount()));
        record.setPayStatus("PAID");
        record.setAlipayTradeNo(queryResult.tradeNo());
        record.setCallbackContent(writeGatewayQueryContent(queryResult));
        record.setUpdatedAt(now);

        order.setPayStatus("PAID");
        order.setStatus("PAID");
        order.setUpdatedAt(now);

        return paymentRecordRepository.save(record)
                .flatMap(savedRecord -> orderRepository.save(order)
                        .doOnSuccess(savedOrder -> log.info(
                                "Order status refreshed from Alipay query: orderNo={}, payStatus={}, alipayTradeNo={}",
                                savedOrder.getOrderNo(),
                                savedOrder.getPayStatus(),
                                savedRecord.getAlipayTradeNo()
                        ))
                        .then());
    }
}
