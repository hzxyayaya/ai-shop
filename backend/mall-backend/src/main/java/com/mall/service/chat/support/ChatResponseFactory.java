package com.mall.service.chat.support;

import com.mall.dto.chat.ChatActionDto;
import com.mall.dto.chat.ChatResponse;
import com.mall.dto.order.OrderDto;
import com.mall.dto.product.ProductDto;
import com.mall.service.chat.intent.ChatIntent;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ChatResponseFactory {

    public ChatResponse searchResult(List<ProductDto> products) {
        return searchResult(products, null);
    }

    public ChatResponse searchResult(List<ProductDto> products, String message) {
        ProductDto first = products.get(0);
        return new ChatResponse(
                ChatIntent.SEARCH_PRODUCT.code(),
                message == null || message.isBlank()
                        ? "我帮你找到了 " + products.size() + " 个相关商品。"
                        : message,
                products,
                List.of(),
                List.of(
                        action("ADD_TO_CART", "加入购物车", String.valueOf(first.id())),
                        action("BUY_NOW", "立即购买", String.valueOf(first.id()))
                )
        );
    }

    public ChatResponse emptySearch() {
        return new ChatResponse(
                ChatIntent.SEARCH_PRODUCT.code(),
                "没有找到符合条件的商品，你可以换个关键词再试。",
                List.of(),
                List.of(),
                List.of(action("GO_HOME", "返回首页", ""))
        );
    }

    public ChatResponse addToCart(ProductDto product) {
        return new ChatResponse(
                ChatIntent.ADD_TO_CART.code(),
                "已将 " + product.title() + " 加入购物车。",
                List.of(),
                List.of(),
                List.of(action("GO_CART", "查看购物车", ""))
        );
    }

    public ChatResponse buyNow(OrderDto order, ProductDto product) {
        String productTitle = product == null ? null : product.title();
        return new ChatResponse(
                ChatIntent.BUY_NOW.code(),
                productTitle == null || productTitle.isBlank()
                        ? "已为你创建订单，可以继续支付。"
                        : "已为你创建 " + productTitle + " 的订单，正在为你跳转支付。",
                List.of(),
                List.of(order),
                List.of(
                        action("PAY_NOW", "立即支付", order.orderNo()),
                        action("VIEW_ORDER", "查看订单", "")
                )
        );
    }

    public ChatResponse viewCart() {
        return new ChatResponse(
                ChatIntent.VIEW_CART.code(),
                "你可以直接去购物车查看已加入的商品。",
                List.of(),
                List.of(),
                List.of(action("GO_CART", "查看购物车", ""))
        );
    }

    public ChatResponse emptyOrders() {
        return new ChatResponse(
                ChatIntent.VIEW_ORDER.code(),
                "你目前还没有订单。",
                List.of(),
                List.of(),
                List.of(action("GO_HOME", "去逛逛", ""))
        );
    }

    public ChatResponse viewOrders(List<OrderDto> orders) {
        OrderDto firstUnpaid = orders.stream()
                .filter(order -> "UNPAID".equalsIgnoreCase(order.payStatus()))
                .findFirst()
                .orElse(null);
        List<ChatActionDto> actions = firstUnpaid == null
                ? List.of(action("VIEW_ORDER", "查看订单", ""))
                : List.of(
                        action("PAY_NOW", "立即支付", firstUnpaid.orderNo()),
                        action("VIEW_ORDER", "查看订单", "")
                );
        return new ChatResponse(
                ChatIntent.VIEW_ORDER.code(),
                "我帮你找到了 " + orders.size() + " 条订单记录。",
                List.of(),
                orders,
                actions
        );
    }

    public String orderNoForOrderViewMemory(List<OrderDto> orders) {
        return orders.stream()
                .filter(order -> "UNPAID".equalsIgnoreCase(order.payStatus()))
                .findFirst()
                .map(OrderDto::orderNo)
                .orElseGet(() -> orders.get(0).orderNo());
    }

    public ChatResponse emptyPayGuide() {
        return new ChatResponse(
                ChatIntent.PAY_GUIDE.code(),
                "你当前没有待支付订单，可以先去挑选商品。",
                List.of(),
                List.of(),
                List.of(action("GO_HOME", "去逛逛", ""))
        );
    }

    public ChatResponse payGuide(List<OrderDto> orders) {
        OrderDto firstOrder = orders.get(0);
        return new ChatResponse(
                ChatIntent.PAY_GUIDE.code(),
                "我帮你找到了待支付订单，可以直接继续支付。",
                List.of(),
                orders,
                List.of(
                        action("PAY_NOW", "立即支付", firstOrder.orderNo()),
                        action("VIEW_ORDER", "查看订单", "")
                )
        );
    }

    public ChatResponse generalQa() {
        return generalQa(null);
    }

    public ChatResponse recommendResult(List<ProductDto> products, String message) {
        ProductDto first = products.get(0);
        return new ChatResponse(
                ChatIntent.RECOMMEND_PRODUCT.code(),
                message == null || message.isBlank()
                        ? "我先帮你挑了几款更值得优先看的商品。"
                        : message,
                products,
                List.of(),
                List.of(
                        action("ADD_TO_CART", "加入购物车", String.valueOf(first.id())),
                        action("BUY_NOW", "立即购买", String.valueOf(first.id()))
                )
        );
    }

    public ChatResponse emptyRecommend() {
        return new ChatResponse(
                ChatIntent.RECOMMEND_PRODUCT.code(),
                "暂时没有找到特别合适的推荐商品，你可以换个需求再试。",
                List.of(),
                List.of(),
                List.of(action("GO_HOME", "返回首页", ""))
        );
    }

    public ChatResponse generalQa(String message) {
        return new ChatResponse(
                ChatIntent.GENERAL_QA.code(),
                message == null || message.isBlank()
                        ? "我可以帮你找商品、加入购物车、立即购买，或者查看订单。"
                        : message,
                List.of(),
                List.of(),
                List.of(
                        action("GO_CART", "查看购物车", ""),
                        action("GO_HOME", "返回首页", ""),
                        action("VIEW_ORDER", "查看订单", "")
                )
        );
    }

    private ChatActionDto action(String type, String label, String targetId) {
        return new ChatActionDto(type, label, targetId);
    }
}
