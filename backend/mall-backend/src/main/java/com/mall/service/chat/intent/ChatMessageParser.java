package com.mall.service.chat.intent;

import com.mall.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ChatMessageParser {

    public String normalizeMessage(String message) {
        if (!StringUtils.hasText(message)) {
            throw new BusinessException(400, "message is required");
        }
        return message.trim();
    }

    public String extractKeyword(String message) {
        String keyword = message
                .replace("帮我", "")
                .replace("给我", "")
                .replace("一下", "")
                .replace("加入购物车", "")
                .replace("加购物车", "")
                .replace("加购", "")
                .replace("立即购买", "")
                .replace("直接买", "")
                .replace("买这个", "")
                .replace("下单", "")
                .replace("看看购物车", "")
                .replace("查看购物车", "")
                .replace("购物车", "")
                .replace("支付", "")
                .replace("付款", "")
                .replace("继续支付", "")
                .replace("查看订单", "")
                .replace("我的订单", "")
                .replace("帮我找", "")
                .replace("搜索", "")
                .replace("推荐", "")
                .replace("第一个", "")
                .replace("第二个", "")
                .replace("第三个", "")
                .trim();
        keyword = keyword.replaceFirst("^(找|看看|看下|搜一下|搜搜|搜|推荐一下)", "").trim();
        return StringUtils.hasText(keyword) ? keyword : message;
    }

    public Long extractProductId(String message) {
        String digits = message.replaceAll("\\D+", "");
        if (!StringUtils.hasText(digits)) {
            return null;
        }
        try {
            return Long.valueOf(digits);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public Integer extractOrdinal(String message) {
        if (message.contains("第一个")) {
            return 0;
        }
        if (message.contains("第二个")) {
            return 1;
        }
        if (message.contains("第三个")) {
            return 2;
        }
        return null;
    }

    public boolean containsAny(String text, String... parts) {
        for (String part : parts) {
            if (text.contains(part)) {
                return true;
            }
        }
        return false;
    }
}
