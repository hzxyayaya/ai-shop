package com.mall.service.chat.llm;

import com.mall.dto.chat.ChatResponse;
import com.mall.model.AuthenticatedUser;
import com.mall.service.chat.intent.ChatIntent;
import com.mall.service.chat.llm.ChatAiAnalysis;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ChatActionExecutionContextRegistry {

    private final ConcurrentHashMap<String, ChatActionExecutionState> states = new ConcurrentHashMap<>();

    public ChatActionExecutionState register(
            String sessionId,
            AuthenticatedUser currentUser,
            String message,
            ChatIntent intent,
            ChatAiAnalysis aiAnalysis
    ) {
        ChatActionExecutionState state = new ChatActionExecutionState(currentUser, message, intent, aiAnalysis);
        states.put(sessionId, state);
        return state;
    }

    public ChatActionExecutionState get(String sessionId) {
        return states.get(sessionId);
    }

    public void clear(String sessionId) {
        states.remove(sessionId);
    }

    public static final class ChatActionExecutionState {

        private final AuthenticatedUser currentUser;
        private final String message;
        private final ChatIntent intent;
        private final ChatAiAnalysis aiAnalysis;
        private volatile ChatResponse response;

        private ChatActionExecutionState(
                AuthenticatedUser currentUser,
                String message,
                ChatIntent intent,
                ChatAiAnalysis aiAnalysis
        ) {
            this.currentUser = currentUser;
            this.message = message;
            this.intent = intent;
            this.aiAnalysis = aiAnalysis;
        }

        public AuthenticatedUser currentUser() {
            return currentUser;
        }

        public String message() {
            return message;
        }

        public ChatIntent intent() {
            return intent;
        }

        public ChatAiAnalysis aiAnalysis() {
            return aiAnalysis;
        }

        public ChatResponse response() {
            return response;
        }

        public void storeResponse(ChatResponse response) {
            this.response = response;
        }
    }
}
