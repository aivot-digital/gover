package de.aivot.gover.backend.plugins.core.v1.nodes.actions;

import de.aivot.gover.backend.elements.models.AuthoredElementValues;
import de.aivot.gover.backend.elements.models.elements.form.input.ProcessInstanceAttachmentSetSelectElement;
import de.aivot.gover.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.gover.backend.identity.models.IdentityDataMap;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.enums.ProcessInstanceStatus;
import de.aivot.gover.backend.process.enums.ProcessTaskStatus;
import de.aivot.gover.backend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.gover.backend.process.models.ProcessExecutionData;
import de.aivot.gover.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.gover.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.gover.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.gover.backend.process.services.ProcessInstanceAttachmentSetService;
import de.aivot.gover.backend.process.services.TemplateRenderService;
import de.aivot.gover.backend.storage.entities.StorageProviderEntity;
import de.aivot.gover.backend.storage.enums.StorageProviderStatus;
import de.aivot.gover.backend.storage.enums.StorageProviderType;
import de.aivot.gover.backend.storage.models.StorageDocument;
import de.aivot.gover.backend.storage.models.StorageItemMetadata;
import de.aivot.gover.backend.storage.repositories.StorageProviderRepository;
import de.aivot.gover.backend.storage.services.StorageService;
import de.aivot.gover.backend.utils.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreAttachmentSetActionNodeV1Test {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final Integer NODE_ID = 123;
    private static final Long PROCESS_INSTANCE_ID = 99L;
    private static final Long TASK_ID = 456L;
    private static final Integer TARGET_STORAGE_PROVIDER_ID = 77;

    private ProcessInstanceAttachmentService processInstanceAttachmentService;
    private ProcessInstanceAttachmentSetService processInstanceAttachmentSetService;
    private StorageService storageService;
    private StorageProviderRepository storageProviderRepository;
    private StoreAttachmentSetActionNodeV1 node;
    private List<StoredDocument> storedDocuments;

    @BeforeEach
    void setUp() throws Exception {
        processInstanceAttachmentService = mock(ProcessInstanceAttachmentService.class);
        processInstanceAttachmentSetService = mock(ProcessInstanceAttachmentSetService.class);
        storageService = mock(StorageService.class);
        storageProviderRepository = mock(StorageProviderRepository.class);
        storedDocuments = new ArrayList<>();

        when(storageProviderRepository.findById(TARGET_STORAGE_PROVIDER_ID))
                .thenReturn(Optional.of(storageProvider(TARGET_STORAGE_PROVIDER_ID, "Ziel", false)));
        when(storageService.storeDocument(
                eq(TARGET_STORAGE_PROVIDER_ID),
                anyString(),
                any(InputStream.class),
                any(StorageItemMetadata.class)
        )).thenAnswer(invocation -> {
            var path = invocation.getArgument(1, String.class);
            var content = invocation.getArgument(2, InputStream.class).readAllBytes();
            storedDocuments.add(new StoredDocument(path, new String(content, StandardCharsets.UTF_8)));
            return new StorageDocument(
                    path,
                    StringUtils.getLastPathSegment(path),
                    (long) content.length,
                    StorageItemMetadata.empty()
            );
        });

        node = new StoreAttachmentSetActionNodeV1(
                new CaseIdTemplateRenderService(),
                processInstanceAttachmentService,
                processInstanceAttachmentSetService,
                storageService,
                storageProviderRepository
        );
    }

    @Test
    void getConfigurationLayout_UsesSingleProcessInstanceAttachmentSetSelector() throws Exception {
        when(storageProviderRepository.findAll()).thenReturn(List.of(
                storageProvider(1, "Read-only", true),
                storageProvider(2, "Writable", false)
        ));

        var layout = node.getConfigurationLayout(null);

        var attachmentSetField = layout
                .findChild(StoreAttachmentSetActionNodeV1.StoreAttachmentSetActionNodeConfig.ATTACHMENT_SET_DATA_KEYS_FIELD_ID, ProcessInstanceAttachmentSetSelectElement.class)
                .orElseThrow();
        assertEquals(1, attachmentSetField.getMinItems());
        assertEquals(1, attachmentSetField.getMaxItems());

        var storageProviderField = layout
                .findChild(StoreAttachmentSetActionNodeV1.StoreAttachmentSetActionNodeConfig.STORAGE_PROVIDER_ID_FIELD_ID, SelectInputElement.class)
                .orElseThrow();
        assertEquals(1, storageProviderField.getOptions().size());
        assertEquals("2", storageProviderField.getOptions().getFirst().getValue());
    }

    @Test
    void init_ReplacesHashWithOneBasedIndexAndPreservesOriginalExtensions() throws Exception {
        arrangeAttachmentSet(
                attachment("zeta.docx", 11, "/source/zeta.docx"),
                attachment("alpha.PDF", 11, "/source/alpha.pdf")
        );

        var configuration = configuration("/case/{{caseId}}/attachment-#.ignored");
        var result = assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, node.init(context(configuration)));

        assertEquals("output", result.getViaPort());
        assertEquals(List.of(
                "/case/123/attachment-1.PDF",
                "/case/123/attachment-2.docx"
        ), result.getNodeData().get("storagePathsFromRoot"));
        assertEquals(2, result.getNodeData().get("count"));
        assertEquals(List.of(
                new StoredDocument("/case/123/attachment-1.PDF", "pdf"),
                new StoredDocument("/case/123/attachment-2.docx", "docx")
        ), storedDocuments);

        verify(storageService, times(1)).createFolder(TARGET_STORAGE_PROVIDER_ID, "/case/");
        verify(storageService, times(1)).createFolder(TARGET_STORAGE_PROVIDER_ID, "/case/123/");
    }

    @Test
    void init_AddsNumericSuffixFromSecondAttachmentWhenNoHashExists() throws Exception {
        arrangeAttachmentSet(
                attachment("alpha.PDF", 11, "/source/alpha.pdf"),
                attachment("zeta.docx", 11, "/source/zeta.docx")
        );

        var configuration = configuration("/case/{{caseId}}/attachment.pdf");
        node.init(context(configuration));

        assertEquals(List.of(
                new StoredDocument("/case/123/attachment.PDF", "pdf"),
                new StoredDocument("/case/123/attachment-2.docx", "docx")
        ), storedDocuments);
    }

    @Test
    void init_FailsWhenSelectedAttachmentSetIsEmpty() {
        when(processInstanceAttachmentSetService.findAllByProcessInstanceIdAndDataKey(PROCESS_INSTANCE_ID, "documents"))
                .thenReturn(List.of(new ProcessInstanceAttachmentSetEntity().setId(321)));
        when(processInstanceAttachmentService.findAllByAttachmentSetId(321))
                .thenReturn(List.of());

        assertThrows(
                ProcessNodeExecutionExceptionMissingValue.class,
                () -> node.init(context(configuration("/case/{{caseId}}/attachment.pdf")))
        );
    }

    @Test
    void cleanConfigurationForExport_RemovesStorageProviderId() {
        var configuration = new AuthoredElementValues();
        configuration.put(StoreAttachmentSetActionNodeV1.StoreAttachmentSetActionNodeConfig.STORAGE_PROVIDER_ID_FIELD_ID, "77");
        configuration.put(StoreAttachmentSetActionNodeV1.StoreAttachmentSetActionNodeConfig.TARGET_PATH_FIELD_ID, "/case/file");

        var cleaned = node.cleanConfigurationForExport(configuration);

        assertNull(cleaned.get(StoreAttachmentSetActionNodeV1.StoreAttachmentSetActionNodeConfig.STORAGE_PROVIDER_ID_FIELD_ID));
        assertEquals("/case/file", cleaned.get(StoreAttachmentSetActionNodeV1.StoreAttachmentSetActionNodeConfig.TARGET_PATH_FIELD_ID));
    }

    private void arrangeAttachmentSet(ProcessInstanceAttachmentEntity... attachments) throws Exception {
        when(processInstanceAttachmentSetService.findAllByProcessInstanceIdAndDataKey(PROCESS_INSTANCE_ID, "documents"))
                .thenReturn(List.of(new ProcessInstanceAttachmentSetEntity().setId(321)));
        when(processInstanceAttachmentService.findAllByAttachmentSetId(321))
                .thenReturn(List.of(attachments));

        for (var attachment : attachments) {
            when(storageService.getDocumentContent(
                    attachment.getStorageProviderId(),
                    attachment.getStoragePathFromRoot()
            )).thenReturn(new ByteArrayInputStream(attachment.getFileName().startsWith("alpha")
                    ? "pdf".getBytes()
                    : "docx".getBytes()));
        }
    }

    private static ProcessInstanceAttachmentEntity attachment(String fileName,
                                                              Integer storageProviderId,
                                                              String storagePathFromRoot) {
        return new ProcessInstanceAttachmentEntity()
                .setKey(UUID.randomUUID())
                .setFileName(fileName)
                .setAttachmentSetId(321)
                .setProcessInstanceId(PROCESS_INSTANCE_ID)
                .setStorageProviderId(storageProviderId)
                .setStoragePathFromRoot(storagePathFromRoot);
    }

    private static StoreAttachmentSetActionNodeV1.StoreAttachmentSetActionNodeConfig configuration(String targetPath) {
        var configuration = new StoreAttachmentSetActionNodeV1.StoreAttachmentSetActionNodeConfig();
        configuration.storageProviderId = TARGET_STORAGE_PROVIDER_ID.toString();
        configuration.attachmentSetDataKeys = List.of("documents");
        configuration.targetPath = targetPath;
        return configuration;
    }

    private static ProcessNodeExecutionInitContext<StoreAttachmentSetActionNodeV1.StoreAttachmentSetActionNodeConfig> context(
            StoreAttachmentSetActionNodeV1.StoreAttachmentSetActionNodeConfig configuration
    ) {
        return new ProcessNodeExecutionInitContext<>(
                logger(),
                processNode(),
                processInstance(),
                task(),
                null,
                new ProcessExecutionData(),
                configuration
        );
    }

    private static ProcessNodeEntity processNode() {
        return new ProcessNodeEntity()
                .setId(NODE_ID)
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setName("Store attachment set")
                .setDataKey("storeAttachmentSet")
                .setProcessNodeDefinitionKey("de.aivot.core.store_attachment_set")
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
                mock(ProcessInstanceHistoryEventRepository.class)
        );
    }

    private static StorageProviderEntity storageProvider(Integer id,
                                                         String name,
                                                         boolean readOnly) {
        return new StorageProviderEntity()
                .setId(id)
                .setName(name)
                .setDescription("")
                .setType(StorageProviderType.Assets)
                .setStatus(StorageProviderStatus.Synced)
                .setReadOnlyStorage(readOnly);
    }

    private record StoredDocument(String path, String content) {
    }

    private static class CaseIdTemplateRenderService extends TemplateRenderService {
        private CaseIdTemplateRenderService() {
            super(null);
        }

        @Override
        public String interpolate(ProcessExecutionData foldedProcessData, String template) {
            return template == null ? null : template.replace("{{caseId}}", "123");
        }
    }
}
