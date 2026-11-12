package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.config.entities.SystemConfigEntity;
import de.aivot.prosuna.backend.config.repositories.SystemConfigRepository;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.configs.DefaultStorageProcessAttachmentsSystemConfigDefinition;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEventEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.models.ProcessAttachmentSizeProjection;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceAttachmentRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceAttachmentSetRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProcessInstanceAttachmentServiceTest {
    private final ProcessInstanceAttachmentRepository attachmentRepository = mock(ProcessInstanceAttachmentRepository.class);
    private final ProcessInstanceHistoryEventRepository eventRepository = mock(ProcessInstanceHistoryEventRepository.class);
    private final StorageService storageService = mock(StorageService.class);
    private final SystemConfigRepository systemConfigRepository = mock(SystemConfigRepository.class);
    private final ProcessInstanceRepository processInstanceRepository = mock(ProcessInstanceRepository.class);
    private final ProcessInstanceAttachmentSetRepository attachmentSetRepository = mock(ProcessInstanceAttachmentSetRepository.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final UUID processAccessKey = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final ProcessInstanceAttachmentService service = new ProcessInstanceAttachmentService(
            attachmentRepository, storageService, systemConfigRepository, processInstanceRepository,
            new ProcessNodeExecutionLoggerFactory(eventRepository), attachmentSetRepository, transactionManager
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
                        .setId(42L)
                        .setProcessId(7)
                        .setAccessKey(processAccessKey.toString())));
        when(storageService.createFolder(eq(5), anyString()))
                .thenReturn(new StorageFolder("/proc-7/42/attachments/", "attachments", List.of(), List.of(), false));
        when(storageService.storeDocument(eq(5), anyString(), any(byte[].class), any(StorageItemMetadata.class)))
                .thenReturn(new StorageDocument("/proc-7/42/attachments/file.pdf", "file.pdf", 4L, StorageItemMetadata.empty()));
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

        // The access key grants customers access to the instance and must not appear in storage paths.
        verify(storageService).createFolder(5, "/proc-7/42/attachments");
        var storedPathCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageService).storeDocument(eq(5), storedPathCaptor.capture(), any(byte[].class), any(StorageItemMetadata.class));
        assertFalse(storedPathCaptor.getValue().contains(processAccessKey.toString()));
        assertEquals("/proc-7/42/attachments/file.pdf", savedAttachment.getStoragePathFromRoot());
        assertEquals(4L, savedAttachment.getSizeInBytes());

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
        expectedDetails.put("storagePathFromRoot", "/proc-7/42/attachments/file.pdf");
        expectedDetails.put("uploadedByUserId", userId);
        assertEquals(expectedDetails, event.getDetails());
    }

    @Test
    void create_PopulatesSizeOnTheEntityReturnedByPersistence() throws Exception {
        var savedAttachment = attachment().setKey(UUID.randomUUID());
        when(attachmentRepository.save(any(ProcessInstanceAttachmentEntity.class))).thenReturn(savedAttachment);

        var result = service.create(attachment());

        assertSame(savedAttachment, result);
        assertEquals(4L, result.getSizeInBytes());
        var json = de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils.createMapper().valueToTree(result);
        assertEquals(4L, json.get("sizeInBytes").asLong());
        assertFalse(json.has("fileBytes"));
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

    @Test
    void list_PopulatesSizesInOneBatchAndPreservesUnknownSizes() throws Exception {
        var empty = attachment().setKey(UUID.randomUUID());
        var large = attachment().setKey(UUID.randomUUID());
        var missing = attachment().setKey(UUID.randomUUID());
        var attachments = List.of(empty, large, missing);
        when(attachmentRepository.findAll((Specification<ProcessInstanceAttachmentEntity>) null, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(attachments));
        when(attachmentRepository.findSizesByKeys(List.of(empty.getKey(), large.getKey(), missing.getKey())))
                .thenReturn(List.of(size(empty.getKey(), 0L), size(large.getKey(), 5_000_000_000L)));

        var page = service.performList(Pageable.unpaged(), null, null);

        assertEquals(attachments, page.getContent());
        assertEquals(0L, empty.getSizeInBytes());
        assertEquals(5_000_000_000L, large.getSizeInBytes());
        assertNull(missing.getSizeInBytes());
        verify(attachmentRepository).findSizesByKeys(List.of(empty.getKey(), large.getKey(), missing.getKey()));
        verifyNoInteractions(storageService);
    }

    @Test
    void retrieve_PopulatesExistingAttachmentSize() throws Exception {
        var attachment = attachment().setKey(UUID.randomUUID());
        when(attachmentRepository.findById(attachment.getKey())).thenReturn(Optional.of(attachment));
        when(attachmentRepository.findSizesByKeys(List.of(attachment.getKey())))
                .thenReturn(List.of(size(attachment.getKey(), 1234L)));

        assertEquals(1234L, service.retrieve(attachment.getKey()).orElseThrow().getSizeInBytes());
        verifyNoInteractions(storageService);
    }

    @Test
    void list_DoesNotLookUpSizesForEmptyPages() throws Exception {
        when(attachmentRepository.findAll((Specification<ProcessInstanceAttachmentEntity>) null, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of()));

        assertTrue(service.performList(Pageable.unpaged(), null, null).isEmpty());

        verify(attachmentRepository, org.mockito.Mockito.never()).findSizesByKeys(any());
    }

    private ProcessAttachmentSizeProjection size(UUID key, Long bytes) {
        return new ProcessAttachmentSizeProjection() {
            @Override
            public UUID getKey() {
                return key;
            }

            @Override
            public Long getSizeInBytes() {
                return bytes;
            }
        };
    }

    @Test
    void updateRemovedUploadsOfTask_FlagsUnreferencedUploadsAndRestoresReferencedOnes() {
        var removedAt = Instant.parse("2026-10-28T08:00:00Z");
        var unreferenced = uploadedAttachment(1, 1);
        var referencedAgain = uploadedAttachment(2, 2).setRemovedAt(removedAt);
        var unchanged = uploadedAttachment(3, 3);
        var alreadyFlagged = uploadedAttachment(4, 4).setRemovedAt(removedAt);
        when(attachmentRepository.findAllByProcessInstanceTaskIdAndAttachmentSetIdIn(9L, List.of(3)))
                .thenReturn(List.of(unreferenced, referencedAgain, unchanged, alreadyFlagged));

        var before = Instant.now();
        service.updateRemovedUploadsOfTask(
                9L,
                List.of(3),
                Set.of(referencedAgain.getKey(), unchanged.getKey()),
                Map.of(referencedAgain.getKey(), 1, unchanged.getKey(), 3)
        );

        verify(attachmentRepository).save(unreferenced);
        assertNotNull(unreferenced.getRemovedAt());
        assertFalse(unreferenced.getRemovedAt().isBefore(before));
        verify(attachmentRepository).save(referencedAgain);
        assertNull(referencedAgain.getRemovedAt());
        assertEquals(1, referencedAgain.getPosition());
        verify(attachmentRepository, never()).save(unchanged);
        verify(attachmentRepository, never()).save(alreadyFlagged);
        assertEquals(removedAt, alreadyFlagged.getRemovedAt());
    }

    @Test
    void findAllUploadsOfTask_LeavesOutFlaggedUploads() {
        var kept = uploadedAttachment(1, 1);
        var flagged = uploadedAttachment(2, 2).setRemovedAt(Instant.parse("2026-10-28T08:00:00Z"));
        when(attachmentRepository.findAllByProcessInstanceTaskIdAndAttachmentSetIdIn(9L, List.of(3)))
                .thenReturn(List.of(kept, flagged));

        assertEquals(List.of(kept), service.findAllUploadsOfTask(9L, List.of(3)));
        assertEquals(List.of(), service.findAllUploadsOfTask(9L, List.of()));
    }

    @Test
    void updateRemovedUploadsOfTask_SkipsTasksWithoutUploadSets() {
        service.updateRemovedUploadsOfTask(9L, List.of(), Set.of(), Map.of());

        verifyNoInteractions(attachmentRepository);
    }

    @Test
    void updateRemovedUploadsOfTask_NeverRestoresUploadsDueForDeletion() {
        var due = uploadedAttachment(1, 1)
                .setRemovedAt(Instant.parse("2026-10-28T08:00:00Z"))
                .setDeletionDueAt(Instant.parse("2026-10-28T09:00:00Z"));
        when(attachmentRepository.findAllByProcessInstanceTaskIdAndAttachmentSetIdIn(9L, List.of(3)))
                .thenReturn(List.of(due));

        service.updateRemovedUploadsOfTask(9L, List.of(3), Set.of(due.getKey()), Map.of(due.getKey(), 1));

        verify(attachmentRepository, never()).save(any());
        assertNotNull(due.getRemovedAt());
    }

    @Test
    void deleteRemovedUploadsOfTask_MarksUploadsDueAndDeletesThemOnlyAfterCommit() throws Exception {
        var removed = uploadedAttachment(1, 1).setRemovedAt(Instant.parse("2026-10-28T08:00:00Z"));
        mockRemovedUploads(removed);
        when(attachmentRepository.existsByAttachmentSetId(3)).thenReturn(false);

        var synchronizations = deleteRemovedUploadsInTransaction("staff-user");

        verify(attachmentRepository).save(removed);
        assertNotNull(removed.getDeletionDueAt());
        verify(attachmentRepository, never()).delete(any(ProcessInstanceAttachmentEntity.class));
        verifyNoInteractions(storageService, attachmentSetRepository);

        synchronizations.forEach(TransactionSynchronization::afterCommit);

        var order = inOrder(attachmentRepository, storageService, attachmentSetRepository, transactionManager);
        order.verify(attachmentRepository).delete(removed);
        order.verify(attachmentRepository).flush();
        order.verify(storageService).deleteDocument(5, "/proc-7/42/attachments/uploaded-1.pdf");
        order.verify(attachmentSetRepository).deleteById(3);
        order.verify(transactionManager).commit(any());
        verify(transactionManager).getTransaction(argThat(definition ->
                definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
    }

    @Test
    void deleteRemovedUploadsOfTask_KeepsFilesWhenTheTransactionRollsBack() {
        var removed = uploadedAttachment(1, 1).setRemovedAt(Instant.parse("2026-10-28T08:00:00Z"));
        mockRemovedUploads(removed);

        var synchronizations = deleteRemovedUploadsInTransaction("staff-user");
        synchronizations.forEach(synchronization ->
                synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verifyNoInteractions(storageService);
        verify(attachmentRepository, never()).delete(any(ProcessInstanceAttachmentEntity.class));
    }

    @Test
    void deleteRemovedUploadsOfTask_KeepsAttachmentSetsWithRemainingAttachments() {
        var removed = uploadedAttachment(1, 1).setRemovedAt(Instant.parse("2026-10-28T08:00:00Z"));
        mockRemovedUploads(removed);
        when(attachmentRepository.existsByAttachmentSetId(3)).thenReturn(true);

        deleteRemovedUploadsInTransaction(null).forEach(TransactionSynchronization::afterCommit);

        verify(attachmentRepository).delete(removed);
        verify(attachmentSetRepository, never()).deleteById(any());
    }

    @Test
    void deleteRemovedUploadsOfTask_KeepsTheAttachmentDueWhenStorageFailsAndTheRetryDeletesIt() throws Exception {
        var removed = uploadedAttachment(1, 1).setRemovedAt(Instant.parse("2026-10-28T08:00:00Z"));
        mockRemovedUploads(removed);
        when(attachmentRepository.existsByAttachmentSetId(3)).thenReturn(false);
        doThrow(ResponseException.internalServerError("Storage unavailable"))
                .doNothing()
                .when(storageService).deleteDocument(5, "/proc-7/42/attachments/uploaded-1.pdf");

        deleteRemovedUploadsInTransaction("staff-user").forEach(TransactionSynchronization::afterCommit);

        // The deletion of the row rolls back with the failed file deletion, so the attachment stays due.
        verify(transactionManager).rollback(any());
        verify(transactionManager, never()).commit(any());
        verify(attachmentSetRepository, never()).deleteById(any());
        assertNotNull(removed.getDeletionDueAt());

        // The retry finds the attachment in the database, without the callback of the ended task.
        when(attachmentRepository.findTop100ByDeletionDueAtLessThanOrderByDeletionDueAtAsc(any()))
                .thenReturn(List.of(removed));
        service.retryOverdueAttachmentDeletions();

        verify(storageService, times(2)).deleteDocument(5, "/proc-7/42/attachments/uploaded-1.pdf");
        verify(transactionManager).commit(any());
        verify(attachmentSetRepository).deleteById(3);
    }

    @Test
    void retryOverdueAttachmentDeletions_OnlyRetriesDeletionsDueBeforeTheRetryDelay() {
        when(attachmentRepository.findTop100ByDeletionDueAtLessThanOrderByDeletionDueAtAsc(any())).thenReturn(List.of());

        var before = Instant.now();
        service.retryOverdueAttachmentDeletions();
        var after = Instant.now();

        verify(attachmentRepository).findTop100ByDeletionDueAtLessThanOrderByDeletionDueAtAsc(argThat(dueBefore ->
                !dueBefore.isBefore(before.minusSeconds(300)) && !dueBefore.isAfter(after.minusSeconds(300))));
        verifyNoInteractions(storageService, transactionManager);
    }

    @Test
    void retryOverdueAttachmentDeletions_SkipsAttachmentsThatAreNoLongerDue() {
        var deleted = uploadedAttachment(1, 1).setDeletionDueAt(Instant.parse("2026-10-28T08:00:00Z"));
        when(attachmentRepository.findTop100ByDeletionDueAtLessThanOrderByDeletionDueAtAsc(any())).thenReturn(List.of(deleted));
        when(attachmentRepository.findByKeyAndDeletionDueAtIsNotNull(deleted.getKey())).thenReturn(Optional.empty());

        service.retryOverdueAttachmentDeletions();

        verify(attachmentRepository, never()).delete(any(ProcessInstanceAttachmentEntity.class));
        verifyNoInteractions(storageService, attachmentSetRepository);
    }

    @Test
    void deleteRemovedUploadsOfTask_LogsDeletionEvent() {
        var removed = uploadedAttachment(1, 1).setRemovedAt(Instant.parse("2026-10-28T08:00:00Z"));
        mockRemovedUploads(removed);

        deleteRemovedUploadsInTransaction("staff-user");

        var eventCaptor = ArgumentCaptor.forClass(ProcessInstanceEventEntity.class);
        verify(eventRepository).save(eventCaptor.capture());
        var event = eventCaptor.getValue();
        assertEquals(42L, event.getProcessInstanceId());
        assertEquals(9L, event.getProcessInstanceTaskId());
        assertEquals("Anhang gelöscht", event.getTitle());
        assertEquals(
                "Der Anhang %s wurde gelöscht, weil er vor dem Ende der Aufgabe aus dem Anlagen-Feld entfernt wurde."
                        .formatted(StringUtils.quote("uploaded-1.pdf")),
                event.getMessage()
        );
        assertEquals("staff-user", event.getTriggeringUserId());
        assertFalse(event.getTechnical());
        assertTrue(event.getAudit());
        assertTrue(event.getHistoryRelevant());
    }

    @Test
    void deleteRemovedUploadsOfTask_DoesNothingWithoutRemovedUploads() {
        when(attachmentRepository.findAllByProcessInstanceTaskIdAndRemovedAtIsNotNullAndDeletionDueAtIsNull(9L)).thenReturn(List.of());

        service.deleteRemovedUploadsOfTask(new ProcessInstanceTaskEntity().setId(9L), "staff-user");

        verify(attachmentRepository, never()).save(any());
        verifyNoInteractions(attachmentSetRepository, storageService, eventRepository, transactionManager);
    }

    private void mockRemovedUploads(ProcessInstanceAttachmentEntity removed) {
        when(attachmentRepository.findAllByProcessInstanceTaskIdAndRemovedAtIsNotNullAndDeletionDueAtIsNull(9L))
                .thenReturn(List.of(removed));
        when(attachmentRepository.findByKeyAndDeletionDueAtIsNotNull(removed.getKey())).thenReturn(Optional.of(removed));
    }

    private List<TransactionSynchronization> deleteRemovedUploadsInTransaction(String userId) {
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.deleteRemovedUploadsOfTask(new ProcessInstanceTaskEntity().setId(9L).setProcessInstanceId(42L), userId);
            return TransactionSynchronizationManager.getSynchronizations();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static ProcessInstanceAttachmentEntity uploadedAttachment(int index, int position) {
        return ProcessInstanceAttachmentEntity
                .of("uploaded-%d.pdf".formatted(index), position, 42L, 9L, new byte[0])
                .setKey(UUID.nameUUIDFromBytes(("uploaded-" + index).getBytes(StandardCharsets.UTF_8)))
                .setAttachmentSetId(3)
                .setStorageProviderId(5)
                .setStoragePathFromRoot("/proc-7/42/attachments/uploaded-%d.pdf".formatted(index));
    }
}
