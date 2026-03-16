package com.mall.service.chat.retrieval;

public record ProductQueryPlan(
        String query,
        boolean aiGenerated
) {
}
