package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import de.aivot.GoverBackend.asset.repositories.VStorageIndexItemWithAssetRepository;
import de.aivot.GoverBackend.asset.services.AssetService;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.models.ProcessExecutionData;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionContextInit;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionLogger;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.GoverBackend.process.services.ProcessInstanceAttachmentService;
import de.aivot.GoverBackend.process.services.TemplateRenderService;
import de.aivot.GoverBackend.services.PdfService;
import de.aivot.GoverBackend.storage.services.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static de.aivot.GoverBackend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PdfActionNodeV1Test {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final Integer NODE_ID = 123;
    private static final Long PROCESS_INSTANCE_ID = 99L;
    private static final Long TASK_ID = 456L;

    private PdfService pdfService;
    private ProcessInstanceAttachmentService processInstanceAttachmentService;
    private PdfActionNodeV1 node;

    @BeforeEach
    void setUp() throws Exception {
        pdfService = mock(PdfService.class);
        processInstanceAttachmentService = mock(ProcessInstanceAttachmentService.class);

        when(pdfService.generatePdfFromHtml(anyString(), anyString(), anyString()))
                .thenReturn("pdf-bytes".getBytes(StandardCharsets.UTF_8));
        when(processInstanceAttachmentService.create(any(ProcessInstanceAttachmentEntity.class)))
                .thenAnswer(invocation -> {
                    var attachment = invocation.getArgument(0, ProcessInstanceAttachmentEntity.class);
                    return attachment
                            .setKey(UUID.randomUUID())
                            .setStorageProviderId(7)
                            .setStoragePathFromRoot("attachments/report.pdf");
                });

        node = new PdfActionNodeV1(
                pdfService,
                new PassthroughTemplateRenderService(),
                processInstanceAttachmentService,
                mock(AssetService.class),
                mock(VStorageIndexItemWithAssetRepository.class),
                mock(StorageService.class)
        );
    }

    @Test
    void init_SplitsHtmlBlocksIntoContentHeaderAndFooter() throws Exception {
        var html = "<html><body>::footer::<div>Footer</div></body></html>\n"
                + "<html><body><main>Body</main></body></html>\n"
                + "<html><body>::header::<div>Header</div></body></html>";

        var result = assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, node.init(context(html)));

        verify(pdfService).generatePdfFromHtml(
                "<html><body><main>Body</main></body></html>",
                "<html><body><div>Header</div></body></html>",
                "<html><body><div>Footer</div></body></html>"
        );
        assertEquals("report.pdf", result.getNodeData().get("fileName"));
        assertEquals("application/pdf", result.getNodeData().get("mimeType"));
    }

    @Test
    void init_RejectsMultipleContentHtmlBlocksWithoutMarkers() throws Exception {
        var html = "<html><body><main>One</main></body></html>"
                + "<html><body><main>Two</main></body></html>";

        assertThrows(
                ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> node.init(context(html))
        );

        verify(pdfService, never()).generatePdfFromHtml(anyString(), anyString(), anyString());
    }

    private static ProcessNodeExecutionContextInit context(String html) {
        return new ProcessNodeExecutionContextInit(
                logger(),
                processNode(),
                processInstance(),
                task(),
                null,
                new ProcessExecutionData(),
                runtime(
                        PdfActionNodeV1.PdfActionNodeConfig.FILE_NAME_FIELD_ID, "report",
                        PdfActionNodeV1.PdfActionNodeConfig.CONTENT_HTML_SOURCE_FIELD_ID, PdfActionNodeV1.PdfActionNodeConfig.CONTENT_HTML_SOURCE_FIELD_OPTION_CODE,
                        PdfActionNodeV1.PdfActionNodeConfig.CONTENT_HTML_CODE_FIELD_ID, html
                )
        );
    }

    private static ProcessNodeEntity processNode() {
        return new ProcessNodeEntity()
                .setId(NODE_ID)
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setName("PDF")
                .setDataKey("pdfNode")
                .setProcessNodeDefinitionKey("de.aivot.core.pdf")
                .setProcessNodeDefinitionVersion(1)
                .setConfiguration(new AuthoredElementValues())
                .setOutputMappings(Map.of());
    }

    private static ProcessInstanceEntity processInstance() {
        var now = LocalDateTime.now();

        return new ProcessInstanceEntity()
                .setId(PROCESS_INSTANCE_ID)
                .setAccessKey(UUID.randomUUID())
                .setProcessId(PROCESS_ID)
                .setInitialProcessVersion(PROCESS_VERSION)
                .setStatus(ProcessInstanceStatus.Running)
                .setAssignedFileNumbers(java.util.List.of())
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
        public String interpolate(Map<String, Object> processData, String template) {
            return template;
        }
    }

    @FunctionalInterface
    private interface MethodHandler {
        Object invoke(String methodName, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, MethodHandler handler) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> {
                    var methodName = method.getName();
                    return switch (methodName) {
                        case "toString" -> type.getSimpleName() + "TestProxy";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> handler.invoke(methodName, args);
                    };
                }
        );
    }

    private static Object unsupported(String methodName) {
        throw new UnsupportedOperationException("Unexpected repository method call in test: " + methodName);
    }
}
