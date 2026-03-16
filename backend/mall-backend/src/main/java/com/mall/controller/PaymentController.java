package com.mall.controller;

import com.mall.model.AuthenticatedUser;
import com.mall.security.WebFluxJwtFilter;
import com.mall.common.exception.UnauthorizedException;
import com.mall.common.response.ApiResponse;
import com.mall.dto.payment.CreatePaymentRequest;
import com.mall.dto.payment.PaymentCreateResponse;
import com.mall.dto.payment.PaymentStatusResponse;
import com.mall.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create")
    public Mono<ApiResponse<PaymentCreateResponse>> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            ServerWebExchange exchange
    ) {
        return paymentService.createPayment(requireUser(exchange), request, exchange)
                .map(ApiResponse::success);
    }

    @PostMapping(
            value = "/callback/alipay",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public Mono<String> handleAlipayCallback(ServerWebExchange exchange) {
        return exchange.getFormData()
                .doOnNext(formData -> log.info(
                        "Received Alipay callback: out_trade_no={}, trade_no={}, trade_status={}, total_amount={}",
                        formData.getFirst("out_trade_no"),
                        formData.getFirst("trade_no"),
                        formData.getFirst("trade_status"),
                        formData.getFirst("total_amount")
                ))
                .flatMap(paymentService::handleAlipayCallback)
                .thenReturn("success")
                .doOnSuccess(result -> log.info("Alipay callback handled successfully, response={}", result))
                .onErrorResume(ex -> {
                    log.warn("Alipay callback handling failed", ex);
                    return Mono.just("failure");
                });
    }

    @GetMapping("/{orderNo}/status")
    public Mono<ApiResponse<PaymentStatusResponse>> getPaymentStatus(
            @PathVariable String orderNo,
            ServerWebExchange exchange
    ) {
        return paymentService.getPaymentStatus(requireUser(exchange), orderNo)
                .map(ApiResponse::success);
    }

    private AuthenticatedUser requireUser(ServerWebExchange exchange) {
        AuthenticatedUser authenticatedUser = exchange.getAttribute(WebFluxJwtFilter.AUTHENTICATED_USER_ATTR);
        if (authenticatedUser == null) {
            throw new UnauthorizedException("unauthorized");
        }
        return authenticatedUser;
    }
}
