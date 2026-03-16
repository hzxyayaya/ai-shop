package com.mall.client;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.exception.BusinessException;
import com.mall.config.AlipayProperties;
import com.mall.dto.order.OrderItemDto;
import com.mall.entity.order.OrderEntity;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

@Component
public class AlipayGatewayClient {

    private static final int MAX_SUBJECT_LENGTH = 128;
    private static final int MAX_BODY_LENGTH = 256;
    private static final int TITLE_PREVIEW_CHARS = 5;
    private static final TypeReference<List<OrderItemDto>> ORDER_ITEM_LIST = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final AlipayProperties alipayProperties;

    public AlipayGatewayClient(ObjectMapper objectMapper, AlipayProperties alipayProperties) {
        this.objectMapper = objectMapper;
        this.alipayProperties = alipayProperties;
    }

    public String createPagePayForm(OrderEntity order) {
        return createPagePayForm(order, alipayProperties.returnUrl());
    }

    public String createPagePayForm(OrderEntity order, String returnUrl) {
        assertConfigured();
        try {
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setNotifyUrl(alipayProperties.notifyUrl());
            request.setReturnUrl(returnUrl);
            request.setBizContent(objectMapper.writeValueAsString(buildBizContent(order)));
            return createClient().pageExecute(request).getBody();
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500, "internal server error");
        } catch (AlipayApiException ex) {
            throw new BusinessException(500, "failed to create alipay page form");
        }
    }

    public boolean verifyCallback(MultiValueMap<String, String> callbackForm) {
        assertConfigured();
        Map<String, String> params = toSingleValueMap(callbackForm);
        try {
            return AlipaySignature.rsaCheckV1(
                    params,
                    normalizeKey(alipayProperties.alipayPublicKey()),
                    alipayProperties.resolvedCharset(),
                    alipayProperties.resolvedSignType()
            );
        } catch (AlipayApiException ex) {
            throw new BusinessException(400, "failed to verify alipay callback signature");
        }
    }

    public TradeQueryResult queryTrade(String orderNo) {
        assertConfigured();
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizContent(objectMapper.writeValueAsString(Map.of("out_trade_no", orderNo)));
            AlipayTradeQueryResponse response = createClient().execute(request);
            if (response == null) {
                throw new BusinessException(502, "empty alipay trade query response");
            }
            return new TradeQueryResult(
                    response.getTradeStatus(),
                    response.getTradeNo(),
                    response.getTotalAmount()
            );
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500, "internal server error");
        } catch (AlipayApiException ex) {
            throw new BusinessException(502, "failed to query alipay trade status");
        }
    }

    public List<String> missingRequiredFields() {
        return alipayProperties.missingRequiredFields();
    }

    private Map<String, Object> buildBizContent(OrderEntity order) {
        List<OrderItemDto> orderItems = readOrderItems(order);
        Map<String, Object> bizContent = new LinkedHashMap<>();
        bizContent.put("out_trade_no", order.getOrderNo());
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        bizContent.put("total_amount", order.getTotalAmount().toPlainString());
        bizContent.put("subject", buildSubject(order, orderItems));
        String body = buildBody(orderItems);
        if (StringUtils.hasText(body)) {
            bizContent.put("body", body);
        }
        return bizContent;
    }

    private List<OrderItemDto> readOrderItems(OrderEntity order) {
        if (order == null || order.getItemsJson() == null || !StringUtils.hasText(order.getItemsJson().asString())) {
            return List.of();
        }
        try {
            return objectMapper.readValue(order.getItemsJson().asString(), ORDER_ITEM_LIST);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500, "failed to parse order items for payment");
        }
    }

    private String buildSubject(OrderEntity order, List<OrderItemDto> orderItems) {
        if (orderItems.isEmpty()) {
            return truncate("AI Shop Order " + order.getOrderNo(), MAX_SUBJECT_LENGTH);
        }

        OrderItemDto firstItem = orderItems.get(0);
        String firstTitle = compactTitle(firstItem.title());
        if (orderItems.size() == 1) {
            Integer quantity = firstItem.quantity();
            if (quantity != null && quantity > 1) {
                return truncate(firstTitle + " x" + quantity, MAX_SUBJECT_LENGTH);
            }
            return truncate(firstTitle, MAX_SUBJECT_LENGTH);
        }

        return truncate(firstTitle + " 等" + orderItems.size() + "件商品", MAX_SUBJECT_LENGTH);
    }

    private String buildBody(List<OrderItemDto> orderItems) {
        if (orderItems.isEmpty()) {
            return null;
        }

        OrderItemDto firstItem = orderItems.get(0);
        StringBuilder body = new StringBuilder();
        appendDetail(body, "商品", compactTitle(firstItem.title()));
        appendDetail(body, "数量", firstItem.quantity() == null ? null : String.valueOf(firstItem.quantity()));
        appendDetail(body, "店铺", firstItem.shopName());
        if (orderItems.size() > 1) {
            appendDetail(body, "订单包含", "共" + orderItems.size() + "件商品");
        }
        return truncate(body.toString(), MAX_BODY_LENGTH);
    }

    private void appendDetail(StringBuilder target, String label, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (!target.isEmpty()) {
            target.append("; ");
        }
        target.append(label).append(": ").append(value.trim());
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 1) + "…";
    }

    private String compactTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return "商品订单";
        }
        String trimmed = title.trim();
        if (trimmed.length() <= TITLE_PREVIEW_CHARS) {
            return trimmed;
        }
        return trimmed.substring(0, TITLE_PREVIEW_CHARS) + "...";
    }

    private AlipayClient createClient() throws AlipayApiException {
        AlipayConfig config = new AlipayConfig();
        config.setServerUrl(alipayProperties.gatewayUrl());
        config.setAppId(alipayProperties.appId());
        config.setFormat(alipayProperties.resolvedFormat());
        config.setCharset(alipayProperties.resolvedCharset());
        config.setSignType(alipayProperties.resolvedSignType());
        config.setPrivateKey(normalizeKey(alipayProperties.privateKey()));
        config.setAlipayPublicKey(normalizeKey(alipayProperties.alipayPublicKey()));
        return new DefaultAlipayClient(config);
    }

    private void assertConfigured() {
        List<String> missingFields = alipayProperties.missingRequiredFields();
        if (!missingFields.isEmpty()) {
            throw new BusinessException(500, "alipay sandbox config missing: " + String.join(", ", missingFields));
        }
    }

    private Map<String, String> toSingleValueMap(MultiValueMap<String, String> callbackForm) {
        return callbackForm == null ? new LinkedHashMap<>() : new LinkedHashMap<>(callbackForm.toSingleValueMap());
    }

    private String normalizeKey(String key) {
        if (!StringUtils.hasText(key)) {
            return key;
        }
        return key
                .replace("\\n", "\n")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
    }

    public record TradeQueryResult(
            String tradeStatus,
            String tradeNo,
            String totalAmount
    ) {
    }
}
