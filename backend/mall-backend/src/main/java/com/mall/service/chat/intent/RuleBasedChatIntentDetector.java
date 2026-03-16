package com.mall.service.chat.intent;

import org.springframework.stereotype.Component;

@Component
public class RuleBasedChatIntentDetector {

    private final ChatMessageParser messageParser;

    public RuleBasedChatIntentDetector(ChatMessageParser messageParser) {
        this.messageParser = messageParser;
    }

    public ChatIntent detect(String message) {
        boolean referencesRankedProduct = messageParser.extractOrdinal(message) != null
                || messageParser.containsAny(message, "这个", "刚才那个");

        if (messageParser.containsAny(message, "支付", "付款", "去支付", "继续支付")) {
            return ChatIntent.PAY_GUIDE;
        }
        if (messageParser.containsAny(message, "推荐", "适合", "有什么好的", "哪款好", "哪个好")) {
            return ChatIntent.RECOMMEND_PRODUCT;
        }
        if (messageParser.containsAny(message, "购物车", "看看购物车", "查看购物车")
                && !messageParser.containsAny(message, "加入购物车", "加购物车", "加购", "放到购物车", "放进购物车", "放购物车")) {
            return ChatIntent.VIEW_CART;
        }
        if (messageParser.containsAny(message, "订单", "查看订单", "我的订单")) {
            return ChatIntent.VIEW_ORDER;
        }
        if (messageParser.containsAny(message, "加入购物车", "加购物车", "加购", "放到购物车", "放进购物车", "放购物车")) {
            return ChatIntent.ADD_TO_CART;
        }
        if (referencesRankedProduct && messageParser.containsAny(message, "买", "购买", "下单")) {
            return ChatIntent.BUY_NOW;
        }
        if (messageParser.containsAny(message, "立即购买", "直接买", "下单", "买这个", "购买")) {
            return ChatIntent.BUY_NOW;
        }
        if (messageParser.containsAny(message, "找", "搜索", "想要", "看看", "便宜", "商品")) {
            return ChatIntent.SEARCH_PRODUCT;
        }
        return ChatIntent.GENERAL_QA;
    }
}
