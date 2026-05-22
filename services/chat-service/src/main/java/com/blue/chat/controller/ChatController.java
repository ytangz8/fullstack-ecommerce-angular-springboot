package com.blue.chat.controller;

import com.blue.chat.dto.ChatMessage;
import com.blue.chat.dto.ChatRequest;
import com.blue.chat.dto.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final WebClient ollamaClient;

    @Value("${ollama.model:llava:7b}")
    private String model;

    public ChatController(WebClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@RequestBody List<ChatMessage> messages) {
        ChatRequest request = new ChatRequest(model, messages, true);

        return ollamaClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(ChatResponse.class)
                .mapNotNull(response -> {
                    if (response.getMessage() == null
                            || response.getMessage().getContent() == null) {
                        return null;
                    }
                    return ServerSentEvent.<String>builder()
                            .data(response.getMessage().getContent())
                            .build();
                })
                .onErrorResume(error -> Flux.just(
                        ServerSentEvent.<String>builder()
                                .data("Error: " + error.getMessage())
                                .build()
                ));
    }
}
