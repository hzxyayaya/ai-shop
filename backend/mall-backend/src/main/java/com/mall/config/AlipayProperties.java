package com.mall.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.payment.alipay")
public record AlipayProperties(
        String gatewayUrl,
        String notifyUrl,
        String returnUrl,
        String appId,
        String sellerId,
        String privateKey,
        String alipayPublicKey,
        String format,
        String charset,
        String signType
) {
    public List<String> missingRequiredFields() {
        List<String> missingFields = new ArrayList<>();
        if (!StringUtils.hasText(gatewayUrl)) {
            missingFields.add("app.payment.alipay.gateway-url");
        }
        if (!StringUtils.hasText(notifyUrl)) {
            missingFields.add("app.payment.alipay.notify-url");
        }
        if (!StringUtils.hasText(returnUrl)) {
            missingFields.add("app.payment.alipay.return-url");
        }
        if (!StringUtils.hasText(appId)) {
            missingFields.add("app.payment.alipay.app-id");
        }
        if (!StringUtils.hasText(privateKey)) {
            missingFields.add("app.payment.alipay.private-key");
        }
        if (!StringUtils.hasText(alipayPublicKey)) {
            missingFields.add("app.payment.alipay.alipay-public-key");
        }
        return missingFields;
    }

    public String resolvedFormat() {
        return StringUtils.hasText(format) ? format : "json";
    }

    public String resolvedCharset() {
        return StringUtils.hasText(charset) ? charset : "UTF-8";
    }

    public String resolvedSignType() {
        return StringUtils.hasText(signType) ? signType : "RSA2";
    }
}
