package de.aivot.GoverBackend.plugins.ai.v1.nodes;

import de.aivot.GoverBackend.core.models.HttpServiceHeaders;
import de.aivot.GoverBackend.core.services.HttpService;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.plugins.ai.properties.AiPluginProperties;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.GoverBackend.process.models.ProcessExecutionData;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionLogger;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.GoverBackend.process.services.TemplateRenderService;
import de.aivot.GoverBackend.secrets.entities.SecretEntity;
import de.aivot.GoverBackend.secrets.repositories.SecretRepository;
import de.aivot.GoverBackend.secrets.services.SecretService;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiProcessDataTransformationActionNodeV1Test {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final Integer NODE_ID = 123;
    private static final Long PROCESS_INSTANCE_ID = 99L;
    private static final Long TASK_ID = 456L;
    private static final int CONFIGURED_TRANSFORMATION_MAX_TOKENS = 4444;

    private HttpService httpService;
    private SecretRepository secretRepository;
    private SecretService secretService;
    private RecordingTemplateRenderService templateRenderService;
    private AiProcessDataTransformationActionNodeV1 node;

    @BeforeEach
    void setUp() {
        httpService = mock(HttpService.class);
        secretRepository = mock(SecretRepository.class);
        secretService = mock(SecretService.class);
        templateRenderService = new RecordingTemplateRenderService();

        node = new AiProcessDataTransformationActionNodeV1(
                httpService,
                templateRenderService,
                secretRepository,
                secretService,
                createAiPluginProperties(1000, 1337, CONFIGURED_TRANSFORMATION_MAX_TOKENS)
        );
    }

    @Test
    void init_ShouldRenderPromptSendFullExecutionDataAndReplaceProcessData() throws Exception {
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
                                "content": "{\\"decision\\":\\"approve\\",\\"person\\":{\\"name\\":\\"Ada Lovelace\\"}}"
                              }
                            }
                          ],
                          "created": 1716972000,
                          "object": "chat.completion",
                          "model": "meta-llama/Llama-3.3-70B-Instruct",
                          "usage": {
                            "prompt_tokens": 42,
                            "completion_tokens": 17,
                            "total_tokens": 59
                          }
                        }
                        """.getBytes(StandardCharsets.UTF_8)));

        templateRenderService.nextInterpolationResult = "Use formalized applicant data.";

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(context(configuration(
                        "https://aihub.example/api/completions",
                        secretId,
                        "meta-llama/Llama-3.3-70B-Instruct",
                        "Transform {{ $.person.name }}"
                )))
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
        assertEquals("meta-llama/Llama-3.3-70B-Instruct", result.getNodeData().get("responseModel"));
        assertEquals(
                Map.of(
                        "prompt_tokens", 42,
                        "completion_tokens", 17,
                        "total_tokens", 59
                ),
                result.getNodeData().get("usage")
        );
        assertEquals(List.of("decision", "person"), result.getNodeData().get("topLevelKeys"));

        var uriCaptor = ArgumentCaptor.forClass(URI.class);
        var bodyCaptor = ArgumentCaptor.forClass(String.class);
        var headersCaptor = ArgumentCaptor.forClass(HttpServiceHeaders.class);
        verify(httpService).request(eq(HttpMethod.POST), uriCaptor.capture(), bodyCaptor.capture(), headersCaptor.capture());

        assertEquals(URI.create("https://aihub.example/api/completions/chat/completions"), uriCaptor.getValue());
        assertEquals("Transform {{ $.person.name }}", templateRenderService.lastTemplate);

        var requestBody = ObjectMapperFactory.getInstance().readValue(bodyCaptor.getValue(), Map.class);
        assertEquals("meta-llama/Llama-3.3-70B-Instruct", requestBody.get("model"));
        assertEquals(CONFIGURED_TRANSFORMATION_MAX_TOKENS, ((Number) requestBody.get("max_tokens")).intValue());

        @SuppressWarnings("unchecked")
        var messages = (List<Map<String, Object>>) requestBody.get("messages");
        assertEquals(2, messages.size());
        assertEquals("system", messages.get(0).get("role"));
        assertTrue(messages.get(0).get("content").toString().contains("Return exactly one valid JSON object"));
        assertEquals("user", messages.get(1).get("role"));
        assertTrue(messages.get(1).get("content").toString().contains("Use formalized applicant data."));
        assertTrue(messages.get(1).get("content").toString().contains("\"$\""));
        assertTrue(messages.get(1).get("content").toString().contains("\"$$\""));
        assertTrue(messages.get(1).get("content").toString().contains("\"_\""));
        assertTrue(messages.get(1).get("content").toString().contains("\"workflow\":\"intake\""));

        var headers = new LinkedHashMap<String, String>();
        headersCaptor.getValue().forEach(headers::put);
        assertEquals("application/json", headers.get("Content-Type"));
        assertEquals("application/json", headers.get("Accept"));
        assertEquals("Bearer secret-token", headers.get("Authorization"));
    }

    @Test
    void init_ShouldAcceptJsonCodeFenceResponses() throws Exception {
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
                                "content": "```json\\n{\\"status\\":\\"updated\\"}\\n```"
                              }
                            }
                          ],
                          "created": 1716972001,
                          "object": "chat.completion",
                          "model": "meta-llama/Llama-3.3-70B-Instruct",
                          "usage": {
                            "prompt_tokens": 12,
                            "completion_tokens": 6,
                            "total_tokens": 18
                          }
                        }
                        """.getBytes(StandardCharsets.UTF_8)));

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(context(configuration(
                        "https://aihub.example/api/completions",
                        secretId,
                        "meta-llama/Llama-3.3-70B-Instruct",
                        "Prompt"
                )))
        );

        assertEquals(Map.of("status", "updated"), result.getProcessData());
    }

    @Test
    void init_ShouldUsePluginDefaultMaxTokensWhenTransformationOverrideIsMissing() throws Exception {
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
                                "content": "{\\"status\\":\\"ok\\"}"
                              }
                            }
                          ],
                          "created": 1716972002,
                          "object": "chat.completion",
                          "model": "meta-llama/Llama-3.3-70B-Instruct",
                          "usage": {
                            "prompt_tokens": 10,
                            "completion_tokens": 4,
                            "total_tokens": 14
                          }
                        }
                        """.getBytes(StandardCharsets.UTF_8)));

        var defaultOnlyNode = new AiProcessDataTransformationActionNodeV1(
                httpService,
                templateRenderService,
                secretRepository,
                secretService,
                createAiPluginProperties(2222, 1337, null)
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
    void init_ShouldThrowWhenResponseIsNotAJsonObject() throws Exception {
        var secretId = UUID.randomUUID();
        when(secretService.retrieve(secretId)).thenReturn(Optional.of(secret(secretId, "AI Hub Token")));
        when(secretService.decrypt(any(SecretEntity.class))).thenReturn("secret-token");
        when(httpService.request(eq(HttpMethod.POST), any(), anyString(), any()))
                .thenReturn(ResponseEntity.ok("""
                        {
                          "id": "resp-4",
                          "choices": [
                            {
                              "finish_reason": "stop",
                              "index": 0,
                              "message": {
                                "role": "assistant",
                                "content": "[1, 2, 3]"
                              }
                            }
                          ],
                          "created": 1716972003,
                          "object": "chat.completion",
                          "model": "meta-llama/Llama-3.3-70B-Instruct",
                          "usage": {
                            "prompt_tokens": 10,
                            "completion_tokens": 4,
                            "total_tokens": 14
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

        assertTrue(exception.getMessage().contains("JSON-Objekt"));
    }

    @Test
    void validateConfiguration_ShouldAllowValidConfig() throws Exception {
        var secretId = UUID.randomUUID();
        when(secretService.retrieve(secretId)).thenReturn(Optional.of(secret(secretId, "AI Hub Token")));

        var errors = node.validateConfiguration(
                processNode(Map.of()),
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
        configuration.put(AiProcessDataTransformationActionNodeV1.AiProcessDataTransformationActionNodeConfig.ENDPOINT_URL_FIELD_ID, "https://aihub.example/api/completions");
        configuration.put(AiProcessDataTransformationActionNodeV1.AiProcessDataTransformationActionNodeConfig.API_KEY_SECRET_FIELD_ID, UUID.randomUUID().toString());

        var cleaned = node.cleanConfigurationForExport(configuration);

        assertEquals(
                "https://aihub.example/api/completions",
                cleaned.get(AiProcessDataTransformationActionNodeV1.AiProcessDataTransformationActionNodeConfig.ENDPOINT_URL_FIELD_ID)
        );
        assertTrue(!cleaned.containsKey(AiProcessDataTransformationActionNodeV1.AiProcessDataTransformationActionNodeConfig.API_KEY_SECRET_FIELD_ID));
    }

    private static AiProcessDataTransformationActionNodeV1.AiProcessDataTransformationActionNodeConfig configuration(String endpointUrl,
                                                                                                                     UUID apiKeySecret,
                                                                                                                     String model,
                                                                                                                     String prompt) {
        var configuration = new AiProcessDataTransformationActionNodeV1.AiProcessDataTransformationActionNodeConfig();
        configuration.endpointUrl = endpointUrl;
        configuration.apiKeySecret = apiKeySecret.toString();
        configuration.model = model;
        configuration.prompt = prompt;
        return configuration;
    }

    private static ProcessNodeExecutionInitContext<AiProcessDataTransformationActionNodeV1.AiProcessDataTransformationActionNodeConfig> context(
            AiProcessDataTransformationActionNodeV1.AiProcessDataTransformationActionNodeConfig configuration
    ) {
        var processData = new ProcessExecutionData();
        processData.put("$", Map.of(
                "person", Map.of("name", "Ada")
        ));
        processData.put("_", Map.of(
                "previous", Map.of("result", "kept")
        ));
        processData.put("$$", Map.of(
                "workflow", "intake"
        ));

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
                .setAccessKey(UUID.randomUUID())
                .setProcessId(PROCESS_ID)
                .setInitialProcessVersion(PROCESS_VERSION)
                .setStatus(ProcessInstanceStatus.Running)
                .setAssignedFileNumbers(List.of())
                .setIdentities(new de.aivot.GoverBackend.identity.models.IdentityDataMap())
                .setStarted(now)
                .setUpdated(now)
                .setInitialPayload(Map.of())
                .setInitialNodeId(1);
    }

    private static ProcessInstanceTaskEntity task() {
        var now = Instant.now();

        return new ProcessInstanceTaskEntity()
                .setId(TASK_ID)
                .setAccessKey(UUID.randomUUID())
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
