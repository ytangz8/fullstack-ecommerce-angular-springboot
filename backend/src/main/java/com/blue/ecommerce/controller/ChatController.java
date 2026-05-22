package com.blue.ecommerce.controller;

import com.blue.ecommerce.dto.ChatMessage;
import com.blue.ecommerce.dto.ChatRequest;
import com.blue.ecommerce.dto.ChatResponse;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;

//@CrossOrigin(origins = "https://localhost:4200")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final WebClient webClient;
    private static final String OLLAMA_API_URL = "http://localhost:11434/api/chat";

    public ChatController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(OLLAMA_API_URL).build();
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@RequestBody List<ChatMessage> messages) {
        
        // Create request for Ollama
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setModel("llava:7b");
        chatRequest.setMessages(messages);
        chatRequest.setStream(true);

        // Call Ollama API and stream the response
        return webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chatRequest)
                .retrieve()
                .bodyToFlux(ChatResponse.class)
                .map(response -> {
                    // Extract the content from the response
                    String content = "";
                    if (response.getMessage() != null && response.getMessage().getContent() != null) {
                        content = response.getMessage().getContent();
                    }
                    
                    // Create SSE event
                    return ServerSentEvent.<String>builder()
                            .data(content)
                            .build();
                })
                .onErrorResume(error -> {
                    // Handle errors gracefully
                    return Flux.just(
                        ServerSentEvent.<String>builder()
                                .data("Error: " + error.getMessage())
                                .build()
                    );
                });
    }
}
