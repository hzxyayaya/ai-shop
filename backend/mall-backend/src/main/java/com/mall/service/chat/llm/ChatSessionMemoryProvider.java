package com.mall.service.chat.llm;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import com.mall.service.ChatMessageHistoryService;
import dev.langchain4j.data.message.ChatMessage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ChatSessionMemoryProvider implements ChatMemoryProvider {

    private static final int MAX_MESSAGES = 12;

    private final ChatMessageHistoryService chatMessageHistoryService;
    private final Map<Object, ChatMemory> memories = new ConcurrentHashMap<>();

    public ChatSessionMemoryProvider(ChatMessageHistoryService chatMessageHistoryService) {
        this.chatMessageHistoryService = chatMessageHistoryService;
    }

    @Override
    public ChatMemory get(Object memoryId) {
        return memories.computeIfAbsent(memoryId, id -> new SessionScopedChatMemory(id, chatMessageHistoryService));
    }

    public void evict(String memoryId) {
        memories.remove(memoryId);
    }

    private static final class SessionScopedChatMemory implements ChatMemory {

        private final Object id;
        private final ChatMessageHistoryService chatMessageHistoryService;
        private final LinkedList<ChatMessage> messages = new LinkedList<>();
        private boolean initialized;

        private SessionScopedChatMemory(Object id, ChatMessageHistoryService chatMessageHistoryService) {
            this.id = id;
            this.chatMessageHistoryService = chatMessageHistoryService;
        }

        @Override
        public Object id() {
            return id;
        }

        @Override
        public synchronized void add(ChatMessage message) {
            initializeIfNeeded();
            messages.add(message);
            while (messages.size() > MAX_MESSAGES) {
                messages.removeFirst();
            }
        }

        @Override
        public synchronized List<ChatMessage> messages() {
            initializeIfNeeded();
            return new ArrayList<>(messages);
        }

        @Override
        public synchronized void clear() {
            messages.clear();
            initialized = true;
        }

        private void initializeIfNeeded() {
            if (initialized) {
                return;
            }
            messages.clear();
            ChatSessionMemoryKey key = ChatSessionMemoryKey.parse(String.valueOf(id));
            messages.addAll(chatMessageHistoryService.loadRecentMessagesBlocking(key.userId(), key.sessionId()));
            while (messages.size() > MAX_MESSAGES) {
                messages.removeFirst();
            }
            initialized = true;
        }
    }
}
