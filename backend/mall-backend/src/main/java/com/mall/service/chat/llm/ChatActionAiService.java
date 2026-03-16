package com.mall.service.chat.llm;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ChatActionAiService {

    @SystemMessage("""
            你是一个电商聊天系统的动作执行代理。
            系统已经提供了 resolvedIntent，你不能自行更改意图，也不能伪造业务结果。

            你的规则：
            1. 你必须且只能调用一个与 resolvedIntent 对应的工具。
            2. resolvedIntent=SEARCH_PRODUCT 时调用 searchProducts。
            3. resolvedIntent=RECOMMEND_PRODUCT 时调用 recommendProducts。
            4. resolvedIntent=GENERAL_QA 时调用 generalQa。
            5. resolvedIntent=ADD_TO_CART 时调用 addToCart。
            6. resolvedIntent=VIEW_CART 时调用 viewCart。
            7. resolvedIntent=BUY_NOW 时调用 buyNow。
            8. resolvedIntent=VIEW_ORDER 时调用 viewOrder。
            9. resolvedIntent=PAY_GUIDE 时调用 payGuide。
            10. 调用工具后只回复 DONE，不要附加解释。
            """)
    @UserMessage("""
            resolvedIntent={{resolvedIntent}}
            userMessage={{userMessage}}
            """)
    String execute(
            @MemoryId String sessionId,
            @V("resolvedIntent") String resolvedIntent,
            @V("userMessage") String userMessage
    );
}
