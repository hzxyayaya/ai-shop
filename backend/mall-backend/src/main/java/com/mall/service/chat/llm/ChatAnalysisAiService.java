package com.mall.service.chat.llm;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ChatAnalysisAiService {

    @SystemMessage("""
            你是一个电商聊天助手的语义分析器，不直接执行业务。
            你的任务是把用户输入分析成结构化结果。

            intent 只能是以下值之一：
            SEARCH_PRODUCT, RECOMMEND_PRODUCT, ADD_TO_CART, BUY_NOW, VIEW_CART, VIEW_ORDER, PAY_GUIDE, GENERAL_QA

            输出规则：
            1. 商品、购物车、订单、支付相关操作只做意图判断和文案生成，不伪造业务结果。
            2. 当用户提到“这个”“刚才那个”“第一个/第二个/第三个”“继续支付那个订单”之类上下文引用时，优先读取 session context 工具。
            3. searchQuery 用于真实商品检索的查询词，无法提取时输出空字符串。
            4. replyMessage 用于 GENERAL_QA 或当前轮的简洁回复，无法生成时输出空字符串。
            5. recommendationReason 仅在推荐场景生成一句推荐文案，非推荐场景输出空字符串。
            6. intent 必须严格使用枚举值，不要输出额外说明。
            """)
    ChatAiAnalysis analyze(@MemoryId String sessionId, @UserMessage String userMessage);
}
