package com.blue.chat.controller;

import com.blue.chat.dto.ChatMessage;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatControllerIntegrationTest {

    /**
     * MockWebServer must be started in a static initialiser — before Spring
     * creates the application context — so that {@code @DynamicPropertySource}
     * can read the actual port and inject it as {@code ollama.uri}.
     */
    private static final MockWebServer MOCK_OLLAMA;

    static {
        try {
            MOCK_OLLAMA = new MockWebServer();
            MOCK_OLLAMA.start();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void overrideOllamaUri(DynamicPropertyRegistry registry) {
        registry.add("ollama.uri", () -> "http://localhost:" + MOCK_OLLAMA.getPort());
    }

    @Autowired
    private WebTestClient webTestClient;

    @AfterAll
    static void shutdownMockServer() throws IOException {
        MOCK_OLLAMA.shutdown();
    }

    // ─── helpers ────────────────────────────────────────────────────────────────

    /**
     * Enqueues an NDJSON Ollama streaming response.
     * Each {@code token} becomes one {@code done:false} line.
     * A final {@code done:true} sentinel (message:null) is appended;
     * the controller's {@code mapNotNull} filters it out transparently.
     */
    private static void enqueueOllamaStream(String... tokens) {
        StringBuilder body = new StringBuilder();
        for (String token : tokens) {
            body.append("{\"model\":\"llava:7b\",")
                .append("\"message\":{\"role\":\"assistant\",\"content\":\"")
                .append(token).append("\"},")
                .append("\"done\":false}\n");
        }
        body.append("{\"model\":\"llava:7b\",\"message\":null,\"done\":true}\n");

        MOCK_OLLAMA.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(body.toString()));
    }

    // ─── Test 1: happy-path SSE streaming ───────────────────────────────────────

    /**
     * POST /api/chat/stream with Accept: text/event-stream must return HTTP 200,
     * Content-Type: text/event-stream, and stream one SSE event per Ollama token.
     */
    @Test
    void streamChat_withValidSseRequest_returns200AndStreamsTokensAsEvents() {
        enqueueOllamaStream("Hello", "World");

        FluxExchangeResult<ServerSentEvent<String>> result = webTestClient.post()
                .uri("/api/chat/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(List.of(new ChatMessage("user", "Hi")))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .returnResult(new ParameterizedTypeReference<ServerSentEvent<String>>() {});

        StepVerifier.create(result.getResponseBody())
                .assertNext(e -> assertThat(e.data()).isEqualTo("Hello"))
                .assertNext(e -> assertThat(e.data()).isEqualTo("World"))
                .verifyComplete();
    }

    // ─── Test 2: wrong HTTP method → 405 JSON, not Whitelabel ───────────────────

    /**
     * GET /api/chat/stream is illegal (endpoint is POST-only).
     * Spring WebFlux must return 405 with a structured JSON error body —
     * NOT the Spring MVC "Whitelabel Error Page".
     */
    @Test
    void streamChat_withGetMethod_returns405AndJsonErrorBody() {
        webTestClient.get()
                .uri("/api/chat/stream")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(405)
                .jsonPath("$.error").isEqualTo("Method Not Allowed")
                .jsonPath("$.path").isEqualTo("/api/chat/stream");
    }

    // ─── Test 3: wrong Accept header → 406 JSON, not Whitelabel ─────────────────

    /**
     * POST with Accept: application/json must be rejected with 406 because the
     * endpoint declares {@code produces = text/event-stream} only.
     * The error body must be structured JSON, not a Whitelabel page.
     */
    @Test
    void streamChat_withUnsupportedAcceptHeader_returns406AndJsonErrorBody() {
        webTestClient.post()
                .uri("/api/chat/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(new ChatMessage("user", "Hi")))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_ACCEPTABLE)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(406)
                .jsonPath("$.path").isEqualTo("/api/chat/stream");
    }
}
