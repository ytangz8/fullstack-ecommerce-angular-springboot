# AI Workflow

## What the Project Does

Blue's Bakery is a full-stack e-commerce web application built with Angular (frontend) and
Spring Boot (backend). It supports product browsing by category and keyword search, a shopping
cart, Stripe-based checkout, Okta authentication, order history, and an AI chat assistant
powered by a local LLM (Ollama/llava:7b) served over SSE.

The project has been progressively refactored from a Spring Boot monolith into a microservices
architecture using the Strangler Fig pattern. Completed phases include:

- **Phase 0**: Spring Cloud Gateway as a unified entry point with CORS and HTTPS termination
- **Phase 1**: chat-service extracted (WebFlux reactive stack, SSE streaming)
- **Phase 2**: geo-service extracted (static country/state reference data)
- **Phase 3**: catalog-service extracted (product catalog, Spring Data REST, HAL)

Remaining services (order, customer, payment) are planned for future phases.

## Why I Chose This Project

I wanted hands-on experience connecting a real frontend to a real backend — handling auth,
payments, file-based sessions, and database-backed APIs together rather than in isolation. The
transition to microservices added a second dimension: learning how to decompose a working
system safely without breaking existing behaviour, which mirrors what production engineering
teams actually do.

## Tools Used

| Layer | Technology |
|-------|-----------|
| Frontend | Angular 17, TypeScript, Bootstrap |
| Backend (monolith) | Spring Boot 3, Spring Data REST, Spring Security, Spring WebFlux |
| Microservices | Spring Cloud Gateway, Spring Boot per service, Spring WebFlux (chat), Spring Data JPA |
| Auth | Okta OIDC (monolith), JWT header forwarding (gateway) |
| Payments | Stripe Java SDK |
| AI chat | Ollama (llava:7b), local WebClient proxy over SSE |
| Database | H2 (dev/test), MySQL connector wired for production |
| Containerisation | Docker Compose (multi-service orchestration) |
| Testing | JUnit 5, Spring Boot Test, WebTestClient (integration tests per service) |
| Build | Maven (multi-module layout under `services/`) |

## How AI Tools Were Used

**Claude Code (Anthropic)** was the primary AI tool throughout the project.

**UI redesign**: The original template-based frontend was restyled by prompting Claude Code to
redesign the Angular components with a modern layout. Claude generated component HTML/CSS
changes and I reviewed diffs before accepting them.

**Architecture planning**: Before starting the microservices refactor, I asked Claude Code to
analyse the existing entity graph, identify natural seam lines (e.g. `OrderItem.productId`
already being a bare `Long` with no JPA foreign key to `Product`), and produce a phased
migration plan. The result became `ARCHITECTURE.md` and drove the phase-by-phase extraction
sequence.

**Service extraction**: For each new microservice I described the target scope and Claude Code
generated the initial Spring Boot project scaffold — `pom.xml`, entity/repository/controller
classes, `application.properties`, and Docker Compose entries. I then reviewed, adjusted, and
ran the service to verify behaviour.

**Integration tests**: After extracting each service, Claude Code wrote `@SpringBootTest`
integration tests using `WebTestClient` and H2. This caught a content-negotiation bug in
chat-service (SSE `text/event-stream` vs `application/json`) before it reached any manual
testing.

**Debugging**: When the Spring Cloud Gateway CORS configuration conflicted with the individual
service CORS filters, I pasted the error and relevant config into Claude Code and it identified
the duplicate filter issue and proposed removing downstream CORS in favour of a single gateway
policy.

**What I kept control of**: Decisions about service boundaries, which phase to tackle next,
whether to accept or modify generated code, and all git commits. AI accelerated implementation
and test writing; architectural trade-offs remained mine.
