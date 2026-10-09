package de.aivot.prosuna.backend.plugins.ai.v1.nodes;

import de.aivot.prosuna.backend.elements.enums.InputVariableSource;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.ComputedElementState;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.AiProcessDataTransformationActionNodeV1;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessNodeConfigurationValidationPhase;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.models.InputVariableSuggestion;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinitionMetadata;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeConfigurationValidationContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiProcessDataTransformationActionNodeV1Test {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final Integer NODE_ID = 123;
    private static final Long PROCESS_INSTANCE_ID = 99L;
    private static final Long TASK_ID = 456L;
    private static final int CENTRAL_MAX_TOKENS = 4096;
    private static final String CENTRAL_MODEL = "central-model";
    private static final Duration CENTRAL_CHAT_TIMEOUT = Duration.ofMinutes(2);

    private final List<Prompt> prompts = new ArrayList<>();
    private ChatModel chatModel;
    private AiProcessDataTransformationActionNodeV1 node;

    @BeforeEach
    void setUp() {
        chatModel = mock(ChatModel.class, CALLS_REAL_METHODS);
        when(chatModel.getOptions()).thenReturn(OpenAiChatOptions.builder()
                .model(CENTRAL_MODEL)
                .temperature(0.25d)
                .topP(0.7d)
                .n(2)
                .maxTokens(CENTRAL_MAX_TOKENS)
                .timeout(CENTRAL_CHAT_TIMEOUT)
                .build());
        when(chatModel.call(any(Prompt.class))).thenAnswer(invocation -> {
            prompts.add(invocation.getArgument(0));
            return completionResponse("{\"decision\":\"approve\",\"person\":{\"name\":\"Ada Lovelace\"}}");
        });
        node = createNode();
    }

    @Test
    void init_ShouldUseCentralSpringAiOptionsAndTransformProcessData() throws Exception {
        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(context(configuration("Use formalized applicant data.")))
        );

        assertEquals("success", result.getViaPort());
        assertEquals(
                Map.of(
                        "decision", "approve",
                        "person", Map.of("name", "Ada Lovelace")
                ),
                result.getProcessData()
        );
        assertEquals("Use formalized applicant data.", result.getNodeData().get("prompt"));
        assertEquals("stop", result.getNodeData().get("finishReason"));
        assertEquals("response-model", result.getNodeData().get("responseModel"));
        assertEquals(
                Map.of(
                        "prompt_tokens", 42,
                        "completion_tokens", 17,
                        "total_tokens", 59
                ),
                result.getNodeData().get("usage")
        );
        assertEquals(List.of("decision", "person"), result.getNodeData().get("topLevelKeys"));

        var prompt = prompts.getFirst();
        assertEquals(2, prompt.getInstructions().size());
        var systemMessage = assertInstanceOf(SystemMessage.class, prompt.getInstructions().get(0));
        assertTrue(systemMessage.getText().contains("Return exactly one valid JSON object"));
        var userMessage = assertInstanceOf(UserMessage.class, prompt.getInstructions().get(1));
        assertTrue(userMessage.getText().contains("Use formalized applicant data."));
        assertTrue(userMessage.getText().contains("\"$\""));
        assertTrue(userMessage.getText().contains("\"$$\""));
        assertTrue(userMessage.getText().contains("\"_\""));
        assertTrue(userMessage.getText().contains("\"workflow\":\"intake\""));

        var options = assertInstanceOf(OpenAiChatOptions.class, prompt.getOptions());
        assertEquals(CENTRAL_MODEL, options.getModel());
        assertEquals(0.25d, options.getTemperature());
        assertEquals(0.7d, options.getTopP());
        assertEquals(2, options.getN());
        assertEquals(CENTRAL_MAX_TOKENS, options.getMaxTokens());
        assertEquals(CENTRAL_CHAT_TIMEOUT, options.getTimeout());
    }

    @Test
    void init_ShouldAcceptJsonCodeFenceResponses() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenReturn(completionResponse("""
                ```json
                {"status":"updated"}
                ```
                """));

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(context(configuration("Prompt")))
        );

        assertEquals(Map.of("status", "updated"), result.getProcessData());
    }

    @Test
    void init_ShouldLeaveMaxTokensUnsetWhenCentralModelDoesNotConfigureLimit() throws Exception {
        when(chatModel.getOptions()).thenReturn(OpenAiChatOptions.builder()
                .model(CENTRAL_MODEL)
                .build());
        var nodeWithoutTokenLimit = createNode();

        nodeWithoutTokenLimit.init(context(configuration("Prompt")));

        var options = assertInstanceOf(OpenAiChatOptions.class, prompts.getFirst().getOptions());
        assertNull(options.getMaxTokens());
    }

    @Test
    void init_ShouldThrowWhenResponseIsNotAJsonObject() {
        when(chatModel.call(any(Prompt.class))).thenReturn(completionResponse("[1, 2, 3]"));

        var exception = assertThrows(
                ProcessNodeExecutionExceptionUnknown.class,
                () -> node.init(context(configuration("Prompt")))
        );

        assertTrue(exception.getMessage().contains("JSON-Objekt"));
    }

    @Test
    void init_ShouldWrapSpringAiFailures() {
        var failure = new IllegalStateException("upstream failed");
        when(chatModel.call(any(Prompt.class))).thenThrow(failure);

        var exception = assertThrows(
                ProcessNodeExecutionExceptionUnknown.class,
                () -> node.init(context(configuration("Prompt")))
        );

        assertEquals("Die KI-Anfrage konnte nicht ausgeführt werden.", exception.getMessage());
        assertEquals(failure, exception.getCause());
    }

    @Test
    void validateConfiguration_ShouldDeferPromptChecksOnlyDuringAuthoring() throws Exception {
        var configuration = configuration(null);
        var derivedData = new DerivedRuntimeElementData();
        derivedData.getElementStates().put(
                AiProcessDataTransformationActionNodeV1.AiProcessDataTransformationActionNodeConfig.PROMPT_FIELD_ID,
                new ComputedElementState().setInputValueDeferred(true)
        );

        var authoringErrors = node.validateConfiguration(new ProcessNodeConfigurationValidationContext<>(
                processNode(Map.of()),
                configuration,
                derivedData,
                ProcessNodeConfigurationValidationPhase.Authoring
        ));
        var runtimeErrors = node.validateConfiguration(new ProcessNodeConfigurationValidationContext<>(
                processNode(Map.of()),
                configuration,
                derivedData,
                ProcessNodeConfigurationValidationPhase.Runtime
        ));

        assertNull(authoringErrors);
        assertNotNull(runtimeErrors);
        assertTrue(runtimeErrors.containsKey(
                AiProcessDataTransformationActionNodeV1.AiProcessDataTransformationActionNodeConfig.PROMPT_FIELD_ID
        ));
    }

    @Test
    void cleanConfigurationForExport_ShouldRemoveLegacyConnectionFields() {
        var configuration = new AuthoredElementValues();
        configuration.putLiteral("endpointUrl", "https://aihub.example/api/completions");
        configuration.putLiteral("apiKeySecret", UUID.randomUUID().toString());
        configuration.putLiteral("model", "legacy-model");
        configuration.putLiteral("prompt", "Prompt");

        var cleaned = node.cleanConfigurationForExport(configuration);

        assertFalse(cleaned.containsKey("endpointUrl"));
        assertFalse(cleaned.containsKey("apiKeySecret"));
        assertFalse(cleaned.containsKey("model"));
        assertEquals("Prompt", cleaned.getLiteral("prompt"));
    }

    @Test
    void getMetadata_ShouldForwardAllPreviousMetadata() {
        var origin = processNode(Map.of());
        var previousMetadata = ProcessNodeDefinitionMetadata.empty()
                .addInputVariable(new InputVariableSuggestion(
                        InputVariableSource.ProcessData,
                        "person.name",
                        null,
                        "Name",
                        null,
                        origin
                ))
                .addInputVariable(new InputVariableSuggestion(
                        InputVariableSource.ElementData,
                        "result",
                        "previousNode",
                        "Ergebnis",
                        null,
                        origin
                ));

        var metadata = node.getMetadata(
                origin,
                new AiProcessDataTransformationActionNodeV1.AiProcessDataTransformationActionNodeConfig(),
                previousMetadata
        );

        assertEquals(previousMetadata, metadata);
    }

    private AiProcessDataTransformationActionNodeV1 createNode() {
        return new AiProcessDataTransformationActionNodeV1(ChatClient.builder(chatModel));
    }

    private static ChatResponse completionResponse(String content) {
        return new ChatResponse(
                List.of(new Generation(
                        new AssistantMessage(content),
                        ChatGenerationMetadata.builder().finishReason("STOP").build()
                )),
                ChatResponseMetadata.builder()
                        .model("response-model")
                        .usage(new DefaultUsage(42, 17, 59))
                        .build()
        );
    }

    private static AiProcessDataTransformationActionNodeV1.AiProcessDataTransformationActionNodeConfig configuration(String prompt) {
        var configuration = new AiProcessDataTransformationActionNodeV1.AiProcessDataTransformationActionNodeConfig();
        configuration.prompt = prompt;
        return configuration;
    }

    private static ProcessNodeExecutionInitContext<AiProcessDataTransformationActionNodeV1.AiProcessDataTransformationActionNodeConfig> context(
            AiProcessDataTransformationActionNodeV1.AiProcessDataTransformationActionNodeConfig configuration
    ) {
        var processData = new ProcessExecutionData();
        processData.put("$", Map.of("person", Map.of("name", "Ada")));
        processData.put("_", Map.of("previous", Map.of("result", "kept")));
        processData.put("$$", Map.of("workflow", "intake"));

        return new ProcessNodeExecutionInitContext<>(
                logger(),
                processNode(Map.of()),
                processInstance(),
                task(),
                null,
                processData,
                configuration
        );
    }

    private static ProcessNodeEntity processNode(Map<String, String> outputMappings) {
        return new ProcessNodeEntity()
                .setId(NODE_ID)
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setName("KI-Vorgangsdaten")
                .setDataKey("aiProcessDataNode")
                .setProcessNodeDefinitionKey("de.aivot.ai.ai_process_data_transformation")
                .setProcessNodeDefinitionVersion(1)
                .setConfiguration(new AuthoredElementValues())
                .setOutputMappings(outputMappings);
    }

    private static ProcessInstanceEntity processInstance() {
        var now = Instant.now();
        return new ProcessInstanceEntity()
                .setId(PROCESS_INSTANCE_ID)
                .setAccessKey(UUID.randomUUID().toString())
                .setProcessId(PROCESS_ID)
                .setInitialProcessVersion(PROCESS_VERSION)
                .setStatus(ProcessInstanceStatus.Running)
                .setAssignedFileNumbers(List.of())
                .setIdentities(new IdentityDataMap())
                .setStarted(now)
                .setUpdated(now)
                .setInitialPayload(Map.of())
                .setInitialNodeId(1);
    }

    private static ProcessInstanceTaskEntity task() {
        var now = Instant.now();
        return new ProcessInstanceTaskEntity()
                .setId(TASK_ID)
                .setAccessKey(UUID.randomUUID().toString())
                .setProcessInstanceId(PROCESS_INSTANCE_ID)
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setProcessNodeId(NODE_ID)
                .setPreviousProcessInstanceTaskId(null)
                .setPreviousProcessNodePortKey(null)
                .setStatus(ProcessTaskStatus.Running)
                .setStarted(now)
                .setUpdated(now)
                .setRuntimeData(Map.of())
                .setNodeData(Map.of())
                .setProcessData(Map.of());
    }

    private static ProcessNodeExecutionLogger logger() {
        return new ProcessNodeExecutionLogger(
                PROCESS_INSTANCE_ID,
                TASK_ID,
                null,
                null,
                proxy(ProcessInstanceHistoryEventRepository.class, (methodName, args) -> switch (methodName) {
                    case "save" -> args[0];
                    default -> unsupported(methodName);
                })
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> invocation.invoke(method.getName(), args)
        );
    }

    private static UnsupportedOperationException unsupported(String methodName) {
        return new UnsupportedOperationException("Unexpected invocation: " + methodName);
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(String methodName, Object[] args) throws Throwable;
    }
}
