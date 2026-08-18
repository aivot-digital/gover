package de.aivot.gover.backend.plugins.core.v1.nodes.actions;

import de.aivot.gover.backend.core.models.HttpServiceHeaders;
import de.aivot.gover.backend.core.services.HttpService;
import de.aivot.gover.backend.core.services.ObjectMapperFactory;
import de.aivot.gover.backend.elements.models.AuthoredElementValues;
import de.aivot.gover.backend.identity.models.IdentityDataMap;
import de.aivot.gover.backend.plugins.ai.properties.AiPluginProperties;
import de.aivot.gover.backend.plugins.ai.v1.nodes.AiCompletionActionNodeV1;
import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.enums.ProcessInstanceStatus;
import de.aivot.gover.backend.process.enums.ProcessTaskStatus;
import de.aivot.gover.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.gover.backend.process.models.ProcessExecutionData;
import de.aivot.gover.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.gover.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.gover.backend.process.services.TemplateRenderService;
import de.aivot.gover.backend.secrets.entities.SecretEntity;
import de.aivot.gover.backend.secrets.repositories.SecretRepository;
import de.aivot.gover.backend.secrets.services.SecretService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiHubCompletionActionNodeV1Test {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final Integer NODE_ID = 123;
    private static final Long PROCESS_INSTANCE_ID = 99L;
    private static final Long TASK_ID = 456L;
    private static final int CONFIGURED_COMPLETION_MAX_TOKENS = 1337;

    private HttpService httpService;
    private SecretRepository secretRepository;
    private SecretService secretService;
    private RecordingTemplateRenderService templateRenderService;
    private AiCompletionActionNodeV1 node;

    @BeforeEach
    void setUp() {
        httpService = mock(HttpService.class);
        secretRepository = mock(SecretRepository.class);
        secretService = mock(SecretService.class);
        templateRenderService = new RecordingTemplateRenderService();

        node = new AiCompletionActionNodeV1(
                httpService,
                templateRenderService,
                secretRepository,
                secretService,
                createAiPluginProperties(1000, CONFIGURED_COMPLETION_MAX_TOKENS, 4000)
        );
    }

    @Test
    void init_ShouldRenderPromptSendBearerTokenAndExposeOutputs() throws Exception {
        var secretId = UUID.randomUUID();
        when(secretService.retrieve(secretId)).thenReturn(Optional.of(secret(secretId, "AI Hub Token")));
        when(secretService.decrypt(any(SecretEntity.class))).thenReturn("secret-token");
        when(httpService.request(eq(HttpMethod.POST), any(), anyString(), any()))
                .thenReturn(ResponseEntity.ok("""
                        {
                          "id": "resp-1",
                          "choices": [
                            {
                              "finish_reason": "stop",
                              "index": 0,
                              "message": {
                                "role": "assistant",
                                "content": "First completion"
                              }
                            },
                            {
                              "finish_reason": "length",
                              "index": 1,
                              "message": {
                                "role": "assistant",
                                "content": "Second completion"
                              }
                            }
                          ],
                          "created": 1716972000,
                          "object": "chat.completion",
                          "model": "meta-llama/Llama-3.3-70B-Instruct",
                          "usage": {
                            "prompt_tokens": 11,
                            "completion_tokens": 7,
                            "total_tokens": 18
                          }
                        }
                        """.getBytes(StandardCharsets.UTF_8)));

        templateRenderService.nextInterpolationResult = "Rendered prompt";

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(context(configuration(
                        "https://aihub.example/api/completions",
                        secretId,
                        "meta-llama/Llama-3.3-70B-Instruct",
                        "Hello {{ $.person.name }}"
                )))
        );

        assertEquals("success", result.getViaPort());
        assertNull(result.getProcessData());
        assertEquals("Rendered prompt", result.getNodeData().get("prompt"));
        assertEquals("First completion", result.getNodeData().get("completion"));
        assertEquals("stop", result.getNodeData().get("finishReason"));
        assertEquals("meta-llama/Llama-3.3-70B-Instruct", result.getNodeData().get("responseModel"));
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

        @SuppressWarnings("unchecked")
        var usage = (Map<String, Object>) result.getNodeData().get("usage");
        assertEquals(18, usage.get("total_tokens"));

        var uriCaptor = ArgumentCaptor.forClass(URI.class);
        var bodyCaptor = ArgumentCaptor.forClass(String.class);
        var headersCaptor = ArgumentCaptor.forClass(HttpServiceHeaders.class);
        verify(httpService).request(eq(HttpMethod.POST), uriCaptor.capture(), bodyCaptor.capture(), headersCaptor.capture());

        assertEquals(URI.create("https://aihub.example/api/completions/chat/completions"), uriCaptor.getValue());
        assertEquals("Hello {{ $.person.name }}", templateRenderService.lastTemplate);

        var requestBody = ObjectMapperFactory.getInstance().readValue(bodyCaptor.getValue(), Map.class);
        assertEquals("meta-llama/Llama-3.3-70B-Instruct", requestBody.get("model"));
        @SuppressWarnings("unchecked")
        var messages = (List<Map<String, Object>>) requestBody.get("messages");
        assertEquals(1, messages.size());
        assertEquals("user", messages.get(0).get("role"));
        assertEquals("Rendered prompt", messages.get(0).get("content"));
        assertEquals(0.01d, ((Number) requestBody.get("temperature")).doubleValue(), 0.0001d);
        assertEquals(0.9d, ((Number) requestBody.get("top_p")).doubleValue(), 0.0001d);
        assertEquals(1, ((Number) requestBody.get("n")).intValue());
        assertEquals(false, requestBody.get("stream"));
        assertEquals(CONFIGURED_COMPLETION_MAX_TOKENS, ((Number) requestBody.get("max_tokens")).intValue());
        assertNull(requestBody.get("prompt"));
        assertNull(requestBody.get("stop"));
        assertNull(requestBody.get("presence_penalty"));
        assertNull(requestBody.get("frequency_penalty"));
        assertNull(requestBody.get("logit_bias"));
        assertNull(requestBody.get("user"));

        var headers = new LinkedHashMap<String, String>();
        headersCaptor.getValue().forEach(headers::put);
        assertEquals("application/json", headers.get("Content-Type"));
        assertEquals("application/json", headers.get("Accept"));
        assertEquals("Bearer secret-token", headers.get("Authorization"));

        assertEquals("KI-Anfrage", node.getName());
        assertEquals(1, node.getPorts().size());
        assertEquals("success", node.getPorts().getFirst().key());
    }

    @Test
    void init_ShouldThrowForNon2xxResponses() throws Exception {
        var secretId = UUID.randomUUID();
        when(secretService.retrieve(secretId)).thenReturn(Optional.of(secret(secretId, "AI Hub Token")));
        when(secretService.decrypt(any(SecretEntity.class))).thenReturn("secret-token");
        when(httpService.request(eq(HttpMethod.POST), any(), anyString(), any()))
                .thenReturn(ResponseEntity
                        .status(HttpStatus.BAD_GATEWAY)
                        .body("upstream failed".getBytes(StandardCharsets.UTF_8)));

        var exception = assertThrows(
                ProcessNodeExecutionExceptionUnknown.class,
                () -> node.init(context(configuration(
                        "https://aihub.example/api/completions",
                        secretId,
                        "meta-llama/Llama-3.3-70B-Instruct",
                        "Prompt"
                )))
        );

        assertTrue(exception.getMessage().contains("HTTP-Status 502"));
    }

    @Test
    void init_ShouldThrowWhenNoCompletionTextsExist() throws Exception {
        var secretId = UUID.randomUUID();
        when(secretService.retrieve(secretId)).thenReturn(Optional.of(secret(secretId, "AI Hub Token")));
        when(secretService.decrypt(any(SecretEntity.class))).thenReturn("secret-token");
        when(httpService.request(eq(HttpMethod.POST), any(), anyString(), any()))
                .thenReturn(ResponseEntity.ok("""
                        {
                          "id": "resp-2",
                          "choices": [
                            {
                              "finish_reason": "stop",
                              "index": 0,
                              "message": {
                                "role": "assistant",
                                "content": "   "
                              }
                            }
                          ],
                          "created": 1716972001,
                          "object": "chat.completion",
                          "model": "meta-llama/Llama-3.3-70B-Instruct",
                          "usage": {
                            "prompt_tokens": 11,
                            "completion_tokens": 0,
                            "total_tokens": 11
                          }
                        }
                        """.getBytes(StandardCharsets.UTF_8)));

        var exception = assertThrows(
                ProcessNodeExecutionExceptionUnknown.class,
                () -> node.init(context(configuration(
                        "https://aihub.example/api/completions",
                        secretId,
                        "meta-llama/Llama-3.3-70B-Instruct",
                        "Prompt"
                )))
        );

        assertTrue(exception.getMessage().contains("Die Antwort der KI enthält keinen Texte."));
    }

    @Test
    void init_ShouldUsePluginDefaultMaxTokensWhenCompletionOverrideIsMissing() throws Exception {
        var secretId = UUID.randomUUID();
        when(secretService.retrieve(secretId)).thenReturn(Optional.of(secret(secretId, "AI Hub Token")));
        when(secretService.decrypt(any(SecretEntity.class))).thenReturn("secret-token");
        when(httpService.request(eq(HttpMethod.POST), any(), anyString(), any()))
                .thenReturn(ResponseEntity.ok("""
                        {
                          "id": "resp-3",
                          "choices": [
                            {
                              "finish_reason": "stop",
                              "index": 0,
                              "message": {
                                "role": "assistant",
                                "content": "Completion"
                              }
                            }
                          ],
                          "created": 1716972002,
                          "object": "chat.completion",
                          "model": "meta-llama/Llama-3.3-70B-Instruct",
                          "usage": {
                            "prompt_tokens": 11,
                            "completion_tokens": 7,
                            "total_tokens": 18
                          }
                        }
                        """.getBytes(StandardCharsets.UTF_8)));

        var defaultOnlyNode = new AiCompletionActionNodeV1(
                httpService,
                templateRenderService,
                secretRepository,
                secretService,
                createAiPluginProperties(2222, null, 4000)
        );

        defaultOnlyNode.init(context(configuration(
                "https://aihub.example/api/completions",
                secretId,
                "meta-llama/Llama-3.3-70B-Instruct",
                "Prompt"
        )));

        var bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpService).request(eq(HttpMethod.POST), any(), bodyCaptor.capture(), any());

        var requestBody = ObjectMapperFactory.getInstance().readValue(bodyCaptor.getValue(), Map.class);
        assertEquals(2222, ((Number) requestBody.get("max_tokens")).intValue());
    }

    @Test
    void validateConfiguration_ShouldAllowOutputMappingsForValidConfig() throws Exception {
        var secretId = UUID.randomUUID();
        when(secretService.retrieve(secretId)).thenReturn(Optional.of(secret(secretId, "AI Hub Token")));

        var errors = node.validateConfiguration(
                processNode(Map.of("completion", "ai.text")),
                configuration(
                        "https://aihub.example/api/completions",
                        secretId,
                        "meta-llama/Llama-3.3-70B-Instruct",
                        "Prompt"
                )
        );

        assertNull(errors);
    }

    @Test
    void cleanConfigurationForExport_ShouldRemoveSecretReference() {
        var configuration = new AuthoredElementValues();
        configuration.put(AiCompletionActionNodeV1.AiCompletionActionNodeConfig.ENDPOINT_URL_FIELD_ID, "https://aihub.example/api/completions");
        configuration.put(AiCompletionActionNodeV1.AiCompletionActionNodeConfig.API_KEY_SECRET_FIELD_ID, UUID.randomUUID().toString());

        var cleaned = node.cleanConfigurationForExport(configuration);

        assertEquals("https://aihub.example/api/completions",
                cleaned.get(AiCompletionActionNodeV1.AiCompletionActionNodeConfig.ENDPOINT_URL_FIELD_ID));
        assertTrue(!cleaned.containsKey(AiCompletionActionNodeV1.AiCompletionActionNodeConfig.API_KEY_SECRET_FIELD_ID));
    }

    private static AiCompletionActionNodeV1.AiCompletionActionNodeConfig configuration(String endpointUrl,
                                                                                       UUID apiKeySecret,
                                                                                       String model,
                                                                                       String prompt) {
        var configuration = new AiCompletionActionNodeV1.AiCompletionActionNodeConfig();
        configuration.endpointUrl = endpointUrl;
        configuration.apiKeySecret = apiKeySecret.toString();
        configuration.model = model;
        configuration.prompt = prompt;
        return configuration;
    }

    private static ProcessNodeExecutionInitContext<AiCompletionActionNodeV1.AiCompletionActionNodeConfig> context(
            AiCompletionActionNodeV1.AiCompletionActionNodeConfig configuration
    ) {
        var processData = new ProcessExecutionData();
        processData.put("$", Map.of(
                "person", Map.of("name", "Ada")
        ));
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
                .setDataKey("aiHubNode")
                .setProcessNodeDefinitionKey("de.aivot.core.ai_hub_completion")
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

    private static SecretEntity secret(UUID key, String name) {
        var secret = new SecretEntity();
        secret.setKey(key);
        secret.setName(name);
        secret.setDescription(name);
        secret.setValue("encrypted");
        secret.setSalt("salt");
        return secret;
    }

    private static AiPluginProperties createAiPluginProperties(int defaultMaxTokens,
                                                               Integer completionMaxTokens,
                                                               Integer processDataTransformationMaxTokens) {
        var properties = new AiPluginProperties();
        properties.setDefaultMaxTokens(defaultMaxTokens);

        var completion = new AiPluginProperties.CompletionProperties();
        completion.setMaxTokens(completionMaxTokens);
        properties.setCompletion(completion);

        var processDataTransformation = new AiPluginProperties.ProcessDataTransformationProperties();
        processDataTransformation.setMaxTokens(processDataTransformationMaxTokens);
        properties.setProcessDataTransformation(processDataTransformation);

        return properties;
    }

    private static class RecordingTemplateRenderService extends TemplateRenderService {
        private String nextInterpolationResult = "";
        private String lastTemplate;

        private RecordingTemplateRenderService() {
            super(null);
        }

        @Override
        public String interpolate(ProcessExecutionData foldedProcessData, String template) {
            lastTemplate = template;
            return nextInterpolationResult.isEmpty() ? template : nextInterpolationResult;
        }
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
