package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.config.entities.SystemConfigEntity;
import de.aivot.prosuna.backend.config.repositories.SystemConfigRepository;
import de.aivot.prosuna.backend.process.configs.DefaultStorageProcessAttachmentsSystemConfigDefinition;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEventEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceAttachmentRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.storage.models.StorageDocument;
import de.aivot.prosuna.backend.storage.models.StorageFolder;
import de.aivot.prosuna.backend.storage.models.StorageItemMetadata;
import de.aivot.prosuna.backend.storage.services.StorageService;
import de.aivot.prosuna.backend.utils.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProcessInstanceAttachmentServiceTest {
    private final ProcessInstanceAttachmentRepository attachmentRepository = mock(ProcessInstanceAttachmentRepository.class);
    private final ProcessInstanceHistoryEventRepository eventRepository = mock(ProcessInstanceHistoryEventRepository.class);
    private final StorageService storageService = mock(StorageService.class);
    private final SystemConfigRepository systemConfigRepository = mock(SystemConfigRepository.class);
    private final ProcessInstanceRepository processInstanceRepository = mock(ProcessInstanceRepository.class);
    private final UUID processAccessKey = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final ProcessInstanceAttachmentService service = new ProcessInstanceAttachmentService(
            attachmentRepository, storageService, systemConfigRepository, processInstanceRepository,
            new ProcessNodeExecutionLoggerFactory(eventRepository)
    );

    @Test
    void delete_RemovesStoredDocumentAndItsDatabaseReference() throws Exception {
        var attachmentRepository = mock(ProcessInstanceAttachmentRepository.class);
        var storageService = mock(StorageService.class);
        var service = new ProcessInstanceAttachmentService(attachmentRepository, storageService,
                mock(SystemConfigRepository.class), mock(ProcessInstanceRepository.class));
        var attachment = new ProcessInstanceAttachmentEntity()
                .setStorageProviderId(5)
                .setStoragePathFromRoot("/proc-7/instance/attachments/file.pdf");

        service.deleteEntity(attachment);

        var order = inOrder(attachmentRepository, storageService);
        order.verify(attachmentRepository).delete(attachment);
        order.verify(attachmentRepository).flush();
        order.verify(storageService).deleteDocument(5, "/proc-7/instance/attachments/file.pdf");
    }

    @BeforeEach
    void setUp() throws Exception {
        when(systemConfigRepository.findById(DefaultStorageProcessAttachmentsSystemConfigDefinition.KEY))
                .thenReturn(Optional.of(new SystemConfigEntity().setValue("5")));
        when(processInstanceRepository.findById(42L))
                .thenReturn(Optional.of(new ProcessInstanceEntity()
                        .setProcessId(7)
                        .setAccessKey(processAccessKey.toString())));
        when(storageService.createFolder(eq(5), anyString()))
                .thenReturn(new StorageFolder("/proc-7/%s/attachments/".formatted(processAccessKey), "attachments", List.of(), List.of(), false));
        when(storageService.storeDocument(eq(5), anyString(), any(byte[].class), any(StorageItemMetadata.class)))
                .thenReturn(new StorageDocument("/proc-7/%s/attachments/file.pdf".formatted(processAccessKey), "file.pdf", 4L, StorageItemMetadata.empty()));
        when(attachmentRepository.save(any(ProcessInstanceAttachmentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @ParameterizedTest
    @CsvSource({"true, true", "true, false", "false, true", "false, false"})
    void create_LogsAttachmentCreationEvent(boolean hasUser, boolean hasTask) throws Exception {
        Long taskId = hasTask ? 9L : null;
        String userId = hasUser ? "00000000-0000-0000-0000-000000000002" : null;
        var attachment = attachment()
                .setProcessInstanceTaskId(taskId)
                .setUploadedByUserId(userId);

        var before = Instant.now();
        var savedAttachment = service.create(attachment);
        var after = Instant.now();

        var eventCaptor = ArgumentCaptor.forClass(ProcessInstanceEventEntity.class);
        var order = inOrder(attachmentRepository, eventRepository);
        order.verify(attachmentRepository).save(attachment);
        order.verify(eventRepository).save(eventCaptor.capture());
        var event = eventCaptor.getValue();

        assertNotNull(savedAttachment.getKey());
        assertNull(event.getId());
        assertEquals(42L, event.getProcessInstanceId());
        assertEquals(taskId, event.getProcessInstanceTaskId());
        assertEquals(ProcessNodeExecutionLogLevel.Info, event.getLevel());
        assertEquals(!hasUser, event.getTechnical());
        assertTrue(event.getAudit());
        assertFalse(event.getHistoryRelevant());
        assertEquals("Anhang erstellt", event.getTitle());
        assertEquals("Der Anhang %s wurde erstellt.".formatted(StringUtils.quote("file.pdf")), event.getMessage());
        assertEquals(userId, event.getTriggeringUserId());
        assertNull(event.getConcernedUserId());
        assertNull(event.getConcernedIdentityId());
        assertNull(event.getConcernedIdentityTitle());
        assertFalse(event.getTimestamp().isBefore(before));
        assertFalse(event.getTimestamp().isAfter(after));
        var expectedDetails = new LinkedHashMap<String, Object>();
        expectedDetails.put("attachmentKey", savedAttachment.getKey());
        expectedDetails.put("fileName", "file.pdf");
        expectedDetails.put("originalFileName", "uploaded-file.pdf");
        expectedDetails.put("group", "person-1/dog-2");
        expectedDetails.put("position", 1);
        expectedDetails.put("attachmentSetId", 3);
        expectedDetails.put("processInstanceId", 42L);
        expectedDetails.put("processInstanceTaskId", taskId);
        expectedDetails.put("storageProviderId", 5);
        expectedDetails.put("storagePathFromRoot", "/proc-7/%s/attachments/file.pdf".formatted(processAccessKey));
        expectedDetails.put("uploadedByUserId", userId);
        assertEquals(expectedDetails, event.getDetails());
    }

    @Test
    void create_ReturnsSavedAttachmentWhenEventPersistenceFails() throws Exception {
        when(eventRepository.save(any(ProcessInstanceEventEntity.class)))
                .thenThrow(new IllegalStateException("Event persistence failed"));
        var attachment = attachment();

        assertSame(attachment, service.create(attachment));

        verify(attachmentRepository).save(attachment);
        verify(eventRepository).save(any(ProcessInstanceEventEntity.class));
    }

    @Test
    void create_DoesNotLogSuccessWhenAttachmentPersistenceFails() {
        when(attachmentRepository.save(any(ProcessInstanceAttachmentEntity.class)))
                .thenThrow(new IllegalStateException("Attachment persistence failed"));

        assertThrows(IllegalStateException.class, () -> service.create(attachment()));

        verifyNoInteractions(eventRepository);
    }

    @Test
    void create_DoesNotLogSuccessWhenDocumentStorageFails() throws Exception {
        when(storageService.storeDocument(eq(5), anyString(), any(byte[].class), any(StorageItemMetadata.class)))
                .thenThrow(new IllegalStateException("Document storage failed"));

        assertThrows(IllegalStateException.class, () -> service.create(attachment()));

        verifyNoInteractions(attachmentRepository, eventRepository);
    }

    @Test
    void create_SupportsProtectedConstructorWithoutLogging() throws Exception {
        var serviceWithoutLogging = new ProcessInstanceAttachmentService(
                attachmentRepository, storageService, systemConfigRepository, processInstanceRepository
        );
        var attachment = attachment();

        assertSame(attachment, serviceWithoutLogging.create(attachment));

        verifyNoInteractions(eventRepository);
    }

    private ProcessInstanceAttachmentEntity attachment() {
        return ProcessInstanceAttachmentEntity
                .of("file.pdf", "uploaded-file.pdf", "person-1/dog-2", 1, 42L, 9L, "data".getBytes(StandardCharsets.UTF_8))
                .setAttachmentSetId(3);
    }
}
