package de.aivot.gover.backend.plugins.core.v1.nodes.actions;

import de.aivot.gover.backend.asset.entities.AssetEntity;
import de.aivot.gover.backend.asset.services.AssetService;
import de.aivot.gover.backend.elements.models.AuthoredElementValues;
import de.aivot.gover.backend.elements.models.elements.form.input.HtmlTemplateInputElementResolver;
import de.aivot.gover.backend.elements.models.elements.form.input.HtmlTemplateInputElementValue;
import de.aivot.gover.backend.identity.models.IdentityDataMap;
import de.aivot.gover.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.gover.backend.plugins.core.v1.nodes.actions.PdfActionNodeV1;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.enums.ProcessInstanceStatus;
import de.aivot.gover.backend.process.enums.ProcessTaskStatus;
import de.aivot.gover.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.gover.backend.process.models.ProcessExecutionData;
import de.aivot.gover.backend.process.models.ProcessNodeDefinitionMetadata;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.gover.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.gover.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.gover.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.gover.backend.process.services.ProcessInstanceAttachmentSetService;
import de.aivot.gover.backend.process.services.TemplateRenderService;
import de.aivot.gover.backend.services.PdfService;
import de.aivot.gover.backend.storage.services.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private ProcessInstanceAttachmentSetService processInstanceAttachmentSetService;
    private AssetService assetService;
    private StorageService storageService;
    private PdfActionNodeV1 node;

    @BeforeEach
    void setUp() throws Exception {
        pdfService = mock(PdfService.class);
        processInstanceAttachmentService = mock(ProcessInstanceAttachmentService.class);
        processInstanceAttachmentSetService = mock(ProcessInstanceAttachmentSetService.class);
        assetService = mock(AssetService.class);
        storageService = mock(StorageService.class);

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
        when(processInstanceAttachmentSetService.create(any(ProcessInstanceAttachmentSetEntity.class)))
                .thenAnswer(invocation -> invocation
                        .getArgument(0, ProcessInstanceAttachmentSetEntity.class)
                        .setId(321));

        node = createNode(new PassthroughTemplateRenderService());
    }

    @Test
    void init_SplitsHtmlBlocksIntoContentHeaderAndFooter() throws Exception {
        var html = "<html><body><div>Header</div></body></html>\n"
                + "<!-- KOPFZEILE -->\n"
                + "<html><body><main>Body</main></body></html>\n"
                + "<!-- FUSSZEILE -->\n"
                + "<html><body><div>Footer</div></body></html>";

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

    @Test
    void init_RendersAssetTemplateBeforeSplittingHtmlSections() throws Exception {
        var assetKey = UUID.randomUUID();
        var html = "<html><head>{% useBlock sharedStyles %}</head><body><div>Header</div></body></html>"
                + "<!-- KOPFZEILE -->"
                + "<html><head>{% useBlock sharedStyles %}</head><body><main>Body</main></body></html>"
                + "{% block sharedStyles %}<style>.shared{color:red;}</style>{% endblock %}";

        when(assetService.retrieve(assetKey)).thenReturn(Optional.of(
                new AssetEntity()
                        .setKey(assetKey)
                        .setPrivate(false)
                        .setStorageProviderId(11)
                        .setStoragePathFromRoot("templates/standardbrief.html")
        ));
        when(storageService.getDocumentContent(11, "templates/standardbrief.html"))
                .thenReturn(new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)));

        var assetNode = createNode(new TemplateRenderService(new JavascriptEngineFactoryService(List.of())));
        var result = assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, assetNode.init(assetContext(assetKey)));

        verify(pdfService).generatePdfFromHtml(
                "<html><head><style>.shared{color:red;}</style></head><body><main>Body</main></body></html>",
                "<html><head><style>.shared{color:red;}</style></head><body><div>Header</div></body></html>",
                ""
        );
        assertEquals("report.pdf", result.getNodeData().get("fileName"));
    }

    @Test
    void getMetadata_ShouldForwardPdfAttachmentSetAsSingleFile() {
        var metadata = node.getMetadata(processNode(), codeConfiguration("<html></html>"), ProcessNodeDefinitionMetadata.empty());

        assertEquals(1, metadata.forwardedAttachmentSets().size());

        var attachmentSet = metadata.forwardedAttachmentSets().getFirst();
        assertEquals("pdfNode", attachmentSet.dataKey());
        assertEquals("report", attachmentSet.label());
        assertFalse(attachmentSet.isMultifile());
    }

    private static ProcessNodeExecutionInitContext context(String html) {
        return new ProcessNodeExecutionInitContext(
                logger(),
                processNode(),
                processInstance(),
                task(),
                null,
                new ProcessExecutionData(),
                codeConfiguration(html)
        );
    }

    private static ProcessNodeExecutionInitContext assetContext(UUID assetKey) {
        return new ProcessNodeExecutionInitContext(
                logger(),
                processNode(),
                processInstance(),
                task(),
                null,
                new ProcessExecutionData(),
                assetConfiguration(assetKey)
        );
    }

    private static PdfActionNodeV1.PdfActionNodeConfig codeConfiguration(String html) {
        var configuration = new PdfActionNodeV1.PdfActionNodeConfig();
        configuration.fileName = "report";
        configuration.contentHtmlSource = PdfActionNodeV1.PdfActionNodeConfig.CONTENT_HTML_SOURCE_FIELD_OPTION_CODE;
        configuration.contentHtml = html;
        return configuration;
    }

    private static PdfActionNodeV1.PdfActionNodeConfig assetConfiguration(UUID assetKey) {
        var configuration = new PdfActionNodeV1.PdfActionNodeConfig();
        configuration.fileName = "report";
        configuration.contentHtmlSource = PdfActionNodeV1.PdfActionNodeConfig.CONTENT_HTML_SOURCE_FIELD_OPTION_ASSET_KEY;
        configuration.contentHtmlTemplate = new HtmlTemplateInputElementValue()
                .setAssetKey(assetKey.toString())
                .setSlots(Map.of());
        return configuration;
    }

    private PdfActionNodeV1 createNode(TemplateRenderService templateRenderService) {
        var htmlTemplateInputElementResolver = new HtmlTemplateInputElementResolver(
                assetService,
                storageService,
                templateRenderService
        );

        return new PdfActionNodeV1(
                pdfService,
                templateRenderService,
                processInstanceAttachmentService,
                processInstanceAttachmentSetService,
                htmlTemplateInputElementResolver
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
        var now = Instant.now();

        return new ProcessInstanceEntity()
                .setId(PROCESS_INSTANCE_ID)
                .setAccessKey(UUID.randomUUID())
                .setProcessId(PROCESS_ID)
                .setInitialProcessVersion(PROCESS_VERSION)
                .setStatus(ProcessInstanceStatus.Running)
                .setAssignedFileNumbers(java.util.List.of())
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
