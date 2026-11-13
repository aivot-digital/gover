package de.aivot.prosuna.backend.ai.controllers;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.aivot.prosuna.backend.ai.models.AiChatTraceContext;
import de.aivot.prosuna.backend.ai.repositories.AiChatSessionRepository;
import de.aivot.prosuna.backend.ai.services.AiChatElementService;
import de.aivot.prosuna.backend.ai.services.AiChatTraceService;
import de.aivot.prosuna.backend.ai.services.AiChatAttachmentService;
import de.aivot.prosuna.backend.ai.tools.AiChatElementTools;
import de.aivot.prosuna.backend.ai.tools.AiChatSharedTools;
import de.aivot.prosuna.backend.ai.tools.AiChatProcessTools;
import de.aivot.prosuna.backend.ai.tools.AiChatElementSchemaTools;
import de.aivot.prosuna.backend.ai.tools.AiChatAttachmentTools;
import de.aivot.prosuna.backend.ai.services.AiChatProcessService;
import de.aivot.prosuna.backend.ai.services.AiInputValueSchemaService;
import de.aivot.prosuna.backend.av.services.AVService;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiChatRequestTimeoutTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> {
                context.getBeanFactory().setConversionService(ApplicationConversionService.getSharedInstance());
                var sources = context.getEnvironment().getPropertySources();
                sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
                sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
                try {
                    new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"))
                            .forEach(sources::addLast);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            })
            .withBean(AiChatController.class)
            .withBean(ChatMemory.class, () -> MessageWindowChatMemory.builder().build())
            .withBean(ToolCallingManager.class, () -> ToolCallingManager.builder().build())
            .withBean(AiChatElementTools.class, () -> mock(AiChatElementTools.class))
            .withBean(AiChatSharedTools.class)
            .withBean(AiChatElementSchemaTools.class, () -> mock(AiChatElementSchemaTools.class))
            .withBean(AiChatProcessTools.class, () -> mock(AiChatProcessTools.class))
            .withBean(AiChatProcessService.class, () -> mock(AiChatProcessService.class))
            .withBean(AiChatAttachmentService.class, () -> mock(AiChatAttachmentService.class))
            .withBean(AiChatAttachmentTools.class, () -> mock(AiChatAttachmentTools.class))
            .withBean(AVService.class, () -> mock(AVService.class))
            .withBean(PermissionService.class, () -> mock(PermissionService.class))
            .withBean(EmbeddingModel.class, () -> mock(EmbeddingModel.class))
            .withBean(AiChatSessionRepository.class, () -> mock(AiChatSessionRepository.class))
            .withBean(AiChatElementService.class, () -> mock(AiChatElementService.class))
            .withBean(AiChatTraceService.class, AiChatRequestTimeoutTest::traceService);

    private static AiChatTraceService traceService() {
        var service = mock(AiChatTraceService.class);
        when(service.startTurn(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(new AiChatTraceContext("owner", "session", "turn"));
        return service;
    }

    @Test
    void passesDefaultTenMinutesToTheModelWithoutReplacingOtherOptions() {
        assertPromptTimeout(contextRunner, Duration.ofMinutes(10));
    }

    @Test
    void passesConfiguredDurationToTheModel() {
        assertPromptTimeout(contextRunner.withPropertyValues("PROSUNA_OPENAI_CHAT_TIMEOUT=120s"), Duration.ofSeconds(120));
    }

    @Test
    void usesConfiguredTimeoutForTheActualStreamingHttpRequest() throws IOException {
        var requestTimeout = new AtomicReference<String>();
        withServer(exchange -> {
            try (exchange) {
                exchange.getRequestBody().readAllBytes();
                // Spring's transport writes this header from the final per-call OkHttp configuration.
                requestTimeout.set(exchange.getRequestHeaders().getFirst("X-Stainless-Timeout"));
                exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().write(("""
                        data: {"id":"test","object":"chat.completion.chunk","model":"test-model","choices":[{"index":0,"delta":{"role":"assistant","content":"Antwort"},"finish_reason":null}]}

                        data: {"id":"test","object":"chat.completion.chunk","model":"test-model","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

                        data: [DONE]

                        """).getBytes(StandardCharsets.UTF_8));
            }
        }, baseUrl -> httpContext(baseUrl)
                .withPropertyValues("PROSUNA_OPENAI_CHAT_TIMEOUT=120s")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(send(context.getBean(AiChatController.class))).containsExactly("Antwort");
                    assertThat(requestTimeout.get()).isEqualTo("120");
                }));
    }

    @Test
    void abortsAStreamingRequestThatDoesNotReceiveResponseHeadersInTime() throws IOException {
        var releaseResponse = new CountDownLatch(1);
        withServer(exchange -> {
            try (exchange) {
                exchange.getRequestBody().readAllBytes();
                // Hold the response until the assertion completes, without a slow fixed sleep.
                releaseResponse.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, baseUrl -> {
            try {
                httpContext(baseUrl).withPropertyValues("PROSUNA_OPENAI_CHAT_TIMEOUT=500ms")
                        .run(context -> {
                            assertThat(context).hasNotFailed();
                            assertThatThrownBy(() -> send(context.getBean(AiChatController.class)))
                                    .satisfies(error -> assertThat(Stream.iterate(error, Objects::nonNull, Throwable::getCause)
                                            .anyMatch(InterruptedIOException.class::isInstance))
                                            .as("HTTP timeout, not the test's blocking deadline: %s", error)
                                            .isTrue());
                        });
            } finally {
                releaseResponse.countDown();
            }
        });
    }

    private void assertPromptTimeout(ApplicationContextRunner runner, Duration expected) {
        var model = mock(ChatModel.class);
        when(model.getOptions()).thenReturn(OpenAiChatOptions.builder()
                .model("test-model").temperature(0.25).build());
        var receivedPrompt = new AtomicReference<Prompt>();
        when(model.stream(any(Prompt.class))).thenAnswer(invocation -> {
            receivedPrompt.set(invocation.getArgument(0));
            return Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage("Antwort")))));
        });
        runner.withBean(ChatClient.Builder.class, () -> ChatClient.builder(model)).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(send(context.getBean(AiChatController.class))).containsExactly("Antwort");
            var options = (OpenAiChatOptions) receivedPrompt.get().getOptions();
            assertThat(options.getTimeout()).isEqualTo(expected);
            assertThat(options.getModel()).isEqualTo("test-model");
            assertThat(options.getTemperature()).isEqualTo(0.25);
            assertThat(options.getToolCallbacks()).extracting(callback -> callback.getToolDefinition().name())
                    .containsExactly("hole-chatmodus");
            assertThat(options.getToolContext()).containsEntry("sessionId", "session");
        });
    }

    private ApplicationContextRunner httpContext(String baseUrl) {
        return contextRunner.withConfiguration(AutoConfigurations.of(OpenAiChatAutoConfiguration.class))
                .withUserConfiguration(HttpChatClientConfiguration.class)
                .withPropertyValues("spring.ai.openai.api-key=test-key", "spring.ai.openai.base-url=" + baseUrl,
                        "spring.ai.openai.chat.model=test-model", "spring.ai.openai.chat.max-retries=0");
    }

    private static List<String> send(AiChatController controller) throws Exception {
        var jwt = Jwt.withTokenValue("token").header("alg", "none").subject("owner").build();
        return controller.send(jwt, "session", "Hallo", null, null, null, null, null)
                .collectList().block(Duration.ofSeconds(5));
    }

    private static void withServer(HttpHandler handler, Consumer<String> test) throws IOException {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            try {
                server.setExecutor(executor);
                server.createContext("/v1/chat/completions", handler);
                server.start();
                test.accept("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
            } finally {
                server.stop(0);
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class HttpChatClientConfiguration {
        @Bean
        ChatClient.Builder chatClientBuilder(OpenAiChatModel model) {
            return ChatClient.builder(model);
        }
    }
}
