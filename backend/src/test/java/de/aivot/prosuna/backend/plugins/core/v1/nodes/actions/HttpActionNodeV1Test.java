package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import de.aivot.prosuna.backend.core.exceptions.HttpConnectionException;
import de.aivot.prosuna.backend.core.services.HttpService;
import de.aivot.prosuna.backend.elements.enums.InputMode;
import de.aivot.prosuna.backend.elements.enums.InputVariableSource;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.elements.models.elements.DynamicTextElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.FileUploadInputElementItem;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SecretSelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.ProcessDataKeyInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.HttpActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.HttpActionNodeV1Config;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionIO;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidDataType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentSetService;
import de.aivot.prosuna.backend.process.enums.TemplateOutputFormat;
import de.aivot.prosuna.backend.process.services.TemplateRenderService;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import de.aivot.prosuna.backend.secrets.entities.SecretEntity;
import de.aivot.prosuna.backend.storage.services.StorageService;
import de.aivot.prosuna.backend.utils.MultipartUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.core.JacksonException;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class HttpActionNodeV1Test {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final Integer NODE_ID = 123;
    private static final Long PROCESS_INSTANCE_ID = 99L;
    private static final Long TASK_ID = 456L;

    private HttpService httpService;
    private ProcessInstanceAttachmentService processInstanceAttachmentService;
    private ProcessInstanceAttachmentSetService processInstanceAttachmentSetService;
    private StorageService storageService;
    private SecretService secretService;
    private HttpActionNodeV1 node;
    private PassthroughTemplateRenderService templateRenderService;

    @BeforeEach
    void setUp() {
        httpService = mock(HttpService.class);
        processInstanceAttachmentService = mock(ProcessInstanceAttachmentService.class);
        processInstanceAttachmentSetService = mock(ProcessInstanceAttachmentSetService.class);
        storageService = mock(StorageService.class);
        secretService = mock(SecretService.class);

        templateRenderService = new PassthroughTemplateRenderService();
        node = new HttpActionNodeV1(
                httpService,
                templateRenderService,
                new JavascriptEngineFactoryService(List.of()),
                processInstanceAttachmentService,
                processInstanceAttachmentSetService,
                storageService,
                secretService
        );
    }

    @Test
    void configurationLayout_ShouldDeserializeBasicAuthPasswordAsSecretSelection() throws Exception {
        var layout = node.loadConfigLayoutFromResource(
                new ClassPathResource("nodes/configs/HttpActionNodeV1Config.json")
        );

        assertTrue(layout.findChild(
                HttpActionNodeV1Config.BasicAuthConfig.PASSWORD_SECRET_KEY_FIELD_ID,
                SecretSelectInputElement.class
        ).isPresent());
    }

    @Test
    void getOutputs_ShouldExposeDynamicAndStructuredResponseTypes() {
        var processedResponseOutput = node
                .getOutputs()
                .stream()
                .filter(output -> "processedResponse".equals(output.key()))
                .findFirst()
                .orElseThrow();
        var headersOutput = node
                .getOutputs()
                .stream()
                .filter(output -> "headers".equals(output.key()))
                .findFirst()
                .orElseThrow();
        var filesOutput = node
                .getOutputs()
                .stream()
                .filter(output -> "files".equals(output.key()))
                .findFirst()
                .orElseThrow();

        assertEquals("unknown", processedResponseOutput.typeDefinition());
        assertEquals("Antwortdaten", processedResponseOutput.label());
        assertEquals("Record<string, Array<string>>", headersOutput.typeDefinition());
        assertEquals("Antwortheader", headersOutput.label());
        assertEquals("Dateiverweise", filesOutput.label());
        assertEquals(
                "Array<{ name: string; originalFileName: string; uri: string; size: number; }>",
                filesOutput.typeDefinition()
        );
    }

    @Test
    void getConfigurationLayout_ShouldEnableDynamicRuntimeValuesOnly() throws Exception {
        ReflectionTestUtils.setField(
                node,
                "configResource",
                new ClassPathResource("nodes/configs/HttpActionNodeV1Config.json")
        );

        var layout = node.getConfigurationLayout(null);
        assertEquals("HTTP-Schnittstelle aufrufen", node.getName());
        var port = node.getPorts(baseConfig("GET", "https://prosuna.test/api", "text", "200")).getFirst();
        assertEquals("success", port.key());
        assertEquals("Antwort verarbeitet", port.label());
        assertTrue(port.description().contains("Statuscode zugelassen"));
        assertEquals("Adresse der HTTP-Schnittstelle (URL)", layout.findChild("url", TextInputElement.class).orElseThrow().getLabel());
        var requestContentType = layout.findChild("requestContentType", SelectInputElement.class).orElseThrow();
        assertEquals("Format des Anfrageinhalts", requestContentType.getLabel());
        assertEquals("Ohne Anfrageinhalt", requestContentType.getOptions().stream()
                .filter(option -> "custom".equals(option.getValue())).findFirst().orElseThrow().getLabel());
        var path = layout.findChild("requestJsonProcessDataKey", ProcessDataKeyInputElement.class).orElseThrow();
        assertEquals("Vorgangsdatenpfad", path.getLabel());
        assertTrue(path.getHint().contains("Präfix $. steht für die Vorgangsdatenebene"));
        for (var fieldId : List.of("url", "bearerToken")) {
            var input = assertInstanceOf(BaseInputElement.class, layout.findChild(fieldId).orElseThrow());
            assertEquals(List.of(InputMode.values()), input.getInputModePolicy().allowedModes(), fieldId);
            assertEquals(List.of(InputVariableSource.values()), input.getInputModePolicy().allowedVariableSources(), fieldId);
        }
        for (var fieldId : List.of("url", "bearerToken", "responseFileName")) {
            var input = assertInstanceOf(DynamicTextElement.class, layout.findChild(fieldId).orElseThrow());
            assertEquals(
                    List.of(InputVariableSource.values()),
                    input.getDynamicTextPolicy().variableSuggestionSources(),
                    fieldId
            );
            assertEquals(TemplateOutputFormat.SingleLineText, input.getDynamicTextPolicy().outputFormat(), fieldId);
        }

        var username = layout.findChild("username").orElseThrow();
        assertNull(assertInstanceOf(BaseInputElement.class, username).getInputModePolicy());
        assertNull(assertInstanceOf(DynamicTextElement.class, username).getDynamicTextPolicy());

        var passwordSecret = assertInstanceOf(
                BaseInputElement.class,
                layout.findChild("passwordSecretKey").orElseThrow()
        );
        assertNull(passwordSecret.getInputModePolicy());
        assertFalse(passwordSecret instanceof DynamicTextElement);
    }

    @Test
    void init_ShouldBuildMultipartFieldsFromConfiguredRows() throws Exception {
        when(httpService.request(
                eq(HttpMethod.POST),
                any(),
                any(MultipartUtils.MultipartBodyPublisher.class),
                any()
        )).thenReturn(ResponseEntity.ok("ok".getBytes(StandardCharsets.UTF_8)));

        var configuration = baseConfig("POST", "https://prosuna.test/api", "text", "200");
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
    void init_ShouldInsertValuesIntoHeadersAsOneLineAndIntoFormFieldsWithTheirLineBreaks() throws Exception {
        when(httpService.request(
                eq(HttpMethod.POST),
                any(),
                any(MultipartUtils.MultipartBodyPublisher.class),
                any()
        )).thenReturn(ResponseEntity.ok("ok".getBytes(StandardCharsets.UTF_8)));

        var configuration = baseConfig("POST", "https://prosuna.test/api", "text", "200");
        configuration.additionalSettings = new HttpActionNodeV1Config.AdditionalSettings();
        configuration.additionalSettings.additionalHeaders = List.of(
                Map.of("field", "X-Antrag-{{ $.typ }}", "value", "{{ $.person.vorname }}")
        );
        configuration.requestData = new HttpActionNodeV1Config.RequestData();
        configuration.requestData.requestContentType = HttpActionNodeV1Config.RequestData.REQUEST_CONTENT_TYPE_OPT_MULTIPARTFORMDATA;
        configuration.requestData.requestFormFields = List.of(
                Map.of("name", "feld-{{ $.typ }}", "value", "{{ $.nachricht }}")
        );

        node.init(context(configuration));

        // Header lines must not be split, while the HTTP client encodes form fields, line breaks included.
        assertEquals(TemplateOutputFormat.SingleLineText, templateRenderService.outputFormats.get("X-Antrag-{{ $.typ }}"));
        assertEquals(TemplateOutputFormat.SingleLineText, templateRenderService.outputFormats.get("{{ $.person.vorname }}"));
        assertEquals(TemplateOutputFormat.SingleLineText, templateRenderService.outputFormats.get("feld-{{ $.typ }}"));
        assertEquals(TemplateOutputFormat.PlainText, templateRenderService.outputFormats.get("{{ $.nachricht }}"));
    }

    @Test
    void init_ShouldBuildJsonBodyFromProcessDataKey() throws Exception {
        when(httpService.request(eq(HttpMethod.POST), any(), anyString(), any()))
                .thenReturn(ResponseEntity.ok("ok".getBytes(StandardCharsets.UTF_8)));

        var configuration = baseConfig("POST", "https://prosuna.test/api", "text", "200");
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

    @ParameterizedTest
    @ValueSource(strings = {"text", "file"})
    void init_ShouldRejectUnexpectedStatusWithoutExposingTheResponseBody(String responseType) throws Exception {
        when(httpService.request(eq(HttpMethod.GET), any(), any()))
                .thenReturn(ResponseEntity
                        .status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .header("X-Error-Code", "unprocessable")
                        .body("internal-response-100%".getBytes(StandardCharsets.UTF_8)));

        var configuration = baseConfig("GET", "https://prosuna.test/api", responseType, "200");

        var exception = assertThrows(
                ProcessNodeExecutionExceptionIO.class,
                () -> node.init(context(configuration))
        );
        assertEquals("Die HTTP-Schnittstelle hat mit dem Statuscode 422 geantwortet. Für diese Aufgabe sind folgende Statuscodes zugelassen: [200].", exception.getMessage());
        verifyNoInteractions(processInstanceAttachmentService, processInstanceAttachmentSetService);
    }

    @Test
    void init_ShouldReportInvalidUriWithoutExposingTheOriginalUrl() {
        var configuration = baseConfig("GET", "https://user:private-password@prosuna.test/bad path?token=private-token", "text", "200");

        var exception = assertThrows(ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> node.init(context(configuration)));

        assertEquals("Die Zieladresse (URL) für den HTTP-Aufruf ist ungültig.", exception.getMessage());
        assertNotNull(exception.getCause());
        verifyNoInteractions(httpService);
    }

    @Test
    void init_ShouldPreserveConnectionFailureCauseWithoutExposingClientDetails() throws Exception {
        var cause = new HttpConnectionException(new IOException("internal host detail 100%"));
        when(httpService.request(eq(HttpMethod.GET), any(), any())).thenThrow(cause);

        var exception = assertThrows(ProcessNodeExecutionExceptionIO.class,
                () -> node.init(context(baseConfig("GET", "https://prosuna.test/api", "text", "200"))));

        assertEquals("Bei der Kommunikation mit der HTTP-Schnittstelle ist ein Fehler aufgetreten.", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void init_ShouldPreserveUnexpectedFailureCauseWithoutExposingClientDetails() throws Exception {
        var cause = new IllegalStateException("internal client detail 100%");
        when(httpService.request(eq(HttpMethod.GET), any(), any())).thenThrow(cause);

        var exception = assertThrows(ProcessNodeExecutionExceptionUnknown.class,
                () -> node.init(context(baseConfig("GET", "https://prosuna.test/api", "text", "200"))));

        assertEquals("Die HTTP-Anfrage konnte nicht vorbereitet oder ausgeführt werden.", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void init_ShouldPreserveJsonParserCauseWithoutExposingResponseContents() throws Exception {
        when(httpService.request(eq(HttpMethod.GET), any(), any()))
                .thenReturn(ResponseEntity.ok("{\"credential\":\"internal-100%\"".getBytes(StandardCharsets.UTF_8)));

        var exception = assertThrows(ProcessNodeExecutionExceptionInvalidDataType.class,
                () -> node.init(context(baseConfig("GET", "https://prosuna.test/api", "json", "200"))));

        assertEquals("Die Antwort der HTTP-Schnittstelle konnte nicht als JSON verarbeitet werden.", exception.getMessage());
        assertInstanceOf(JacksonException.class, exception.getCause());
    }

    @ParameterizedTest
    @CsvSource({
            "basic, Geben Sie den Nutzernamen und das gespeicherte Passwort für die Basic-Authentifizierung an.",
            "bearer, Geben Sie den Bearer-Token für den Zugang zur HTTP-Schnittstelle an."
    })
    void init_ShouldExplainMissingAuthenticationSettings(String authMethod, String expectedMessage) {
        var configuration = baseConfig("GET", "https://prosuna.test/api", "text", "200");
        configuration.authMethod = authMethod;

        var exception = assertThrows(ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> node.init(context(configuration)));

        assertEquals(expectedMessage, exception.getMessage());
        verifyNoInteractions(httpService);
    }

    @Test
    void init_ShouldPreservePasswordDecryptionCauseWithoutExposingSecretDetails() throws Exception {
        var configuration = baseConfig("GET", "https://prosuna.test/api", "text", "200");
        configuration.authMethod = "basic";
        configuration.basicAuthConfig = new HttpActionNodeV1Config.BasicAuthConfig();
        configuration.basicAuthConfig.username = "service-user";
        var secretKey = UUID.randomUUID();
        configuration.basicAuthConfig.passwordSecretKey = secretKey.toString();
        var secret = new SecretEntity();
        secret.setKey(secretKey);
        var cause = new IllegalStateException("internal decryption detail 100%");
        when(secretService.retrieve(secretKey)).thenReturn(Optional.of(secret));
        when(secretService.decrypt(secret)).thenThrow(cause);

        var exception = assertThrows(ProcessNodeExecutionExceptionUnknown.class,
                () -> node.init(context(configuration)));

        assertEquals("Das gespeicherte Passwort für die Basic-Authentifizierung konnte nicht gelesen werden.", exception.getMessage());
        assertSame(cause, exception.getCause());
        verifyNoInteractions(httpService);
    }

    @Test
    void init_ShouldPreserveResponseAttachmentStorageCauseWithoutExposingStorageDetails() throws Exception {
        when(httpService.request(eq(HttpMethod.GET), any(), any()))
                .thenReturn(ResponseEntity.ok("file".getBytes(StandardCharsets.UTF_8)));
        var cause = ResponseException.internalServerError("internal storage detail 100%");
        when(processInstanceAttachmentSetService.create(any(ProcessInstanceAttachmentSetEntity.class))).thenThrow(cause);

        var exception = assertThrows(ProcessNodeExecutionExceptionUnknown.class,
                () -> node.init(context(baseConfig("GET", "https://prosuna.test/file", "file", "200"))));

        assertEquals("Die als Datei empfangene HTTP-Antwort konnte nicht als Anlage zum Vorgang gespeichert werden.", exception.getMessage());
        assertSame(cause, exception.getCause());
        verify(processInstanceAttachmentService, never()).create(any(ProcessInstanceAttachmentEntity.class));
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
        when(processInstanceAttachmentSetService.create(any(ProcessInstanceAttachmentSetEntity.class)))
                .thenAnswer(invocation -> invocation
                        .getArgument(0, ProcessInstanceAttachmentSetEntity.class)
                        .setId(321));

        var configuration = baseConfig("GET", "https://prosuna.test/file", "file", "200");
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

        @SuppressWarnings("unchecked")
        var files = (List<FileUploadInputElementItem>) result.getNodeData().get("files");
        assertEquals(1, files.size());
        assertEquals("document.pdf", files.getFirst().getName());
        assertEquals("document.pdf", files.getFirst().getOriginalFileName());
        assertEquals(3, files.getFirst().getSize());
        assertTrue(files.getFirst().getUri().startsWith("process-instance-attachment:"));
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

    private static class PassthroughTemplateRenderService extends TemplateRenderService {
        private final Map<String, TemplateOutputFormat> outputFormats = new HashMap<>();

        private PassthroughTemplateRenderService() {
            super(null);
        }

        @Override
        public String interpolate(ProcessExecutionData foldedProcessData, String template, TemplateOutputFormat outputFormat) {
            outputFormats.put(template, outputFormat);
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
