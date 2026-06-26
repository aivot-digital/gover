package de.aivot.gover.backend.plugins.core.v1.nodes.actions;

import de.aivot.gover.backend.core.services.HttpService;
import de.aivot.gover.backend.elements.models.AuthoredElementValues;
import de.aivot.gover.backend.identity.models.IdentityDataMap;
import de.aivot.gover.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.gover.backend.plugins.core.v1.nodes.actions.HttpActionNodeV1;
import de.aivot.gover.backend.plugins.core.v1.nodes.actions.HttpActionNodeV1Config;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.enums.ProcessInstanceStatus;
import de.aivot.gover.backend.process.enums.ProcessTaskStatus;
import de.aivot.gover.backend.process.exceptions.ProcessNodeExecutionExceptionIO;
import de.aivot.gover.backend.process.models.ProcessExecutionData;
import de.aivot.gover.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.gover.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.gover.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.gover.backend.process.services.TemplateRenderService;
import de.aivot.gover.backend.secrets.repositories.SecretRepository;
import de.aivot.gover.backend.secrets.services.SecretService;
import de.aivot.gover.backend.storage.services.StorageService;
import de.aivot.gover.backend.utils.MultipartUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpActionNodeV1Test {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final Integer NODE_ID = 123;
    private static final Long PROCESS_INSTANCE_ID = 99L;
    private static final Long TASK_ID = 456L;

    private HttpService httpService;
    private ProcessInstanceAttachmentService processInstanceAttachmentService;
    private StorageService storageService;
    private SecretRepository secretRepository;
    private SecretService secretService;
    private HttpActionNodeV1 node;

    @BeforeEach
    void setUp() {
        httpService = mock(HttpService.class);
        processInstanceAttachmentService = mock(ProcessInstanceAttachmentService.class);
        storageService = mock(StorageService.class);
        secretRepository = mock(SecretRepository.class);
        secretService = mock(SecretService.class);

        node = new HttpActionNodeV1(
                httpService,
                new PassthroughTemplateRenderService(),
                new JavascriptEngineFactoryService(List.of()),
                processInstanceAttachmentService,
                storageService,
                secretRepository,
                secretService
        );
    }

    @Test
    void init_ShouldBuildMultipartFieldsFromConfiguredRows() throws Exception {
        when(httpService.request(
                eq(HttpMethod.POST),
                any(),
                any(MultipartUtils.MultipartBodyPublisher.class),
                any()
        )).thenReturn(ResponseEntity.ok("ok".getBytes(StandardCharsets.UTF_8)));

        var configuration = baseConfig("POST", "https://gover.test/api", "text", "200");
        configuration.requestData = new HttpActionNodeV1Config.RequestData();
        configuration.requestData.requestContentType = HttpActionNodeV1Config.RequestData.REQUEST_CONTENT_TYPE_OPT_MULTIPARTFORMDATA;
        configuration.requestData.requestFormFields = List.of(
                Map.of("name", "firstName", "value", "Anna"),
                Map.of("name", "status", "value", "done")
        );

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(context(configuration))
        );

        assertEquals("success", result.getViaPort());

        var bodyCaptor = ArgumentCaptor.forClass(MultipartUtils.MultipartBodyPublisher.class);
        verify(httpService).request(eq(HttpMethod.POST), any(), bodyCaptor.capture(), any());

        @SuppressWarnings("unchecked")
        var builtBody = (MultiValueMap<String, Object>) bodyCaptor.getValue().build();
        assertEquals(List.of("Anna"), builtBody.get("firstName"));
        assertEquals(List.of("done"), builtBody.get("status"));
    }

    @Test
    void init_ShouldBuildJsonBodyFromProcessDataKey() throws Exception {
        when(httpService.request(eq(HttpMethod.POST), any(), anyString(), any()))
                .thenReturn(ResponseEntity.ok("ok".getBytes(StandardCharsets.UTF_8)));

        var configuration = baseConfig("POST", "https://gover.test/api", "text", "200");
        configuration.requestData = new HttpActionNodeV1Config.RequestData();
        configuration.requestData.requestContentType = HttpActionNodeV1Config.RequestData.REQUEST_CONTENT_TYPE_OPT_JSON;
        configuration.requestData.requestContentTypeJsonConfig = new HttpActionNodeV1Config.RequestData.RequestContentTypeJsonConfig();
        configuration.requestData.requestContentTypeJsonConfig.requestJsonSource = HttpActionNodeV1Config.RequestData.RequestContentTypeJsonConfig.REQUEST_JSON_SOURCE_OPT_VORGANGSDATEN;
        configuration.requestData.requestContentTypeJsonConfig.requestJsonProcessDataKey = "person";

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(context(configuration))
        );

        assertEquals("success", result.getViaPort());

        var bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpService).request(eq(HttpMethod.POST), any(), bodyCaptor.capture(), any());
        assertEquals("{\"vorname\":\"Anna\"}", bodyCaptor.getValue());
    }

    @Test
    void init_ShouldRouteUnexpectedStatusToErrorPort() throws Exception {
        when(httpService.request(eq(HttpMethod.GET), any(), any()))
                .thenReturn(ResponseEntity
                        .status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .header("X-Error-Code", "unprocessable")
                        .body("invalid".getBytes(StandardCharsets.UTF_8)));

        var configuration = baseConfig("GET", "https://gover.test/api", "text", "200");

        assertThrows(
                ProcessNodeExecutionExceptionIO.class,
                () -> node.init(context(configuration))
        );
    }

    @Test
    void init_ShouldStoreFileResponseAsAttachment() throws Exception {
        when(httpService.request(eq(HttpMethod.GET), any(), any()))
                .thenReturn(ResponseEntity
                        .status(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"fallback.pdf\"")
                        .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                        .body("pdf".getBytes(StandardCharsets.UTF_8)));

        when(processInstanceAttachmentService.create(any(ProcessInstanceAttachmentEntity.class)))
                .thenAnswer(invocation -> {
                    var attachment = invocation.getArgument(0, ProcessInstanceAttachmentEntity.class);
                    return attachment
                            .setKey(UUID.randomUUID())
                            .setStorageProviderId(7)
                            .setStoragePathFromRoot("attachments/document.pdf");
                });

        var configuration = baseConfig("GET", "https://gover.test/file", "file", "200");
        configuration.responseConfig.responseFileName = "document.pdf";

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(context(configuration))
        );

        assertEquals("success", result.getViaPort());
        assertEquals("document.pdf", result.getNodeData().get("fileName"));
        assertEquals("application/pdf", result.getNodeData().get("mimeType"));
        assertEquals(3, result.getNodeData().get("sizeBytes"));
        assertNull(result.getNodeData().get("rawBody"));
        assertNull(result.getNodeData().get("processedResponse"));
        assertNotNull(result.getNodeData().get("attachmentKey"));
    }

    private static HttpActionNodeV1Config baseConfig(String method,
                                                     String url,
                                                     String responseBodyType,
                                                     String... allowedStatusCodes) {
        var configuration = new HttpActionNodeV1Config();
        configuration.httpMethod = method;
        configuration.url = url;
        configuration.responseConfig = new HttpActionNodeV1Config.ResponseConfig();
        configuration.responseConfig.responseBodyType = responseBodyType;
        configuration.responseConfig.responseStatusCode = List.of(allowedStatusCodes);
        return configuration;
    }

    private static ProcessNodeExecutionInitContext<HttpActionNodeV1Config> context(HttpActionNodeV1Config configuration) {
        var processData = new ProcessExecutionData();
        processData.put("$", Map.of(
                "person", Map.of("vorname", "Anna")
        ));
        processData.put("_", Map.of(
                "nodeA", Map.of("status", "done")
        ));
        processData.put("$$", Map.of());

        return new ProcessNodeExecutionInitContext<>(
                logger(),
                processNode(),
                processInstance(),
                task(),
                null,
                processData,
                configuration
        );
    }

    private static ProcessNodeEntity processNode() {
        return new ProcessNodeEntity()
                .setId(NODE_ID)
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setName("HTTP")
                .setDataKey("httpNode")
                .setProcessNodeDefinitionKey("de.aivot.core.http_request")
                .setProcessNodeDefinitionVersion(1)
                .setConfiguration(new AuthoredElementValues())
                .setOutputMappings(Map.of());
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

    private static class PassthroughTemplateRenderService extends TemplateRenderService {
        private PassthroughTemplateRenderService() {
            super(null);
        }

        @Override
        public String interpolate(ProcessExecutionData foldedProcessData, String template) {
            return template;
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
