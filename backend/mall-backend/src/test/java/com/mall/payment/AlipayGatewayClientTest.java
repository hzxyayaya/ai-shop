package com.mall.payment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alipay.api.internal.util.AlipaySignature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.client.AlipayGatewayClient;
import com.mall.config.AlipayProperties;
import com.mall.dto.order.OrderItemDto;
import com.mall.entity.order.OrderEntity;
import io.r2dbc.postgresql.codec.Json;
import java.math.BigDecimal;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

class AlipayGatewayClientTest {

    @Test
    void createPagePayFormContainsOrderContext() throws Exception {
        TestKeys keys = generateKeys();
        AlipayGatewayClient client = new AlipayGatewayClient(new ObjectMapper(), properties(keys));

        OrderEntity order = new OrderEntity();
        order.setOrderNo("ORD202603130001");
        order.setTotalAmount(new BigDecimal("199.00"));
        order.setItemsJson(Json.of(new ObjectMapper().writeValueAsString(List.of(
                new OrderItemDto(
                        101L,
                        "华为 Mate 70 Pro",
                        "手机",
                        new BigDecimal("199.00"),
                        1,
                        new BigDecimal("199.00"),
                        "https://cdn.ai-shop.example/huawei.jpg",
                        "华为旗舰店"
                )
        ))));

        String form = client.createPagePayForm(order);

        assertTrue(form.contains("ORD202603130001"));
        assertTrue(form.contains("FAST_INSTANT_TRADE_PAY"));
        assertTrue(form.contains("&quot;subject&quot;:&quot;"));
        assertTrue(form.contains("..."));
        assertTrue(form.contains("&quot;body&quot;:&quot;"));
        assertTrue(form.contains("数量: 1"));
        assertTrue(form.contains("店铺:"));
    }

    @Test
    void createPagePayFormUsesProvidedReturnUrl() throws Exception {
        TestKeys keys = generateKeys();
        AlipayGatewayClient client = new AlipayGatewayClient(new ObjectMapper(), properties(keys));

        OrderEntity order = new OrderEntity();
        order.setOrderNo("ORD202603130002");
        order.setTotalAmount(new BigDecimal("9.90"));

        String form = client.createPagePayForm(order, "http://127.0.0.1:5173/payment/result");

        assertTrue(form.contains("return_url=http%3A%2F%2F127.0.0.1%3A5173%2Fpayment%2Fresult"));
    }

    @Test
    void verifyCallbackAcceptsValidSignatureAndRejectsTamperedPayload() throws Exception {
        TestKeys keys = generateKeys();
        AlipayGatewayClient client = new AlipayGatewayClient(new ObjectMapper(), properties(keys));

        MultiValueMap<String, String> callbackForm = buildSignedCallback(keys.privateKey());
        assertTrue(client.verifyCallback(callbackForm));

        callbackForm.set("total_amount", "299.00");
        assertFalse(client.verifyCallback(callbackForm));
    }

    private MultiValueMap<String, String> buildSignedCallback(String privateKey) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_id", "sandbox-app-id");
        params.put("charset", "UTF-8");
        params.put("notify_time", "2026-03-13 21:00:00");
        params.put("out_trade_no", "ORD202603130001");
        params.put("seller_id", "sandbox-seller-id");
        params.put("total_amount", "199.00");
        params.put("trade_no", "2026031322001499999999999999");
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("sign", AlipaySignature.sign(params, privateKey, "UTF-8", "RSA2"));
        params.put("sign_type", "RSA2");

        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        params.forEach(form::add);
        return form;
    }

    private AlipayProperties properties(TestKeys keys) {
        return new AlipayProperties(
                "https://openapi-sandbox.dl.alipaydev.com/gateway.do",
                "http://localhost:8080/api/payments/callback/alipay",
                "http://localhost:5173/payment/result",
                "sandbox-app-id",
                "sandbox-seller-id",
                keys.privateKey(),
                keys.publicKey(),
                "json",
                "UTF-8",
                "RSA2"
        );
    }

    private TestKeys generateKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        return new TestKeys(
                Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()),
                Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded())
        );
    }

    private record TestKeys(String privateKey, String publicKey) {
    }
}
