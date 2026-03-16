package com.mall.config;

import com.mall.service.chat.llm.ChatSessionMemoryProvider;
import com.mall.service.chat.llm.ChatActionAiService;
import com.mall.service.chat.llm.ChatActionTools;
import com.mall.service.chat.llm.ChatAnalysisAiService;
import com.mall.service.chat.llm.ChatSessionContextTools;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.service.AiServices;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties({DeepSeekProperties.class, ZhipuEmbeddingProperties.class})
public class LangChain4jConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.ai.deepseek", name = "enabled", havingValue = "true")
    public ChatModel deepSeekChatModel(DeepSeekProperties properties) {
        return OpenAiChatModel.builder()
                .baseUrl(normalizeBaseUrl(properties.getBaseUrl()))
                .apiKey(properties.getApiKey())
                .modelName(properties.getModel())
                .temperature(0.2)
                .timeout(properties.getTimeout())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.ai.embedding.zhipu", name = "enabled", havingValue = "true")
    public EmbeddingModel zhipuEmbeddingModel(ZhipuEmbeddingProperties properties) {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(normalizeOpenAiBaseUrl(properties.getBaseUrl()))
                .apiKey(properties.getApiKey())
                .modelName(properties.getModel())
                .timeout(properties.getTimeout())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.ai.deepseek", name = "enabled", havingValue = "true")
    public ChatAnalysisAiService chatAnalysisAiService(
            ChatModel deepSeekChatModel,
            ChatSessionMemoryProvider chatSessionMemoryProvider,
            ChatSessionContextTools chatSessionContextTools
    ) {
        return AiServices.builder(ChatAnalysisAiService.class)
                .chatModel(deepSeekChatModel)
                .chatMemoryProvider(chatSessionMemoryProvider)
                .tools(chatSessionContextTools)
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.ai.deepseek", name = "enabled", havingValue = "true")
    public ChatActionAiService chatActionAiService(
            ChatModel deepSeekChatModel,
            ChatSessionMemoryProvider chatSessionMemoryProvider,
            ChatActionTools chatActionTools
    ) {
        return AiServices.builder(ChatActionAiService.class)
                .chatModel(deepSeekChatModel)
                .chatMemoryProvider(chatSessionMemoryProvider)
                .tools(chatActionTools)
                .build();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "https://api.deepseek.com/v1";
        }
        String normalized = baseUrl.trim().replaceAll("/+$", "");
        if (normalized.toLowerCase(Locale.ROOT).endsWith("/v1")) {
            return normalized;
        }
        return normalized + "/v1";
    }

    private String normalizeOpenAiBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "https://open.bigmodel.cn/api/paas/v4";
        }
        return baseUrl.trim().replaceAll("/+$", "");
    }
}
