package de.aivot.gover.backend.plugins.core.v1.nodes.actions;

import de.aivot.gover.backend.elements.models.AuthoredElementValues;
import de.aivot.gover.backend.elements.models.elements.form.input.CheckboxInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.ProcessInstanceAttachmentSetSelectElement;
import de.aivot.gover.backend.elements.models.elements.form.input.StoragePathSelectorInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.StoragePathSelectorInputElementValue;
import de.aivot.gover.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.gover.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.gover.backend.identity.models.IdentityDataMap;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceEventEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.enums.ProcessInstanceStatus;
import de.aivot.gover.backend.process.enums.ProcessNodeExecutionLogLevel;
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
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreAttachmentSetActionNodeV1Test {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final Integer NODE_ID = 123;
    private static final Long PROCESS_INSTANCE_ID = 99L;
    private static final Long TASK_ID = 456L;
    private static final Integer TARGET_STORAGE_PROVIDER_ID = 77;
    private static final Integer SECOND_TARGET_STORAGE_PROVIDER_ID = 88;

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
        when(storageProviderRepository.findById(SECOND_TARGET_STORAGE_PROVIDER_ID))
                .thenReturn(Optional.of(storageProvider(SECOND_TARGET_STORAGE_PROVIDER_ID, "Weiteres Ziel", false)));
        when(storageService.storeDocument(
                any(Integer.class),
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
    void getConfigurationLayout_UsesReplicatingAttachmentSetConfigWithStoragePathSelector() throws Exception {
        var layout = node.getConfigurationLayout(null);

        var attachmentSets = layout
                .findChild(StoreAttachmentSetActionNodeV1.StoreAttachmentSetActionNodeConfig.ATTACHMENT_SETS_FIELD_ID, ReplicatingContainerLayoutElement.class)
                .orElseThrow();
        assertEquals("Anlagensatz #", attachmentSets.getHeadlineTemplate());

        var attachmentSetField = layout
                .findChild(StoreAttachmentSetActionNodeV1.AttachmentSetStorageConfig.ATTACHMENT_SET_DATA_KEYS_FIELD_ID, ProcessInstanceAttachmentSetSelectElement.class)
                .orElseThrow();
        assertEquals(1, attachmentSetField.getMinItems());
        assertEquals(1, attachmentSetField.getMaxItems());

        var storagePathField = layout
                .findChild(StoreAttachmentSetActionNodeV1.AttachmentSetStorageConfig.STORAGE_PATH_FIELD_ID, StoragePathSelectorInputElement.class)
                .orElseThrow();
        assertEquals(List.of(StorageProviderType.Assets, StorageProviderType.External), storagePathField.getAllowedStorageProviderTypes());
        assertEquals("Wählen Sie den beschreibbaren Speicheranbieter aus, in dem der Anlagensatz gespeichert wird.", storagePathField.getStorageProviderSelectHint());

        var ignoreEmptyAttachmentSetField = layout
                .findChild(StoreAttachmentSetActionNodeV1.AttachmentSetStorageConfig.IGNORE_EMPTY_ATTACHMENT_SET_FIELD_ID, CheckboxInputElement.class)
                .orElseThrow();
        assertEquals("Optionalen Anlagensatz ignorieren, falls keine Dateien vorhanden sind", ignoreEmptyAttachmentSetField.getLabel());

        var fileNameField = layout
                .findChild(StoreAttachmentSetActionNodeV1.AttachmentSetStorageConfig.FILE_NAME_FIELD_ID, TextInputElement.class)
                .orElseThrow();
        assertNotNull(fileNameField.getVisibility());
    }

    @Test
    void init_StoresMultipleAttachmentSetsInPositionOrderUsingRenderedFolderPathsAndOriginalFileNames() throws Exception {
        arrangeAttachmentSet(
                "documents",
                321,
                attachment("zeta.docx", 2, 11, "/source/zeta.docx"),
                attachment("alpha.PDF", 1, 11, "/source/alpha.pdf")
        );
        arrangeAttachmentSet(
                "proofs",
                322,
                attachment("hundeversicherung.pdf", 1, 12, "/source/hundeversicherung.pdf")
        );

        var configuration = configuration(
                attachmentSetConfig("documents", TARGET_STORAGE_PROVIDER_ID, "/case/{{caseId}}/documents"),
                attachmentSetConfig("proofs", SECOND_TARGET_STORAGE_PROVIDER_ID, "/proofs/")
        );
        var result = assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, node.init(context(configuration)));

        assertEquals("output", result.getViaPort());
        assertEquals(List.of(
                "/case/123/documents/alpha.PDF",
                "/case/123/documents/zeta.docx",
                "/proofs/hundeversicherung.pdf"
        ), result.getNodeData().get("storagePathsFromRoot"));
        assertEquals(List.of("alpha.PDF", "zeta.docx", "hundeversicherung.pdf"), result.getNodeData().get("fileNames"));
        assertEquals(3, result.getNodeData().get("count"));
        assertEquals(List.of(
                new StoredDocument("/case/123/documents/alpha.PDF", "alpha.PDF"),
                new StoredDocument("/case/123/documents/zeta.docx", "zeta.docx"),
                new StoredDocument("/proofs/hundeversicherung.pdf", "hundeversicherung.pdf")
        ), storedDocuments);

        @SuppressWarnings("unchecked")
        var results = (List<Map<String, Object>>) result.getNodeData().get("results");
        assertEquals(2, results.size());
        assertEquals("documents", results.getFirst().get("attachmentSetDataKey"));
        assertEquals(TARGET_STORAGE_PROVIDER_ID, results.getFirst().get("storageProviderId"));
    }

    @Test
    void init_CustomizesFileNamesAndPostfixesDuplicates() throws Exception {
        arrangeAttachmentSet(
                "documents",
                321,
                attachment("alpha.PDF", 1, 11, "/source/alpha.pdf"),
                attachment("beta.pdf", 2, 11, "/source/beta.pdf")
        );

        var configuration = configuration(
                attachmentSetConfig("documents", TARGET_STORAGE_PROVIDER_ID, "/case/{{caseId}}/#", true, "stored-name.ignored")
        );
        node.init(context(configuration));

        assertEquals(List.of(
                new StoredDocument("/case/123/1/stored-name.pdf", "alpha.PDF"),
                new StoredDocument("/case/123/2/stored-name-2.pdf", "beta.pdf")
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
                () -> node.init(context(configuration(attachmentSetConfig("documents", TARGET_STORAGE_PROVIDER_ID, "/case/{{caseId}}/"))))
        );
    }

    @Test
    void init_FailsWhenSelectedAttachmentSetIsMissing() {
        when(processInstanceAttachmentSetService.findAllByProcessInstanceIdAndDataKey(PROCESS_INSTANCE_ID, "documents"))
                .thenReturn(List.of());

        assertThrows(
                ProcessNodeExecutionExceptionMissingValue.class,
                () -> node.init(context(configuration(attachmentSetConfig("documents", TARGET_STORAGE_PROVIDER_ID, "/case/{{caseId}}/"))))
        );
    }

    @Test
    void init_SkipsOptionalAttachmentSetWhenEmptyAndLogs() throws Exception {
        var eventRepository = mock(ProcessInstanceHistoryEventRepository.class);
        when(processInstanceAttachmentSetService.findAllByProcessInstanceIdAndDataKey(PROCESS_INSTANCE_ID, "documents"))
                .thenReturn(List.of(new ProcessInstanceAttachmentSetEntity()
                        .setId(321)
                        .setDataKey("documents")
                        .setName("Dokumente")));
        when(processInstanceAttachmentService.findAllByAttachmentSetId(321))
                .thenReturn(List.of());

        var result = assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, node.init(context(
                configuration(optionalAttachmentSetConfig("documents", TARGET_STORAGE_PROVIDER_ID, "/case/{{caseId}}/")),
                eventRepository
        )));

        assertEquals(0, result.getNodeData().get("count"));
        assertEquals(List.of(), result.getNodeData().get("storagePathsFromRoot"));
        assertEquals(List.of(), result.getNodeData().get("fileNames"));
        assertEquals(List.of(), result.getNodeData().get("results"));
        assertTrue(storedDocuments.isEmpty());
        verify(storageService, never()).storeDocument(any(Integer.class), anyString(), any(InputStream.class), any(StorageItemMetadata.class));

        var event = captureSingleEvent(eventRepository);
        assertEquals(ProcessNodeExecutionLogLevel.Info, event.getLevel());
        assertEquals(false, event.getTechnical());
        assertEquals(true, event.getAudit());
        assertEquals("Anlagensatz 'Dokumente' enthielt keine Dateien. Speichervorgang für diesen Anlagensatz übersprungen.", event.getMessage());
    }

    @Test
    void init_SkipsOptionalAttachmentSetWhenMissingAndLogs() throws Exception {
        var eventRepository = mock(ProcessInstanceHistoryEventRepository.class);
        when(processInstanceAttachmentSetService.findAllByProcessInstanceIdAndDataKey(PROCESS_INSTANCE_ID, "documents"))
                .thenReturn(List.of());

        var result = assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, node.init(context(
                configuration(optionalAttachmentSetConfig("documents", TARGET_STORAGE_PROVIDER_ID, "/case/{{caseId}}/")),
                eventRepository
        )));

        assertEquals(0, result.getNodeData().get("count"));
        assertEquals(List.of(), result.getNodeData().get("storagePathsFromRoot"));
        assertEquals(List.of(), result.getNodeData().get("fileNames"));
        assertEquals(List.of(), result.getNodeData().get("results"));
        assertTrue(storedDocuments.isEmpty());

        var event = captureSingleEvent(eventRepository);
        assertEquals("Anlagensatz 'documents' enthielt keine Dateien. Speichervorgang für diesen Anlagensatz übersprungen.", event.getMessage());
    }

    @Test
    void validateConfiguration_ValidatesTemplateSyntaxInStoragePath() {
        var configuration = configuration(attachmentSetConfig("documents", TARGET_STORAGE_PROVIDER_ID, "/case/{{"));

        var errors = node.validateConfiguration(processNode(), configuration);

        assertNotNull(errors);
        assertTrue(errors.get(StoreAttachmentSetActionNodeV1.AttachmentSetStorageConfig.STORAGE_PATH_FIELD_ID).getFirst().contains("Zeile"));
    }

    @Test
    void cleanConfigurationForExport_RemovesNestedStorageProviderId() {
        var storagePath = new LinkedHashMap<String, Object>();
        storagePath.put("storageProviderId", TARGET_STORAGE_PROVIDER_ID);
        storagePath.put("path", "/case/{{caseId}}/");

        var attachmentSet = new LinkedHashMap<String, Object>();
        attachmentSet.put(StoreAttachmentSetActionNodeV1.AttachmentSetStorageConfig.STORAGE_PATH_FIELD_ID, storagePath);
        attachmentSet.put(StoreAttachmentSetActionNodeV1.AttachmentSetStorageConfig.ATTACHMENT_SET_DATA_KEYS_FIELD_ID, List.of("documents"));

        var configuration = new AuthoredElementValues();
        configuration.put(StoreAttachmentSetActionNodeV1.StoreAttachmentSetActionNodeConfig.ATTACHMENT_SETS_FIELD_ID, List.of(attachmentSet));

        var cleaned = node.cleanConfigurationForExport(configuration);

        @SuppressWarnings("unchecked")
        var cleanedAttachmentSets = (List<Map<String, Object>>) cleaned.get(StoreAttachmentSetActionNodeV1.StoreAttachmentSetActionNodeConfig.ATTACHMENT_SETS_FIELD_ID);
        @SuppressWarnings("unchecked")
        var cleanedStoragePath = (Map<String, Object>) cleanedAttachmentSets.getFirst().get(StoreAttachmentSetActionNodeV1.AttachmentSetStorageConfig.STORAGE_PATH_FIELD_ID);
        assertNull(cleanedStoragePath.get("storageProviderId"));
        assertEquals("/case/{{caseId}}/", cleanedStoragePath.get("path"));
    }

    private void arrangeAttachmentSet(String dataKey,
                                      Integer attachmentSetId,
                                      ProcessInstanceAttachmentEntity... attachments) throws Exception {
        when(processInstanceAttachmentSetService.findAllByProcessInstanceIdAndDataKey(PROCESS_INSTANCE_ID, dataKey))
                .thenReturn(List.of(new ProcessInstanceAttachmentSetEntity().setId(attachmentSetId).setDataKey(dataKey).setName(dataKey)));
        when(processInstanceAttachmentService.findAllByAttachmentSetId(attachmentSetId))
                .thenReturn(List.of(attachments));

        for (var attachment : attachments) {
            when(storageService.getDocumentContent(
                    eq(attachment.getStorageProviderId()),
                    eq(attachment.getStoragePathFromRoot())
            )).thenReturn(new ByteArrayInputStream(attachment.getFileName().getBytes(StandardCharsets.UTF_8)));
        }
    }

    private static ProcessInstanceAttachmentEntity attachment(String fileName,
                                                              Integer position,
                                                              Integer storageProviderId,
                                                              String storagePathFromRoot) {
        return new ProcessInstanceAttachmentEntity()
                .setKey(UUID.randomUUID())
                .setFileName(fileName)
                .setPosition(position)
                .setAttachmentSetId(321)
                .setProcessInstanceId(PROCESS_INSTANCE_ID)
                .setStorageProviderId(storageProviderId)
                .setStoragePathFromRoot(storagePathFromRoot);
    }

    private static StoreAttachmentSetActionNodeV1.StoreAttachmentSetActionNodeConfig configuration(StoreAttachmentSetActionNodeV1.AttachmentSetStorageConfig... attachmentSets) {
        var configuration = new StoreAttachmentSetActionNodeV1.StoreAttachmentSetActionNodeConfig();
        configuration.attachmentSets = List.of(attachmentSets);
        return configuration;
    }

    private static StoreAttachmentSetActionNodeV1.AttachmentSetStorageConfig attachmentSetConfig(String attachmentSetDataKey,
                                                                                                Integer storageProviderId,
                                                                                                String targetPath) {
        return attachmentSetConfig(attachmentSetDataKey, storageProviderId, targetPath, false, null);
    }

    private static StoreAttachmentSetActionNodeV1.AttachmentSetStorageConfig optionalAttachmentSetConfig(String attachmentSetDataKey,
                                                                                                        Integer storageProviderId,
                                                                                                        String targetPath) {
        var config = attachmentSetConfig(attachmentSetDataKey, storageProviderId, targetPath);
        config.ignoreEmptyAttachmentSet = true;
        return config;
    }

    private static StoreAttachmentSetActionNodeV1.AttachmentSetStorageConfig attachmentSetConfig(String attachmentSetDataKey,
                                                                                                Integer storageProviderId,
                                                                                                String targetPath,
                                                                                                boolean customizeFileName,
                                                                                                String fileName) {
        var config = new StoreAttachmentSetActionNodeV1.AttachmentSetStorageConfig();
        config.attachmentSetDataKeys = List.of(attachmentSetDataKey);
        config.storagePath = new StoragePathSelectorInputElementValue()
                .setStorageProviderId(storageProviderId)
                .setPath(targetPath);
        config.customizeFileName = customizeFileName;
        config.fileName = fileName;
        return config;
    }

    private static ProcessNodeExecutionInitContext<StoreAttachmentSetActionNodeV1.StoreAttachmentSetActionNodeConfig> context(
            StoreAttachmentSetActionNodeV1.StoreAttachmentSetActionNodeConfig configuration
    ) {
        return context(configuration, mock(ProcessInstanceHistoryEventRepository.class));
    }

    private static ProcessNodeExecutionInitContext<StoreAttachmentSetActionNodeV1.StoreAttachmentSetActionNodeConfig> context(
            StoreAttachmentSetActionNodeV1.StoreAttachmentSetActionNodeConfig configuration,
            ProcessInstanceHistoryEventRepository eventRepository
    ) {
        return new ProcessNodeExecutionInitContext<>(
                logger(eventRepository),
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

    private static ProcessNodeExecutionLogger logger(ProcessInstanceHistoryEventRepository eventRepository) {
        return new ProcessNodeExecutionLogger(
                PROCESS_INSTANCE_ID,
                TASK_ID,
                null,
                null,
                eventRepository
        );
    }

    private static ProcessInstanceEventEntity captureSingleEvent(ProcessInstanceHistoryEventRepository eventRepository) {
        var eventCaptor = ArgumentCaptor.forClass(ProcessInstanceEventEntity.class);
        verify(eventRepository).save(eventCaptor.capture());
        return eventCaptor.getValue();
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
