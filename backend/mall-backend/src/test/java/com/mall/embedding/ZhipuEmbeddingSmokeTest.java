package com.mall.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import java.time.Duration;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

class ZhipuEmbeddingSmokeTest {

    @Test
    void shouldCallZhipuEmbeddingApiAndReturnVector() {
        String apiKey = firstNonBlank(System.getenv("ZHIPU_API_KEY"), System.getProperty("zhipu.api.key"));
        Assumptions.assumeTrue(StringUtils.hasText(apiKey), "ZHIPU_API_KEY not set, skip smoke test");

        String baseUrl = firstNonBlank(
                System.getenv("ZHIPU_EMBEDDING_BASE_URL"),
                System.getProperty("zhipu.embedding.base-url"),
                "https://open.bigmodel.cn/api/paas/v4"
        );
        String modelName = firstNonBlank(
                System.getenv("ZHIPU_EMBEDDING_MODEL"),
                System.getProperty("zhipu.embedding.model"),
                "embedding-3"
        );

        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .baseUrl(trimTrailingSlash(baseUrl))
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(20))
                .build();

        Embedding embedding = embeddingModel.embed("帮我推荐一款适合通勤的手机").content();

        assertThat(embedding).isNotNull();
        assertThat(embedding.vector()).isNotNull();
        assertThat(embedding.vector().length).isGreaterThan(0);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String trimTrailingSlash(String url) {
        return url.trim().replaceAll("/+$", "");
    }
}
