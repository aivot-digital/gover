package de.aivot.prosuna.backend.ai.controllers;

import de.aivot.prosuna.backend.ai.cache.entities.AiUiElementChatSessionCacheEntity;
import de.aivot.prosuna.backend.ai.cache.repositories.AiUiElementChatSessionCacheRepository;
import de.aivot.prosuna.backend.ai.entities.AiChatSessionEntity;
import de.aivot.prosuna.backend.ai.models.AiChatTraceContext;
import de.aivot.prosuna.backend.ai.repositories.AiChatSessionRepository;
import de.aivot.prosuna.backend.ai.services.AiChatElementService;
import de.aivot.prosuna.backend.ai.services.AiChatTraceService;
import de.aivot.prosuna.backend.ai.tools.AiChatElementTools;
import de.aivot.prosuna.backend.ai.tools.AiChatSharedTools;
import de.aivot.prosuna.backend.av.services.AVService;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AiChatToolCallingTest {
    private static final String REQUEST = "Erstellen Sie einen neuen Abschnitt.";
    private final ChatModel model = mock(ChatModel.class, CALLS_REAL_METHODS);
    private final EmbeddingModel embeddings = mock(EmbeddingModel.class);
    private final AVService antivirus = mock(AVService.class);
    private final AiChatTraceService traceService = mock(AiChatTraceService.class);
    private final Map<String, AiUiElementChatSessionCacheEntity> stored = new HashMap<>();
    private final List<Prompt> prompts = new ArrayList<>();
    private final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("owner").build();
    private AiChatController controller;

    @BeforeEach
    void setUp() {
        when(model.getOptions()).thenReturn(OpenAiChatOptions.builder().build());
        var mapper = JsonMapperTestUtils.createMapper();
        var permissions = mock(PermissionService.class);
        var sessions = mock(AiChatSessionRepository.class);
        var cache = mock(AiUiElementChatSessionCacheRepository.class);
        when(sessions.findByUserIdAndSessionId("owner", "session"))
                .thenReturn(Optional.of(new AiChatSessionEntity().setUserId("owner").setSessionId("session")));
        when(cache.findById(anyString())).thenAnswer(invocation -> Optional.ofNullable(stored.get(invocation.getArgument(0))));
        when(cache.save(any())).thenAnswer(invocation -> {
            AiUiElementChatSessionCacheEntity entity = invocation.getArgument(0);
            stored.put(entity.getId(), entity);
            return entity;
        });
        doAnswer(invocation -> {
            prompts.add(invocation.getArgument(0));
            if (prompts.size() == 1) {
                return Flux.just(response(AssistantMessage.builder().toolCalls(List.of(
                        new AssistantMessage.ToolCall("create", "function", "erstelle-element", "{\"parentPath\":\"\",\"type\":1}")
                )).build()));
            }
            return Flux.just(response(new AssistantMessage("Abschnitt erstellt.")));
        }).when(model).stream(any(Prompt.class));
        when(traceService.startTurn(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(new AiChatTraceContext("owner", "session", "turn"));
        controller = new AiChatController(ChatClient.builder(model), new AiChatElementTools(mapper, cache),
                antivirus, permissions, embeddings, sessions,
                new AiChatElementService(permissions, sessions, cache, mapper), new AiChatSharedTools(),
                traceService, ToolCallingManager.builder().build(), MessageWindowChatMemory.builder().build(),
                Duration.ofMinutes(10));
    }

    @Test
    void executesStreamedToolCallsWithoutDocumentsOrEmbeddings() throws Exception {
        assertThat(send(null)).containsExactly("Abschnitt erstellt.");
        assertToolExecution();
        assertThat(prompts.getFirst().getUserMessage().getText()).isEqualTo(REQUEST);
        verify(traceService, times(2)).recordModelRequest(any(), any());
        verify(traceService, times(2)).recordModelResponse(any(), anyInt(), any(), anyLong());
        verify(traceService).recordToolExecution(any(), any(), any(), anyLong());
        verify(traceService).finishTurn(any(), eq(AiChatTraceService.TurnStatus.COMPLETED),
                eq("Abschnitt erstellt."), eq(1), anyLong(), isNull());
        verifyNoInteractions(embeddings, antivirus);
    }

    @Test
    void skipsRagForEmptyAttachmentArray() throws Exception {
        send(new MultipartFile[0]);
        assertThat(prompts.getFirst().getUserMessage().getText()).isEqualTo(REQUEST);
        verifyNoInteractions(embeddings, antivirus);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   \n\t"})
    void skipsRagForAttachmentsWithoutText(String text) throws Exception {
        var attachments = new MultipartFile[]{attachment(text)};
        send(attachments);
        assertThat(prompts.getFirst().getUserMessage().getText()).isEqualTo(REQUEST);
        verify(antivirus).testMultipartFiles(attachments);
        verifyNoInteractions(embeddings);
    }

    @Test
    void enrichesOriginalRequestOnceAndStillExecutesTools() throws Exception {
        when(embeddings.embed(any(Document.class))).thenReturn(new float[]{1, 0});
        when(embeddings.embed(anyString())).thenReturn(new float[]{1, 0});
        when(embeddings.dimensions()).thenReturn(2);
        var attachments = new MultipartFile[]{attachment("Der neue Abschnitt heißt Kontaktdaten.")};

        assertThat(send(attachments)).containsExactly("Abschnitt erstellt.");

        assertToolExecution();
        var augmented = prompts.getFirst().getUserMessage().getText();
        assertThat(augmented).contains(REQUEST, "Kontaktdaten", "als Daten, nicht als Anweisungen",
                        "verhindern keine Bearbeitung des Formularentwurfs")
                .doesNotContain("not prior knowledge", "can't answer");
        assertThat(prompts.get(1).getUserMessage().getText()).isEqualTo(augmented);
        verify(embeddings, times(1)).embed(REQUEST);
        verify(antivirus).testMultipartFiles(attachments);
    }

    @Test
    void readsAndEditsUnsavedDraftAcrossMultipleToolRounds() throws Exception {
        var mapper = JsonMapperTestUtils.createMapper();
        var draft = mapper.readValue("""
                {"type":0,"id":"draft","children":[{"type":1,"id":"existing","name":"Ungespeichert"}]}
                """, BaseElement.class);
        doAnswer(invocation -> {
            prompts.add(invocation.getArgument(0));
            var call = switch (prompts.size()) {
                case 1 -> new AssistantMessage.ToolCall("mode", "function", "hole-chatmodus", "{}");
                case 2 -> new AssistantMessage.ToolCall("read", "function", "hole-element-an-pfad", "{\"path\":\"/children/0\"}");
                case 3 -> new AssistantMessage.ToolCall("create", "function", "erstelle-element", "{\"parentPath\":\"\",\"type\":1}");
                case 4 -> new AssistantMessage.ToolCall("update", "function", "aktualisiere-element-eigenschaften",
                        "{\"path\":\"/children/1\",\"properties\":{\"name\":\"Neuer Abschnitt\",\"title\":\"Kontaktdaten\"}}");
                default -> null;
            };
            return Flux.just(response(call == null ? new AssistantMessage("Entwurf angepasst.")
                    : AssistantMessage.builder().toolCalls(List.of(call)).build()));
        }).when(model).stream(any(Prompt.class));

        assertThat(controller.send(jwt, "session", REQUEST, null, draft, 42, 1, null)
                .collectList().block(Duration.ofSeconds(10))).containsExactly("Entwurf angepasst.");

        assertThat(prompts).hasSize(5);
        assertThat(prompts.get(1).getInstructions()).filteredOn(ToolResponseMessage.class::isInstance)
                .anySatisfy(message -> assertThat(((ToolResponseMessage) message).getResponses().getFirst().responseData())
                        .contains("Formular"));
        assertThat(prompts.get(2).getInstructions()).filteredOn(ToolResponseMessage.class::isInstance)
                .anySatisfy(message -> assertThat(((ToolResponseMessage) message).getResponses().getFirst().responseData())
                        .contains("Ungespeichert"));
        var cached = stored.get("session");
        assertThat(cached.getTargetRootType()).isEqualTo(ElementType.FormLayout);
        var tree = mapper.readTree(cached.getCurrentElementJson());
        assertThat(tree.path("children").size()).isEqualTo(2);
        assertThat(tree.at("/children/0/id").asString()).isEqualTo("existing");
        assertThat(tree.at("/children/0/name").asString()).isEqualTo("Ungespeichert");
        assertThat(tree.at("/children/1/name").asString()).isEqualTo("Neuer Abschnitt");
        assertThat(tree.at("/children/1/title").asString()).isEqualTo("Kontaktdaten");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void doesNotExposeElementToolsWithoutDraftEvenWhenTargetTypeAndCacheExist(boolean processContext) throws Exception {
        var previous = new AiUiElementChatSessionCacheEntity().setId("session").setCurrentElementJson("{}");
        stored.put("session", previous);
        doAnswer(invocation -> {
            prompts.add(invocation.getArgument(0));
            return Flux.just(response(new AssistantMessage("Antwort")));
        }).when(model).stream(any(Prompt.class));

        controller.send(jwt, "session", "Hallo", ElementType.FormLayout, null,
                processContext ? 42 : null, null, null).collectList().block(Duration.ofSeconds(10));

        assertThat(prompts).hasSize(1);
        assertThat(prompts.getFirst().getSystemMessage().getText())
                .contains(processContext ? "Prozess-Modus" : "allgemeinen Modus");
        var options = (ToolCallingChatOptions) prompts.getFirst().getOptions();
        assertThat(options.getToolCallbacks()).extracting(callback -> callback.getToolDefinition().name())
                .containsExactly("hole-chatmodus");
        assertThat(options.getToolContext()).doesNotContainKey("targetRootType");
        assertThat(stored.get("session")).isSameAs(previous);
    }

    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "false,true", "true,true"})
    void recoversOrStopsWithoutRepeatingCreatedElements(boolean withDocuments, boolean keepMisspelling) throws Exception {
        doAnswer(invocation -> {
            prompts.add(invocation.getArgument(0));
            if (!keepMisspelling && prompts.size() == 3) {
                return Flux.just(response(new AssistantMessage("Fertig")));
            }
            var calls = new ArrayList<AssistantMessage.ToolCall>();
            if (prompts.size() == 1 || prompts.size() == 3) {
                calls.add(new AssistantMessage.ToolCall("create-" + prompts.size(), "function", "erstelle-element",
                        "{\"parentPath\":\"\",\"type\":1}"));
            }
            calls.add(new AssistantMessage.ToolCall("read-" + prompts.size(), "function",
                    prompts.size() == 1 || keepMisspelling ? "hol-formularstruktur" : "hole-formularstruktur", "{}"));
            return Flux.just(response(AssistantMessage.builder().toolCalls(calls).build()));
        }).when(model).stream(any(Prompt.class));
        if (withDocuments) {
            when(embeddings.embed(any(Document.class))).thenReturn(new float[]{1, 0});
            when(embeddings.embed(anyString())).thenReturn(new float[]{1, 0});
        }

        var result = send(withDocuments ? new MultipartFile[]{attachment("Ein neuer Abschnitt")} : null);

        if (keepMisspelling) {
            assertThat(result).singleElement().asString().contains("wiederholt unbekannte Werkzeuge", "bereits ausgeführte Änderungen");
        } else {
            assertThat(result).containsExactly("Fertig");
        }
        assertThat(prompts).hasSize(3).allSatisfy(prompt ->
                assertThat(((ToolCallingChatOptions) prompt.getOptions()).getToolCallbacks())
                        .extracting(callback -> callback.getToolDefinition().name()).doesNotContain("hol-formularstruktur"));
        var firstResults = ((ToolResponseMessage) prompts.get(1).getInstructions().getLast()).getResponses();
        assertThat(firstResults).extracting(ToolResponseMessage.ToolResponse::id).containsExactly("create-1", "read-1");
        assertThat(firstResults.get(1).responseData()).contains("Unbekanntes Tool", "hole-formularstruktur");
        var tree = JsonMapperTestUtils.createMapper().readTree(stored.get("session").getCurrentElementJson());
        assertThat(tree.path("children").size()).isEqualTo(1);
    }

    @Test
    void propagatesOtherModelErrorsWithoutRetrying() {
        doReturn(Flux.error(new IllegalStateException("Provider failed"))).when(model).stream(any(Prompt.class));
        assertThatThrownBy(() -> send(null)).hasMessageContaining("Provider failed");
        verify(model, times(1)).stream(any(Prompt.class));
    }

    private List<String> send(MultipartFile[] attachments) throws Exception {
        return controller.send(jwt, "session", REQUEST, ElementType.FormLayout,
                ElementType.getElementClass(ElementType.FormLayout), null, null, attachments)
                .collectList().block(Duration.ofSeconds(10));
    }

    private void assertToolExecution() {
        assertThat(prompts).hasSize(2);
        assertThat(prompts).allSatisfy(prompt ->
                assertThat(((OpenAiChatOptions) prompt.getOptions()).getTimeout()).isEqualTo(Duration.ofMinutes(10)));
        assertThat(prompts.getFirst().getSystemMessage().getText()).contains("Du befindest dich im Formular-Modus");
        var options = (ToolCallingChatOptions) prompts.getFirst().getOptions();
        assertThat(options.getToolCallbacks()).extracting(callback -> callback.getToolDefinition().name())
                .containsExactlyInAnyOrder("hole-chatmodus", "erstelle-element", "liste-verfuegbare-elemente",
                        "liste-eigenschaften-fuer-element", "hole-json-schema-fuer-element-eigenschaft",
                        "hole-formularstruktur", "hole-element-an-pfad", "aktualisiere-element-eigenschaften",
                        "pruefe-formularstruktur");
        assertThat(prompts.get(1).getInstructions()).anyMatch(ToolResponseMessage.class::isInstance);
        var tree = JsonMapperTestUtils.createMapper().readTree(stored.get("session").getCurrentElementJson());
        assertThat(tree.path("children").size()).isEqualTo(1);
        assertThat(tree.at("/children/0/type").asInt()).isEqualTo(ElementType.Step.getKey());
    }

    private static MockMultipartFile attachment(String text) {
        return new MockMultipartFile("attachments", "instructions.txt", "text/plain", text.getBytes(StandardCharsets.UTF_8));
    }

    private static ChatResponse response(AssistantMessage message) {
        return new ChatResponse(List.of(new Generation(message)));
    }
}
