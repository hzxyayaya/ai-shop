package com.mall.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mall.dto.chat.ChatRequest;
import com.mall.dto.order.OrderDto;
import com.mall.dto.order.OrderItemDto;
import com.mall.dto.product.ProductDto;
import com.mall.dto.product.ProductListResponse;
import com.mall.common.exception.BusinessException;
import com.mall.entity.chat.ChatSessionContextEntity;
import com.mall.model.AuthenticatedUser;
import com.mall.service.CartService;
import com.mall.service.ChatMessageHistoryService;
import com.mall.service.ChatService;
import com.mall.service.ChatSessionContextService;
import com.mall.service.OrderService;
import com.mall.service.ProductService;
import com.mall.service.chat.flow.ChatFlowCoordinator;
import com.mall.service.chat.handler.CartChatHandler;
import com.mall.service.chat.handler.GeneralChatHandler;
import com.mall.service.chat.handler.OrderChatHandler;
import com.mall.service.chat.handler.SearchChatHandler;
import com.mall.service.chat.intent.ChatMessageParser;
import com.mall.service.chat.intent.IntentCoordinator;
import com.mall.service.chat.intent.RuleBasedChatIntentDetector;
import com.mall.service.chat.llm.ChatActionAssistant;
import com.mall.service.chat.llm.ChatAiAnalysis;
import com.mall.service.chat.llm.ChatAiAssistant;
import com.mall.service.chat.llm.ChatSessionMemoryProvider;
import com.mall.service.chat.memory.ChatMemoryService;
import com.mall.service.chat.retrieval.ChatProductResolver;
import com.mall.service.chat.retrieval.ChatQueryPlanner;
import com.mall.service.chat.retrieval.ChatEmbeddingService;
import com.mall.service.chat.retrieval.TargetProductResolver;
import com.mall.service.chat.support.ChatResponseFactory;
import io.r2dbc.postgresql.codec.Json;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ProductService productService;

    @Mock
    private CartService cartService;

    @Mock
    private OrderService orderService;

    @Mock
    private ChatSessionContextService chatSessionContextService;

    @Mock
    private ChatAiAssistant chatAiAssistant;

    @Mock
    private ChatActionAssistant chatActionAssistant;

    @Mock
    private ChatMessageHistoryService chatMessageHistoryService;

    @Mock
    private ChatSessionMemoryProvider chatSessionMemoryProvider;

        @Mock
        private ChatEmbeddingService chatEmbeddingService;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        ChatMessageParser messageParser = new ChatMessageParser();
        RuleBasedChatIntentDetector ruleDetector = new RuleBasedChatIntentDetector(messageParser);
        IntentCoordinator intentCoordinator = new IntentCoordinator(ruleDetector);
        ChatMemoryService chatMemoryService = new ChatMemoryService(chatSessionContextService);
        ChatQueryPlanner chatQueryPlanner = new ChatQueryPlanner(messageParser);
        ChatProductResolver chatProductResolver = new ChatProductResolver(productService, chatEmbeddingService);
        TargetProductResolver targetProductResolver = new TargetProductResolver(
                productService,
                messageParser,
                chatMemoryService,
                chatQueryPlanner
        );
        ChatResponseFactory responseFactory = new ChatResponseFactory();
        SearchChatHandler searchChatHandler = new SearchChatHandler(
                chatQueryPlanner,
                chatProductResolver,
                chatMemoryService,
                responseFactory
        );
        CartChatHandler cartChatHandler = new CartChatHandler(
                cartService,
                targetProductResolver,
                chatMemoryService,
                responseFactory
        );
        OrderChatHandler orderChatHandler = new OrderChatHandler(
                orderService,
                targetProductResolver,
                chatMemoryService,
                responseFactory
        );
        GeneralChatHandler generalChatHandler = new GeneralChatHandler(chatMemoryService, responseFactory);
        ChatFlowCoordinator coordinator = new ChatFlowCoordinator(
                intentCoordinator,
                searchChatHandler,
                cartChatHandler,
                orderChatHandler,
                generalChatHandler,
                chatActionAssistant
        );
        chatService = new ChatService(
                messageParser,
                chatAiAssistant,
                coordinator,
                chatMessageHistoryService,
                chatSessionMemoryProvider,
                chatSessionContextService
        );

        lenient().when(chatActionAssistant.execute(any(), any(), any(), any(), any())).thenReturn(Mono.empty());
        lenient().when(chatMessageHistoryService.saveTurn(any(), any(), any(), any())).thenReturn(Mono.empty());
                lenient().when(chatEmbeddingService.embedToPgVectorLiteral(any())).thenReturn(Mono.empty());
    }

    @Test
    void shouldResolveSecondProductFromSessionContextForBuyNow() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "orion", "orion@example.com", "Orion");
        ChatSessionContextEntity context = ChatSessionContextEntity.builder()
                .id(10L)
                .userId(1L)
                .sessionId("chat-1")
                .lastProductIdsJson(Json.of("[101,102,103]"))
                .build();
        ProductDto product = new ProductDto(
                102L,
                "箱包",
                "第二个商品",
                BigDecimal.valueOf(129),
                "100+",
                "https://example.com/2.jpg",
                "店铺"
        );
        OrderDto order = new OrderDto(
                "ORD202603140001",
                BigDecimal.valueOf(129),
                "CREATED",
                "UNPAID",
                OffsetDateTime.parse("2026-03-14T00:00:00+08:00"),
                List.of(new OrderItemDto(
                        102L,
                        "第二个商品",
                        "箱包",
                        BigDecimal.valueOf(129),
                        1,
                        BigDecimal.valueOf(129),
                        "https://example.com/2.jpg",
                        "店铺"
                ))
        );

        when(chatSessionContextService.getContext(1L, "chat-1")).thenReturn(Mono.just(context));
        when(chatAiAssistant.analyze("1::chat-1", "第二个直接买")).thenReturn(Mono.empty());
        when(chatSessionContextService.readProductIds(context)).thenReturn(List.of(101L, 102L, 103L));
        when(productService.getProductDetail(102L)).thenReturn(Mono.just(product));
        when(orderService.createBuyNowOrder(eq(user), any())).thenReturn(Mono.just(order));
        when(chatSessionContextService.saveContext(eq(1L), eq("chat-1"), eq("BUY_NOW"), eq("第二个直接买"), eq(List.of(101L, 102L, 103L)), eq("ORD202603140001")))
                .thenReturn(Mono.empty());

        StepVerifier.create(chatService.chat(user, new ChatRequest("chat-1", "第二个直接买")))
                .assertNext(response -> {
                    assertThat(response.intent()).isEqualTo("BUY_NOW");
                    assertThat(response.orders()).hasSize(1);
                    assertThat(response.actions()).hasSize(2);
                    assertThat(response.actions().get(0).type()).isEqualTo("PAY_NOW");
                    assertThat(response.actions().get(0).targetId()).isEqualTo("ORD202603140001");
                    assertThat(response.message()).contains("第二个商品");
                })
                .verifyComplete();

        verify(productService).getProductDetail(102L);
    }

    @Test
    void shouldReturnSearchProductsAndRememberProductIds() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "orion", "orion@example.com", "Orion");
        ProductDto first = new ProductDto(
                101L,
                "箱包",
                "通勤双肩包",
                BigDecimal.valueOf(199),
                "100+",
                "https://example.com/1.jpg",
                "店铺A"
        );
        ProductDto second = new ProductDto(
                102L,
                "箱包",
                "学生双肩包",
                BigDecimal.valueOf(129),
                "200+",
                "https://example.com/2.jpg",
                "店铺B"
        );

        when(productService.searchProducts(eq("双肩包"), eq(1), eq(6), eq(null), eq(null)))
                .thenReturn(Mono.just(new ProductListResponse(1, 6, 2L, List.of(first, second))));
        when(chatAiAssistant.analyze("1::chat-1", "帮我找双肩包")).thenReturn(Mono.empty());
        when(chatSessionContextService.saveContext(
                eq(1L),
                eq("chat-1"),
                eq("SEARCH_PRODUCT"),
                eq("帮我找双肩包"),
                eq(List.of(101L, 102L)),
                eq(null)
        )).thenReturn(Mono.empty());

        StepVerifier.create(chatService.chat(user, new ChatRequest("chat-1", "帮我找双肩包")))
                .assertNext(response -> {
                    assertThat(response.intent()).isEqualTo("SEARCH_PRODUCT");
                    assertThat(response.products()).hasSize(2);
                    assertThat(response.actions()).extracting(action -> action.type())
                            .containsExactly("ADD_TO_CART", "BUY_NOW");
                })
                .verifyComplete();
    }

    @Test
    void shouldTreatPutIntoCartAsAddToCartAndPreserveRecentProducts() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "orion", "orion@example.com", "Orion");
        ChatSessionContextEntity context = ChatSessionContextEntity.builder()
                .id(11L)
                .userId(1L)
                .sessionId("chat-4")
                .lastProductIdsJson(Json.of("[201,202,203]"))
                .build();
        ProductDto product = new ProductDto(
                202L,
                "个护",
                "第二个洗面奶",
                BigDecimal.valueOf(17.9),
                "100000",
                "https://example.com/202.jpg",
                "店铺E"
        );

        when(chatSessionContextService.getContext(1L, "chat-4")).thenReturn(Mono.just(context));
        when(chatSessionContextService.readProductIds(context)).thenReturn(List.of(201L, 202L, 203L));
        when(chatAiAssistant.analyze("1::chat-4", "把第二个放到购物车当中")).thenReturn(Mono.empty());
        when(productService.getProductDetail(202L)).thenReturn(Mono.just(product));
        when(cartService.addToCart(eq(user), any())).thenReturn(Mono.just(88L));
        when(chatSessionContextService.saveContext(
                eq(1L),
                eq("chat-4"),
                eq("ADD_TO_CART"),
                eq("把第二个放到购物车当中"),
                eq(List.of(201L, 202L, 203L)),
                eq(null)
        )).thenReturn(Mono.empty());

        StepVerifier.create(chatService.chat(user, new ChatRequest("chat-4", "把第二个放到购物车当中")))
                .assertNext(response -> {
                    assertThat(response.intent()).isEqualTo("ADD_TO_CART");
                    assertThat(response.message()).contains("第二个洗面奶");
                    assertThat(response.actions()).extracting(action -> action.type())
                            .containsExactly("GO_CART");
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnRecommendedProductsFromAiAnalysis() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "orion", "orion@example.com", "Orion");
        ProductDto first = new ProductDto(
                201L,
                "箱包",
                "轻薄通勤双肩包",
                BigDecimal.valueOf(239),
                "500+",
                "https://example.com/201.jpg",
                "店铺C"
        );

        when(chatAiAssistant.analyze("1::chat-2", "推荐一个适合通勤的包")).thenReturn(Mono.just(
                new ChatAiAnalysis(
                        "RECOMMEND_PRODUCT",
                        "适合通勤的双肩包",
                        "",
                        "我优先帮你挑了更适合通勤和日常搭配的款式。"
                )
        ));
        when(productService.searchProducts(eq("适合通勤的双肩包"), eq(1), eq(6), eq(null), eq(null)))
                .thenReturn(Mono.just(new ProductListResponse(1, 6, 1L, List.of(first))));
        when(chatSessionContextService.saveContext(
                eq(1L),
                eq("chat-2"),
                eq("RECOMMEND_PRODUCT"),
                eq("推荐一个适合通勤的包"),
                eq(List.of(201L)),
                eq(null)
        )).thenReturn(Mono.empty());

        StepVerifier.create(chatService.chat(user, new ChatRequest("chat-2", "推荐一个适合通勤的包")))
                .assertNext(response -> {
                    assertThat(response.intent()).isEqualTo("RECOMMEND_PRODUCT");
                    assertThat(response.message()).contains("适合通勤");
                    assertThat(response.products()).hasSize(1);
                    assertThat(response.actions()).extracting(action -> action.type())
                            .containsExactly("ADD_TO_CART", "BUY_NOW");
                })
                .verifyComplete();
    }

    @Test
    void shouldUseRuleFallbackForHighRiskIntentConflict() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "orion", "orion@example.com", "Orion");
        ProductDto first = new ProductDto(
                301L,
                "箱包",
                "高性价比双肩包",
                BigDecimal.valueOf(159),
                "300+",
                "https://example.com/301.jpg",
                "店铺D"
        );

        when(chatAiAssistant.analyze("1::chat-3", "帮我找双肩包")).thenReturn(Mono.just(
                new ChatAiAnalysis("BUY_NOW", "双肩包", "", "")
        ));
        when(productService.searchProducts(eq("双肩包"), eq(1), eq(6), eq(null), eq(null)))
                .thenReturn(Mono.just(new ProductListResponse(1, 6, 1L, List.of(first))));
        when(chatSessionContextService.saveContext(
                eq(1L),
                eq("chat-3"),
                eq("SEARCH_PRODUCT"),
                eq("帮我找双肩包"),
                eq(List.of(301L)),
                eq(null)
        )).thenReturn(Mono.empty());

        StepVerifier.create(chatService.chat(user, new ChatRequest("chat-3", "帮我找双肩包")))
                .assertNext(response -> assertThat(response.intent()).isEqualTo("SEARCH_PRODUCT"))
                .verifyComplete();
    }

    @Test
    void shouldIsolateContextWhenTwoUsersShareSameSessionId() {
        AuthenticatedUser userA = new AuthenticatedUser(1L, "orion", "orion@example.com", "Orion");
        AuthenticatedUser userB = new AuthenticatedUser(2L, "nova", "nova@example.com", "Nova");

        ChatSessionContextEntity contextA = ChatSessionContextEntity.builder()
                .id(21L)
                .userId(1L)
                .sessionId("shared-session")
                .lastProductIdsJson(Json.of("[1001,1002]"))
                .build();
        ChatSessionContextEntity contextB = ChatSessionContextEntity.builder()
                .id(22L)
                .userId(2L)
                .sessionId("shared-session")
                .lastProductIdsJson(Json.of("[2001,2002]"))
                .build();

        ProductDto userASecondProduct = new ProductDto(
                1002L,
                "数码",
                "A 用户第二个耳机",
                BigDecimal.valueOf(299),
                "50+",
                "https://example.com/a-2.jpg",
                "店铺A"
        );
        ProductDto userBSecondProduct = new ProductDto(
                2002L,
                "数码",
                "B 用户第二个耳机",
                BigDecimal.valueOf(399),
                "60+",
                "https://example.com/b-2.jpg",
                "店铺B"
        );

        OrderDto orderA = new OrderDto(
                "ORD-A-002",
                BigDecimal.valueOf(299),
                "CREATED",
                "UNPAID",
                OffsetDateTime.parse("2026-03-14T00:00:00+08:00"),
                List.of(new OrderItemDto(1002L, "A 用户第二个耳机", "数码", BigDecimal.valueOf(299), 1, BigDecimal.valueOf(299), "https://example.com/a-2.jpg", "店铺A"))
        );
        OrderDto orderB = new OrderDto(
                "ORD-B-002",
                BigDecimal.valueOf(399),
                "CREATED",
                "UNPAID",
                OffsetDateTime.parse("2026-03-14T00:00:00+08:00"),
                List.of(new OrderItemDto(2002L, "B 用户第二个耳机", "数码", BigDecimal.valueOf(399), 1, BigDecimal.valueOf(399), "https://example.com/b-2.jpg", "店铺B"))
        );

        when(chatAiAssistant.analyze("1::shared-session", "买第二个")).thenReturn(Mono.empty());
        when(chatAiAssistant.analyze("2::shared-session", "买第二个")).thenReturn(Mono.empty());
        when(chatSessionContextService.getContext(1L, "shared-session")).thenReturn(Mono.just(contextA));
        when(chatSessionContextService.getContext(2L, "shared-session")).thenReturn(Mono.just(contextB));
        when(chatSessionContextService.readProductIds(contextA)).thenReturn(List.of(1001L, 1002L));
        when(chatSessionContextService.readProductIds(contextB)).thenReturn(List.of(2001L, 2002L));
        when(productService.getProductDetail(1002L)).thenReturn(Mono.just(userASecondProduct));
        when(productService.getProductDetail(2002L)).thenReturn(Mono.just(userBSecondProduct));
        when(orderService.createBuyNowOrder(eq(userA), any())).thenReturn(Mono.just(orderA));
        when(orderService.createBuyNowOrder(eq(userB), any())).thenReturn(Mono.just(orderB));
        when(chatSessionContextService.saveContext(eq(1L), eq("shared-session"), eq("BUY_NOW"), eq("买第二个"), eq(List.of(1001L, 1002L)), eq("ORD-A-002")))
                .thenReturn(Mono.empty());
        when(chatSessionContextService.saveContext(eq(2L), eq("shared-session"), eq("BUY_NOW"), eq("买第二个"), eq(List.of(2001L, 2002L)), eq("ORD-B-002")))
                .thenReturn(Mono.empty());

        StepVerifier.create(chatService.chat(userA, new ChatRequest("shared-session", "买第二个")))
                .assertNext(response -> {
                    assertThat(response.intent()).isEqualTo("BUY_NOW");
                    assertThat(response.message()).contains("A 用户第二个耳机");
                    assertThat(response.orders().get(0).orderNo()).isEqualTo("ORD-A-002");
                })
                .verifyComplete();

        StepVerifier.create(chatService.chat(userB, new ChatRequest("shared-session", "买第二个")))
                .assertNext(response -> {
                    assertThat(response.intent()).isEqualTo("BUY_NOW");
                    assertThat(response.message()).contains("B 用户第二个耳机");
                    assertThat(response.orders().get(0).orderNo()).isEqualTo("ORD-B-002");
                })
                .verifyComplete();
    }

    @Test
    void shouldInvalidateOrdinalReferenceAfterSessionDeletion() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "orion", "orion@example.com", "Orion");

        when(chatMessageHistoryService.clearSessionHistory(1L, "chat-del")).thenReturn(Mono.empty());
        when(chatSessionContextService.clearSession(1L, "chat-del")).thenReturn(Mono.empty());

        StepVerifier.create(chatService.deleteSession(user, "chat-del"))
                .verifyComplete();

        verify(chatSessionMemoryProvider).evict("1::chat-del");

        when(chatAiAssistant.analyze("1::chat-del", "买第一个")).thenReturn(Mono.empty());
        when(chatSessionContextService.getContext(1L, "chat-del")).thenReturn(Mono.empty());

        StepVerifier.create(chatService.chat(user, new ChatRequest("chat-del", "买第一个")))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    BusinessException businessException = (BusinessException) error;
                    assertThat(businessException.getCode()).isEqualTo(404);
                })
                .verify();
    }
}
