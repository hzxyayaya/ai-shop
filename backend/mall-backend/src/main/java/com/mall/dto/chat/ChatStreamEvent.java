package com.mall.dto.chat;

public record ChatStreamEvent(
        String type,
        String stage,
        String message,
        String delta,
        ChatResponse response
) {

    public static ChatStreamEvent status(String message) {
        return new ChatStreamEvent("status", null, message, null, null);
    }

    public static ChatStreamEvent stage(String stage, String message) {
        return new ChatStreamEvent("stage", stage, message, null, null);
    }

    public static ChatStreamEvent messageDelta(String delta) {
        return new ChatStreamEvent("message_delta", null, null, delta, null);
    }

    public static ChatStreamEvent complete(ChatResponse response) {
        return new ChatStreamEvent("complete", "DONE", null, null, response);
    }

    public static ChatStreamEvent error(String message) {
        return new ChatStreamEvent("error", "FAILED", message, null, null);
    }
}
