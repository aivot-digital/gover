package de.aivot.prosuna.backend.ai.services;

import de.aivot.prosuna.backend.ai.entities.AiChatSessionEntity;
import de.aivot.prosuna.backend.ai.models.ChatContextModel;
import de.aivot.prosuna.backend.ai.models.AiChatAttachmentContext;
import de.aivot.prosuna.backend.ai.tools.AiChatAttachmentTools;
import de.aivot.prosuna.backend.ai.permissions.AiChatPermissionProvider;
import de.aivot.prosuna.backend.ai.properties.AiChatTraceProperties;
import de.aivot.prosuna.backend.ai.repositories.AiChatSessionRepository;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiChatTraceServiceTest {
    private final AiChatTracePersistenceService persistence = mock(AiChatTracePersistenceService.class);
    private final AiChatSessionRepository sessions = mock(AiChatSessionRepository.class);
    private final PermissionService permissions = mock(PermissionService.class);
    private final AiChatTraceProperties properties = new AiChatTraceProperties();
    private final AiChatTraceService service = new AiChatTraceService(
            persistence, sessions, permissions, properties, JsonMapperTestUtils.createMapper());

    @Test
    void recordsEffectiveMessagesToolsResponsesAndTerminalState() {
        var events = new ArrayList<Map<String, Object>>();
        doAnswer(invocation -> {
            events.add(new LinkedHashMap<>(invocation.getArgument(1)));
            return null;
        }).when(persistence).append(any(), any());

        var context = service.startTurn(
                "owner",
                "session",
                "Erstellen Sie einen Abschnitt.",
                new ChatContextModel("session", ElementType.FormLayout, null, null),
                null
        );
        var tool = FunctionToolCallback.builder("hole-formularstruktur", () -> "structure")
                .description("Liest die Formularstruktur")
                .inputSchema("{\"type\":\"object\",\"properties\":{}}")
                .build();
        var options = OpenAiChatOptions.builder()
                .model("qwen")
                .temperature(0.2)
                .timeout(Duration.ofMinutes(5))
                .toolCallbacks(tool)
                .build();
        var request = ChatClientRequest.builder()
                .prompt(new Prompt(List.of(
                        new SystemMessage("Systemanweisung"),
                        new UserMessage("Erstellen Sie einen Abschnitt.")
                ), options))
                .context(Map.of())
                .build();

        var round = service.recordModelRequest(context, request);
        var assistant = AssistantMessage.builder()
                .content("Ich prüfe das Formular.")
                .properties(Map.of("reasoningContent", "Interner Gedankengang"))
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "call-1", "function", "hole-formularstruktur", "{}")))
                .build();
        var response = new ChatResponse(
                List.of(new Generation(assistant, ChatGenerationMetadata.builder()
                        .finishReason("tool_calls")
                        .metadata("providerField", "value")
                        .build())),
                ChatResponseMetadata.builder()
                        .id("response-1")
                        .model("qwen")
                        .usage(new DefaultUsage(120, 30, 150))
                        .keyValue("providerMetadata", "preserved")
                        .build()
        );
        service.recordModelResponse(context, round,
                ChatClientResponse.builder().chatResponse(response).context(Map.of()).build(), 42);
        var previousToolResponse = ToolResponseMessage.builder().responses(List.of(
                new ToolResponseMessage.ToolResponse("old-call", "old-tool", "old result")
        )).build();
        var currentToolResponse = ToolResponseMessage.builder().responses(List.of(
                new ToolResponseMessage.ToolResponse("call-1", "hole-formularstruktur", "current structure")
        )).build();
        service.recordToolExecution(context, response, ToolExecutionResult.builder()
                .conversationHistory(List.of(previousToolResponse, currentToolResponse))
                .returnDirect(false)
                .build(), 8);
        service.finishTurn(context, AiChatTraceService.TurnStatus.COMPLETED,
                "Ich prüfe das Formular.", 1, 50, null);

        assertThat(events).extracting(event -> event.get("eventType"))
                .containsExactly("turn_started", "model_request", "model_response", "tool_execution", "turn_finished");
        assertThat(events.get(1)).containsEntry("round", 1);
        var requestData = data(events.get(1));
        assertThat(castList(requestData.get("messages")))
                .extracting(message -> message.get("role"), message -> message.get("content"))
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("system", "Systemanweisung"),
                        org.assertj.core.groups.Tuple.tuple("user", "Erstellen Sie einen Abschnitt.")
                );
        assertThat(castList(requestData.get("tools"))).singleElement().satisfies(recordedTool -> {
            assertThat(recordedTool).containsEntry("name", "hole-formularstruktur");
            assertThat(recordedTool.get("inputSchema")).isInstanceOf(Map.class);
        });

        var responseData = data(events.get(2));
        assertThat(responseData).containsEntry("durationMillis", 42L);
        var generation = castList(responseData.get("generations")).getFirst();
        assertThat(generation).containsEntry("finishReason", "tool_calls");
        var recordedMessage = castMap(generation.get("message"));
        assertThat(recordedMessage).containsEntry("content", "Ich prüfe das Formular.");
        assertThat(castMap(recordedMessage.get("metadata")))
                .containsEntry("reasoningContent", "Interner Gedankengang");
        assertThat(castList(recordedMessage.get("toolCalls"))).singleElement()
                .satisfies(call -> assertThat(call)
                        .containsEntry("id", "call-1")
                        .containsEntry("name", "hole-formularstruktur")
                        .containsEntry("arguments", "{}"));
        assertThat(castMap(responseData.get("metadata")))
                .containsEntry("id", "response-1")
                .containsEntry("model", "qwen");
        var toolData = data(events.get(3));
        assertThat(castList(toolData.get("calls"))).singleElement().satisfies(call -> assertThat(call)
                .containsEntry("id", "call-1")
                .containsEntry("name", "hole-formularstruktur"));
        assertThat(castList(toolData.get("responses"))).singleElement().satisfies(toolResponse -> assertThat(toolResponse)
                .containsEntry("id", "call-1")
                .containsEntry("name", "hole-formularstruktur")
                .containsEntry("responseData", "current structure"));
        assertThat(toolData.toString()).doesNotContain("old-call", "old result");
        assertThat(data(events.getLast()))
                .containsEntry("status", "COMPLETED")
                .containsEntry("streamedContent", "Ich prüfe das Formular.")
                .containsEntry("chunkCount", 1);
    }

    @Test
    void exportsOnlyAnOwnedUnexpiredTraceAsPrettyJson() throws Exception {
        properties.setRetentionDays(7);
        var session = new AiChatSessionEntity().setUserId("owner").setSessionId("session")
                .setMessages(List.of(Map.of("sequence", 1, "eventType", "turn_started")));
        session.prePersist();
        var updated = Instant.now().minusSeconds(60);
        ReflectionTestUtils.setField(session, "updated", updated);
        ReflectionTestUtils.setField(session, "created", updated.minusSeconds(60));
        when(sessions.findByUserIdAndSessionId("owner", "session")).thenReturn(Optional.of(session));

        var trace = service.exportTrace("owner", "session");
        var json = new String(trace.content(), StandardCharsets.UTF_8);

        verify(permissions).requireSystemPermission("owner", AiChatPermissionProvider.AI_CHAT_USE);
        assertThat(trace.filename()).isEqualTo("prosuna-ai-chat-trace-session.json");
        assertThat(json)
                .contains("\"schemaVersion\" : 1")
                .contains("\"sessionId\" : \"session\"")
                .contains("\"eventType\" : \"turn_started\"");
        var export = JsonMapperTestUtils.createMapper().readTree(json);
        assertThat(Instant.parse(export.path("expiresAt").asText()))
                .isEqualTo(updated.plusSeconds(7 * 24 * 60 * 60));
    }

    @Test
    void redactsInjectedAttachmentTextAndAttachmentToolResponses() {
        var events = new ArrayList<Map<String, Object>>();
        doAnswer(invocation -> {
            events.add(new LinkedHashMap<>(invocation.getArgument(1)));
            return null;
        }).when(persistence).append(any(), any());
        var context = service.startTurn("owner", "session", "Auftrag",
                new ChatContextModel("session", null, null, null), null);
        var user = UserMessage.builder()
                .text("Auftrag\n\nGeheimer Dateiinhalt")
                .metadata(Map.of(
                        AiChatAttachmentContext.MESSAGE_METADATA_KEY, true,
                        AiChatAttachmentContext.ORIGINAL_TEXT_METADATA_KEY, "Auftrag"
                ))
                .build();

        service.recordModelRequest(context, ChatClientRequest.builder()
                .prompt(new Prompt(user))
                .context(Map.of())
                .build());
        var response = new ChatResponse(List.of(new Generation(AssistantMessage.builder().toolCalls(List.of(
                new AssistantMessage.ToolCall("attachment", "function", AiChatAttachmentTools.TOOL_NAME, "{}")
        )).build())));
        service.recordToolExecution(context, response, ToolExecutionResult.builder()
                .conversationHistory(List.of(ToolResponseMessage.builder().responses(List.of(
                        new ToolResponseMessage.ToolResponse(
                                "attachment", AiChatAttachmentTools.TOOL_NAME, "Noch mehr geheimer Dateiinhalt"
                        )
                )).build()))
                .returnDirect(false)
                .build(), 1);

        assertThat(events.toString())
                .contains("Auftrag", "attachmentContextRedacted", "[Dateiinhalt ausgeblendet]")
                .doesNotContain("Geheimer Dateiinhalt", "Noch mehr geheimer Dateiinhalt");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> data(Map<String, Object> event) {
        return (Map<String, Object>) event.get("data");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object value) {
        return (List<Map<String, Object>>) value;
    }
}
