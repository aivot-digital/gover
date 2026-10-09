package de.aivot.prosuna.backend.plugins.ai.v1.nodes;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.ComputedElementState;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.AiCompletionActionNodeV1;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessNodeConfigurationValidationPhase;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeConfigurationValidationContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
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

class AiCompletionActionNodeV1Test {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final Integer NODE_ID = 123;
    private static final Long PROCESS_INSTANCE_ID = 99L;
    private static final Long TASK_ID = 456L;
    private static final int CENTRAL_MAX_TOKENS = 1337;
    private static final String CENTRAL_MODEL = "central-model";
    private static final Duration CENTRAL_CHAT_TIMEOUT = Duration.ofMinutes(2);

    private final List<Prompt> prompts = new ArrayList<>();
    private ChatModel chatModel;
    private AiCompletionActionNodeV1 node;

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
            return completionResponse("First completion");
        });
        node = createNode();
    }

    @Test
    void init_ShouldUseCentralSpringAiOptionsAndExposeOutputs() throws Exception {
        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(context(configuration("Rendered prompt")))
        );

        assertEquals("success", result.getViaPort());
        assertNull(result.getProcessData());
        assertEquals("Rendered prompt", result.getNodeData().get("prompt"));
        assertEquals("First completion", result.getNodeData().get("completion"));
        assertEquals("stop", result.getNodeData().get("finishReason"));
        assertEquals("response-model", result.getNodeData().get("responseModel"));
        assertEquals(
                Map.of(
                        "prompt_tokens", 11,
                        "completion_tokens", 7,
                        "total_tokens", 18
                ),
                result.getNodeData().get("usage")
        );
        assertEquals(
                List.of("prompt", "completion", "finishReason", "responseModel", "usage"),
                List.copyOf(result.getNodeData().keySet())
        );

        var prompt = prompts.getFirst();
        assertEquals(1, prompt.getInstructions().size());
        var message = assertInstanceOf(UserMessage.class, prompt.getInstructions().getFirst());
        assertEquals("Rendered prompt", message.getText());

        var options = assertInstanceOf(OpenAiChatOptions.class, prompt.getOptions());
        assertEquals(CENTRAL_MODEL, options.getModel());
        assertEquals(0.25d, options.getTemperature());
        assertEquals(0.7d, options.getTopP());
        assertEquals(2, options.getN());
        assertEquals(CENTRAL_MAX_TOKENS, options.getMaxTokens());
        assertEquals(CENTRAL_CHAT_TIMEOUT, options.getTimeout());

        assertEquals("KI-Anfrage", node.getName());
        assertEquals(1, node.getPorts().size());
        assertEquals("success", node.getPorts().getFirst().key());
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
    void init_ShouldRejectMissingCompletionText() {
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(
                new Generation(new AssistantMessage("   "))
        )));

        var exception = assertThrows(
                ProcessNodeExecutionExceptionUnknown.class,
                () -> node.init(context(configuration("Prompt")))
        );

        assertEquals("Die KI hat keinen Antworttext zurückgegeben.", exception.getMessage());
    }

    @Test
    void init_ShouldExposeNullUsageWhenTheProviderDoesNotReportUsage() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(
                List.of(new Generation(
                        new AssistantMessage("Completion"),
                        ChatGenerationMetadata.builder().finishReason("STOP").build()
                )),
                ChatResponseMetadata.builder().model("response-model").build()
        ));

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(context(configuration("Prompt")))
        );

        assertNull(result.getNodeData().get("usage"));
    }

    @Test
    void validateConfiguration_ShouldDeferPromptChecksOnlyDuringAuthoring() throws Exception {
        var configuration = configuration(null);
        var derivedData = new DerivedRuntimeElementData();
        derivedData.getElementStates().put(
                AiCompletionActionNodeV1.AiCompletionActionNodeConfig.PROMPT_FIELD_ID,
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
        assertTrue(runtimeErrors.containsKey(AiCompletionActionNodeV1.AiCompletionActionNodeConfig.PROMPT_FIELD_ID));
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

    private AiCompletionActionNodeV1 createNode() {
        return new AiCompletionActionNodeV1(ChatClient.builder(chatModel));
    }

    private static ChatResponse completionResponse(String content) {
        return new ChatResponse(
                List.of(new Generation(
                        new AssistantMessage(content),
                        ChatGenerationMetadata.builder().finishReason("STOP").build()
                )),
                ChatResponseMetadata.builder()
                        .model("response-model")
                        .usage(new DefaultUsage(11, 7, 18))
                        .build()
        );
    }

    private static AiCompletionActionNodeV1.AiCompletionActionNodeConfig configuration(String prompt) {
        var configuration = new AiCompletionActionNodeV1.AiCompletionActionNodeConfig();
        configuration.prompt = prompt;
        return configuration;
    }

    private static ProcessNodeExecutionInitContext<AiCompletionActionNodeV1.AiCompletionActionNodeConfig> context(
            AiCompletionActionNodeV1.AiCompletionActionNodeConfig configuration
    ) {
        var processData = new ProcessExecutionData();
        processData.put("$", Map.of("person", Map.of("name", "Ada")));
        processData.put("_", Map.of());
        processData.put("$$", Map.of());

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
                .setName("KI-Anfrage")
                .setDataKey("aiNode")
                .setProcessNodeDefinitionKey("de.aivot.ai.ai_completion")
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
