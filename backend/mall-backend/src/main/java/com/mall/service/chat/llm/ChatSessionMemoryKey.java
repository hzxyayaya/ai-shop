package com.mall.service.chat.llm;

public record ChatSessionMemoryKey(Long userId, String sessionId) {

    private static final String DELIMITER = "::";

    public static String build(Long userId, String sessionId) {
        return userId + DELIMITER + sessionId;
    }

    public static ChatSessionMemoryKey parse(String memoryId) {
        if (memoryId == null || memoryId.isBlank()) {
            throw new IllegalArgumentException("memoryId must not be blank");
        }
        int delimiterIndex = memoryId.indexOf(DELIMITER);
        if (delimiterIndex <= 0 || delimiterIndex >= memoryId.length() - DELIMITER.length()) {
            throw new IllegalArgumentException("invalid memoryId");
        }
        Long userId = Long.parseLong(memoryId.substring(0, delimiterIndex));
        String sessionId = memoryId.substring(delimiterIndex + DELIMITER.length());
        if (sessionId.isBlank()) {
            throw new IllegalArgumentException("invalid memoryId");
        }
        return new ChatSessionMemoryKey(userId, sessionId);
    }
}