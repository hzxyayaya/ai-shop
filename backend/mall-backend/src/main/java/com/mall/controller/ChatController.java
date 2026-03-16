package com.mall.controller;

import com.mall.model.AuthenticatedUser;
import com.mall.security.WebFluxJwtFilter;
import com.mall.dto.chat.ChatRequest;
import com.mall.dto.chat.ChatResponse;
import com.mall.dto.chat.ChatStreamEvent;
import com.mall.service.ChatService;
import com.mall.common.exception.UnauthorizedException;
import com.mall.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public Mono<ApiResponse<ChatResponse>> chat(
            @Valid @RequestBody ChatRequest request,
            ServerWebExchange exchange
    ) {
        return chatService.chat(requireUser(exchange), request)
                .map(ApiResponse::success);
    }

    @PostMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatStreamEvent>> streamChat(
            @Valid @RequestBody ChatRequest request,
            ServerWebExchange exchange
    ) {
        return chatService.streamChat(requireUser(exchange), request)
                .map(event -> ServerSentEvent.builder(event).build());
    }

    @DeleteMapping("/{sessionId}")
    public Mono<ApiResponse<Void>> deleteSession(@PathVariable String sessionId, ServerWebExchange exchange) {
        return chatService.deleteSession(requireUser(exchange), sessionId)
                .thenReturn(ApiResponse.success("session deleted", null));
    }

    private AuthenticatedUser requireUser(ServerWebExchange exchange) {
        AuthenticatedUser authenticatedUser = exchange.getAttribute(WebFluxJwtFilter.AUTHENTICATED_USER_ATTR);
        if (authenticatedUser == null) {
            throw new UnauthorizedException("unauthorized");
        }
        return authenticatedUser;
    }
}
