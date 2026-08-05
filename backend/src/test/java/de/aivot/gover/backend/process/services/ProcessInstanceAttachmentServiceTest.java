package de.aivot.gover.backend.process.services;

import de.aivot.gover.backend.config.entities.SystemConfigEntity;
import de.aivot.gover.backend.config.repositories.SystemConfigRepository;
import de.aivot.gover.backend.process.configs.DefaultStorageProcessAttachmentsSystemConfigDefinition;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceEventEntity;
import de.aivot.gover.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.gover.backend.process.repositories.ProcessInstanceAttachmentRepository;
import de.aivot.gover.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.gover.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.gover.backend.storage.models.StorageDocument;
import de.aivot.gover.backend.storage.models.StorageFolder;
import de.aivot.gover.backend.storage.models.StorageItemMetadata;
import de.aivot.gover.backend.storage.services.StorageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessInstanceAttachmentServiceTest {
    @Test
    void create_LogsAttachmentCreationEvent() throws Exception {
        var attachmentRepository = mock(ProcessInstanceAttachmentRepository.class);
        var eventRepository = mock(ProcessInstanceHistoryEventRepository.class);
        var storageService = mock(StorageService.class);
        var systemConfigRepository = mock(SystemConfigRepository.class);
        var processInstanceRepository = mock(ProcessInstanceRepository.class);
        var processAccessKey = UUID.fromString("00000000-0000-0000-0000-000000000001");

        when(systemConfigRepository.findById(DefaultStorageProcessAttachmentsSystemConfigDefinition.KEY))
                .thenReturn(Optional.of(new SystemConfigEntity().setValue("5")));
        when(processInstanceRepository.findById(42L))
                .thenReturn(Optional.of(new ProcessInstanceEntity()
                        .setProcessId(7)
                        .setAccessKey(processAccessKey)));
        when(storageService.createFolder(eq(5), anyString()))
                .thenReturn(new StorageFolder("/proc-7/%s/attachments/".formatted(processAccessKey), "attachments", List.of(), List.of(), false));
        when(storageService.storeDocument(eq(5), anyString(), any(byte[].class), any(StorageItemMetadata.class)))
                .thenReturn(new StorageDocument("/proc-7/%s/attachments/file.pdf".formatted(processAccessKey), "file.pdf", 4L, StorageItemMetadata.empty()));
        when(attachmentRepository.save(any(ProcessInstanceAttachmentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any(ProcessInstanceEventEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var service = new ProcessInstanceAttachmentService(
                attachmentRepository,
                storageService,
                systemConfigRepository,
                processInstanceRepository,
                eventRepository
        );

        var attachment = ProcessInstanceAttachmentEntity
                .of("file.pdf", "uploaded-file.pdf", "person-1/dog-2", 1, 42L, 9L, "data".getBytes(StandardCharsets.UTF_8))
                .setAttachmentSetId(3)
                .setUploadedByUserId("00000000-0000-0000-0000-000000000002");

        var savedAttachment = service.create(attachment);

        var eventCaptor = ArgumentCaptor.forClass(ProcessInstanceEventEntity.class);
        verify(eventRepository).save(eventCaptor.capture());
        var event = eventCaptor.getValue();

        assertNotNull(savedAttachment.getKey());
        assertEquals(42L, event.getProcessInstanceId());
        assertEquals(9L, event.getProcessInstanceTaskId());
        assertEquals(ProcessNodeExecutionLogLevel.Info, event.getLevel());
        assertFalse(event.getTechnical());
        assertTrue(event.getAudit());
        assertEquals("Anhang erstellt", event.getTitle());
        assertEquals("00000000-0000-0000-0000-000000000002", event.getTriggeringUserId());
        assertEquals(savedAttachment.getKey(), event.getDetails().get("attachmentKey"));
        assertEquals("file.pdf", event.getDetails().get("fileName"));
        assertEquals("uploaded-file.pdf", event.getDetails().get("originalFileName"));
        assertEquals("person-1/dog-2", event.getDetails().get("group"));
        assertEquals(1, event.getDetails().get("position"));
        assertEquals(3, event.getDetails().get("attachmentSetId"));
    }
}
