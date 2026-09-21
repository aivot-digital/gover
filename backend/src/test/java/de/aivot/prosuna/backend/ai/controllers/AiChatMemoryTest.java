package de.aivot.prosuna.backend.ai.controllers;

import de.aivot.prosuna.backend.ai.cache.repositories.AiUiElementChatSessionCacheRepository;
import de.aivot.prosuna.backend.ai.configuration.AiChatMemoryConfiguration;
import de.aivot.prosuna.backend.ai.entities.AiChatSessionEntity;
import de.aivot.prosuna.backend.ai.models.AiChatTraceContext;
import de.aivot.prosuna.backend.ai.repositories.AiChatSessionRepository;
import de.aivot.prosuna.backend.ai.repositories.DatabaseChatMemoryRepository;
import de.aivot.prosuna.backend.ai.services.AiChatElementService;
import de.aivot.prosuna.backend.ai.services.AiChatTraceService;
import de.aivot.prosuna.backend.ai.tools.AiChatElementTools;
import de.aivot.prosuna.backend.ai.tools.AiChatSharedTools;
import de.aivot.prosuna.backend.ai.tools.AiChatProcessTools;
import de.aivot.prosuna.backend.ai.tools.AiChatElementSchemaTools;
import de.aivot.prosuna.backend.ai.services.AiChatProcessService;
import de.aivot.prosuna.backend.ai.services.AiInputValueSchemaService;
import de.aivot.prosuna.backend.av.services.AVService;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiChatMemoryTest {
    private final ChatModel model = mock(ChatModel.class, CALLS_REAL_METHODS);
    private final AiChatSessionRepository sessions = mock(AiChatSessionRepository.class);
    private final PermissionService permissions = mock(PermissionService.class);
    private final EmbeddingModel embeddings = mock(EmbeddingModel.class);
    private final AiChatTraceService traceService = mock(AiChatTraceService.class);
    private final Map<String, AiChatSessionEntity> stored = new HashMap<>();
    private final List<Prompt> prompts = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(model.getOptions()).thenReturn(OpenAiChatOptions.builder().build());
        when(sessions.findByUserIdAndSessionId(anyString(), anyString())).thenAnswer(invocation -> Optional.of(
                session(invocation.getArgument(0), invocation.getArgument(1))));
        when(sessions.findByUserIdAndSessionIdForUpdate(anyString(), anyString())).thenAnswer(invocation -> Optional.of(
                session(invocation.getArgument(0), invocation.getArgument(1))));
        when(sessions.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(traceService.startTurn(anyString(), anyString(), anyString(), any(), any()))
                .thenAnswer(invocation -> new AiChatTraceContext(
                        invocation.getArgument(0), invocation.getArgument(1), "turn"));
        doAnswer(invocation -> {
            prompts.add(invocation.getArgument(0));
            return Flux.just(response(new AssistantMessage("Antwort ")), response(new AssistantMessage("eins")));
        }).when(model).stream(any(Prompt.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void includesPreviousTurnOnceEvenAfterRecreatingMemory(boolean recreateController) throws Exception {
        var controller = controller();
        send(controller, "owner", "session", "Mein Hund heißt Bello", null);
        send(recreateController ? controller() : controller, "owner", "session", "Wie heißt mein Hund?", null);

        assertThat(prompts.get(1).getInstructions().stream().filter(UserMessage.class::isInstance).map(message -> message.getText()))
                .containsExactly("Mein Hund heißt Bello", "Wie heißt mein Hund?");
        assertThat(prompts.get(1).getInstructions().stream().filter(AssistantMessage.class::isInstance).map(message -> message.getText()))
                .containsExactly("Antwort eins");
        assertThat(repository().findByConversationId("owner:session")).containsExactly(
                new UserMessage("Mein Hund heißt Bello"), new AssistantMessage("Antwort eins"),
                new UserMessage("Wie heißt mein Hund?"), new AssistantMessage("Antwort eins"));
    }

    @Test
    void keepsUsersAndSessionsSeparate() throws Exception {
        var controller = controller();
        send(controller, "owner", "session", "Geheim", null);
        send(controller, "other", "session", "Andere Person", null);
        send(controller, "owner", "other-session", "Andere Sitzung", null);

        assertThat(prompts.subList(1, 3)).allSatisfy(prompt ->
                assertThat(prompt.getInstructions().stream().filter(UserMessage.class::isInstance)).hasSize(1));
        assertThat(stored).containsOnlyKeys("owner:session", "other:session", "owner:other-session");
    }

    @Test
    void checksSessionOwnershipAndPermissionBeforeAccessingMemory() throws Exception {
        var controller = controller();
        when(sessions.findByUserIdAndSessionId("owner", "foreign")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> send(controller, "owner", "foreign", "Text", null)).isInstanceOf(ResponseException.class);
        doThrow(ResponseException.forbidden()).when(permissions).requireSystemPermission(anyString(), anyString());
        assertThatThrownBy(() -> send(controller, "owner", "session", "Text", null)).isInstanceOf(ResponseException.class);
        verify(sessions, never()).findByUserIdAndSessionIdForUpdate(anyString(), anyString());
        verify(model, never()).stream(any(Prompt.class));
    }

    @Test
    void persistsOriginalUserTextAndFinalAnswerWithoutToolOrDocumentData() throws Exception {
        when(embeddings.embed(any(Document.class))).thenReturn(new float[]{1, 0});
        when(embeddings.embed(anyString())).thenReturn(new float[]{1, 0});
        doAnswer(invocation -> {
            prompts.add(invocation.getArgument(0));
            if (prompts.size() == 1) {
                return Flux.just(response(AssistantMessage.builder().toolCalls(List.of(
                        new AssistantMessage.ToolCall("mode", "function", "hole-chatmodus", "{}"))).build()));
            }
            return Flux.just(response(AssistantMessage.builder().content("Fertig")
                    .properties(Map.of("reasoningContent", "Private reasoning")).build()));
        }).when(model).stream(any(Prompt.class));
        var document = new MockMultipartFile("attachments", "document.txt", "text/plain",
                "Dokumentinhalt zur Anfrage".getBytes(StandardCharsets.UTF_8));

        send(controller(), "owner", "session", "Auftrag", new MultipartFile[]{document});

        assertThat(prompts).hasSize(2);
        assertThat(prompts.getFirst().getUserMessage().getText()).contains("Dokumentinhalt");
        assertThat(repository().findByConversationId("owner:session")).containsExactly(new UserMessage("Auftrag"), new AssistantMessage("Fertig"));
        assertThat(stored.get("owner:session").getChatMessages()).containsExactly(
                Map.of("role", "user", "content", "Auftrag"),
                Map.of("role", "assistant", "content", "Fertig"));
    }

    @Test
    void doesNotPersistAssistantAnswerForFailedStream() {
        doReturn(Flux.error(new IllegalStateException("Provider failed"))).when(model).stream(any(Prompt.class));

        assertThatThrownBy(() -> send(controller(), "owner", "session", "Auftrag", null)).hasMessageContaining("Provider failed");
        assertThat(repository().findByConversationId("owner:session")).containsExactly(new UserMessage("Auftrag"));
        verify(traceService).finishTurn(any(), eq(AiChatTraceService.TurnStatus.FAILED), eq(""), eq(0), anyLong(),
                isA(IllegalStateException.class));
    }

    @Test
    void recordsAnEmptyModelStreamAsAnExplicitTerminalState() throws Exception {
        doReturn(Flux.just(response(new AssistantMessage("")))).when(model).stream(any(Prompt.class));

        assertThat(send(controller(), "owner", "session", "Auftrag", null)).isEmpty();

        verify(traceService).finishTurn(any(), eq(AiChatTraceService.TurnStatus.EMPTY_RESPONSE), eq(""), eq(0),
                anyLong(), isNull());
    }

    @Test
    void propagatesClientCancellationToTheModelStreamAndTrace() throws Exception {
        var subscribed = new CountDownLatch(1);
        var cancelled = new CountDownLatch(1);
        doReturn(Flux.<ChatResponse>never()
                .doOnSubscribe(ignored -> subscribed.countDown())
                .doOnCancel(cancelled::countDown))
                .when(model).stream(any(Prompt.class));
        var jwt = Jwt.withTokenValue("token").header("alg", "none").subject("owner").build();
        var subscription = controller().send(jwt, "session", "Auftrag", null, null, null, null, null)
                .subscribe();

        assertThat(subscribed.await(1, TimeUnit.SECONDS)).isTrue();
        subscription.dispose();

        assertThat(cancelled.await(1, TimeUnit.SECONDS)).isTrue();
        verify(traceService, timeout(1_000)).recordModelCancellation(any(), anyInt(), anyLong());
        verify(traceService, timeout(1_000)).finishTurn(any(), eq(AiChatTraceService.TurnStatus.CANCELLED),
                eq(""), eq(0), anyLong(), isNull());
    }

    private DatabaseChatMemoryRepository repository() {
        return new DatabaseChatMemoryRepository(sessions);
    }

    private AiChatController controller() {
        var mapper = JsonMapperTestUtils.createMapper();
        var elements = mock(AiUiElementChatSessionCacheRepository.class);
        return new AiChatController(ChatClient.builder(model), new AiChatElementTools(mapper, elements), mock(AVService.class),
                permissions, embeddings, sessions, new AiChatElementService(permissions, sessions, elements, mapper),
                new AiChatSharedTools(), new AiChatElementSchemaTools(mapper, new AiInputValueSchemaService(mapper)), new AiChatProcessTools(mock(AiChatProcessService.class)), mock(AiChatProcessService.class), traceService, ToolCallingManager.builder().build(),
                new AiChatMemoryConfiguration().chatMemory(repository()), Duration.ofMinutes(10));
    }

    private AiChatSessionEntity session(String userId, String sessionId) {
        return stored.computeIfAbsent(DatabaseChatMemoryRepository.conversationId(userId, sessionId), ignored ->
                new AiChatSessionEntity().setUserId(userId).setSessionId(sessionId));
    }

    private List<String> send(AiChatController controller, String owner, String session, String text, MultipartFile[] files) throws Exception {
        var jwt = Jwt.withTokenValue("token").header("alg", "none").subject(owner).build();
        return controller.send(jwt, session, text, null, null, null, null, files)
                .collectList().block(Duration.ofSeconds(10));
    }

    private static ChatResponse response(AssistantMessage message) {
        return new ChatResponse(List.of(new Generation(message)));
    }
}
