package com.mall.service.chat.llm;

import com.mall.service.ChatSessionContextService;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.springframework.stereotype.Component;

@Component
public class ChatSessionContextTools {

    private final ChatSessionContextService chatSessionContextService;

    public ChatSessionContextTools(ChatSessionContextService chatSessionContextService) {
        this.chatSessionContextService = chatSessionContextService;
    }

    @Tool("Read the saved session context summary when the user refers to previous products, orders, or earlier turns.")
    public String readSessionContext(@ToolMemoryId String memoryId) {
        ChatSessionMemoryKey key = ChatSessionMemoryKey.parse(memoryId);
        return chatSessionContextService.summarizeContext(key.userId(), key.sessionId()).blockOptional()
                .orElse("lastIntent=, lastUserMessage=, lastProductIds=[], lastOrderNo=");
    }
}
