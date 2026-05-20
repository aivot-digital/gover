package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import de.aivot.GoverBackend.core.services.HttpService;
import de.aivot.GoverBackend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.GoverBackend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.models.ProcessExecutionData;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionLogger;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.GoverBackend.process.services.ProcessInstanceAttachmentService;
import de.aivot.GoverBackend.process.services.TemplateRenderService;
import de.aivot.GoverBackend.utils.MultipartUtils;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private HttpActionNodeV1 node;

    @BeforeEach
    void setUp() {
        httpService = mock(HttpService.class);
        processInstanceAttachmentService = mock(ProcessInstanceAttachmentService.class);

        node = new HttpActionNodeV1(
                httpService,
                new PassthroughTemplateRenderService(),
                new JavascriptEngineFactoryService(List.of()),
                processInstanceAttachmentService
        );
    }

    @Test
    void init_ShouldBuildMultipartFieldsFromNamedRows() throws Exception {
        when(httpService.request(
                eq(HttpMethod.POST),
                any(),
                any(MultipartUtils.MultipartBodyPublisher.class),
                any()
        )).thenReturn(ResponseEntity.ok("ok".getBytes(StandardCharsets.UTF_8)));

        var configuration = new HttpActionNodeV1.HttpActionNodeConfig();
        configuration.general = generalConfig("POST", "https://gover.test/api");
        configuration.outgoing = new HttpActionNodeV1.HttpActionNodeOutgoingConfig();
        configuration.outgoing.bodyType = "multipart";
        configuration.outgoing.sourceMode = "selected";
        configuration.outgoing.multipartFields = List.of(
                multipartField("firstName", "person.vorname"),
                multipartField("status", "_.nodeA.status")
        );
        configuration.incoming = incomingConfig("text", 200, null);

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
    void init_ShouldRouteUnexpectedStatusToErrorPort() throws Exception {
        when(httpService.request(eq(HttpMethod.GET), any(), any()))
                .thenReturn(ResponseEntity
                        .status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .header("X-Error-Code", "unprocessable")
                        .body("invalid".getBytes(StandardCharsets.UTF_8)));

        var configuration = new HttpActionNodeV1.HttpActionNodeConfig();
        configuration.general = generalConfig("GET", "https://gover.test/api");
        configuration.outgoing = new HttpActionNodeV1.HttpActionNodeOutgoingConfig();
        configuration.incoming = incomingConfig("text", 200, null);

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(context(configuration))
        );

        assertEquals("error", result.getViaPort());
        assertEquals(422, result.getNodeData().get("statusCode"));
        assertEquals("invalid", result.getNodeData().get("rawBody"));

        @SuppressWarnings("unchecked")
        var headers = (Map<String, Object>) result.getNodeData().get("headers");
        assertNotNull(headers);
        assertTrue(headers.containsKey("X-Error-Code"));
    }

    @Test
    void validateConfiguration_ShouldRejectOutputMappingsForFileResponses() throws Exception {
        var configuration = new HttpActionNodeV1.HttpActionNodeConfig();
        configuration.general = generalConfig("GET", "https://gover.test/file");
        configuration.outgoing = new HttpActionNodeV1.HttpActionNodeOutgoingConfig();
        configuration.incoming = incomingConfig("file", 200, null);

        var errors = node.validateConfiguration(
                processNode(Map.of("fileName", "attachment.fileName")),
                configuration
        );

        assertNotNull(errors);
        assertTrue(errors.containsKey("responseType"));
    }

    @Test
    void init_ShouldStoreFileResponseAsAttachment() throws Exception {
        when(httpService.request(eq(HttpMethod.GET), any(), any()))
                .thenReturn(ResponseEntity
                        .status(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"document.pdf\"")
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

        var configuration = new HttpActionNodeV1.HttpActionNodeConfig();
        configuration.general = generalConfig("GET", "https://gover.test/file");
        configuration.outgoing = new HttpActionNodeV1.HttpActionNodeOutgoingConfig();
        configuration.incoming = incomingConfig("file", 200, null);

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

    private static HttpActionNodeV1.HttpActionNodeGeneralConfig generalConfig(String method, String url) {
        var general = new HttpActionNodeV1.HttpActionNodeGeneralConfig();
        general.method = method;
        general.url = url;
        return general;
    }

    private static HttpActionNodeV1.HttpActionNodeIncomingConfig incomingConfig(String responseType,
                                                                                int expectedStatusCode,
                                                                                String responseProcessorCode) {
        var incoming = new HttpActionNodeV1.HttpActionNodeIncomingConfig();
        incoming.responseType = responseType;
        incoming.expectedStatusCode = expectedStatusCode;
        incoming.responseProcessorCode = responseProcessorCode;
        return incoming;
    }

    private static HttpActionNodeV1.HttpActionNodeMultipartFieldConfig multipartField(String name, String valueKey) {
        var field = new HttpActionNodeV1.HttpActionNodeMultipartFieldConfig();
        field.name = name;
        field.valueKey = valueKey;
        return field;
    }

    private static ProcessNodeExecutionInitContext<HttpActionNodeV1.HttpActionNodeConfig> context(HttpActionNodeV1.HttpActionNodeConfig configuration) {
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
                .setName("HTTP")
                .setDataKey("httpNode")
                .setProcessNodeDefinitionKey("de.aivot.core.http_request")
                .setProcessNodeDefinitionVersion(1)
                .setConfiguration(new de.aivot.GoverBackend.elements.models.AuthoredElementValues())
                .setOutputMappings(outputMappings);
    }

    private static ProcessInstanceEntity processInstance() {
        var now = LocalDateTime.now();

        return new ProcessInstanceEntity()
                .setId(PROCESS_INSTANCE_ID)
                .setAccessKey(UUID.randomUUID())
                .setProcessId(PROCESS_ID)
                .setInitialProcessVersion(PROCESS_VERSION)
                .setStatus(ProcessInstanceStatus.Running)
                .setAssignedFileNumbers(List.of())
                .setIdentities(Map.of())
                .setStarted(now)
                .setUpdated(now)
                .setInitialPayload(Map.of())
                .setInitialNodeId(1);
    }

    private static ProcessInstanceTaskEntity task() {
        var now = LocalDateTime.now();

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
