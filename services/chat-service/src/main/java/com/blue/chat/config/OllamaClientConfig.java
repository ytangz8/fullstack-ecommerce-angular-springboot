package com.blue.chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OllamaClientConfig {

    @Value("${ollama.uri:http://localhost:11434}")
    private String ollamaUri;

    @Bean
    public WebClient ollamaClient(WebClient.Builder builder) {
        return builder
                .baseUrl(ollamaUri + "/api/chat")
                // Each SSE chunk is small; 256 KB per chunk is more than enough.
                .codecs(c -> c.defaultCodecs().maxInMemorySize(256 * 1024))
                .build();
    }
}
