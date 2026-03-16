package com.mall.service.chat.retrieval;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class ChatEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(ChatEmbeddingService.class);

    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;

    public ChatEmbeddingService(ObjectProvider<EmbeddingModel> embeddingModelProvider) {
        this.embeddingModelProvider = embeddingModelProvider;
    }

    public Mono<String> embedToPgVectorLiteral(String text) {
        if (!StringUtils.hasText(text)) {
            return Mono.empty();
        }

        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        if (embeddingModel == null) {
            log.warn("EmbeddingModel bean not available, fallback to keyword retrieval");
            return Mono.empty();
        }

        return Mono.fromCallable(() -> {
                    Embedding embedding = embeddingModel.embed(text).content();
                    if (embedding == null || embedding.vector() == null || embedding.vector().length == 0) {
                        return null;
                    }
                    return toPgVectorLiteral(embedding.vector());
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(vector -> StringUtils.hasText(vector) ? Mono.just(vector) : Mono.empty())
                .doOnSuccess(vector -> {
                    if (StringUtils.hasText(vector)) {
                        log.debug("Embedding generated successfully, vector literal length={}", vector.length());
                    }
                })
                .onErrorResume(ex -> {
                    log.warn("Embedding generation failed, fallback to keyword retrieval: {}", ex.getMessage());
                    return Mono.empty();
                });
    }

    private String toPgVectorLiteral(float[] vector) {
        StringBuilder builder = new StringBuilder(vector.length * 10);
        builder.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(String.format(Locale.ROOT, "%f", vector[i]));
        }
        builder.append(']');
        return builder.toString();
    }
}
