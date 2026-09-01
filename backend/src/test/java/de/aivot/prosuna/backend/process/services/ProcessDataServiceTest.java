package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceAttachmentRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceAttachmentSetRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessNodeRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessDataServiceTest {
    @Test
    void foldProcessInstanceData_IncludesAttachmentSetsWithContainedAttachments() {
        var instance = new ProcessInstanceEntity(
                42L,
                "CASE-42",
                UUID.randomUUID().toString(),
                7,
                1,
                ProcessInstanceStatus.Running,
                null,
                null,
                List.of(),
                new IdentityDataMap(),
                Instant.now(),
                Instant.now(),
                null,
                null,
                Map.of("initial", "payload"),
                11,
                null,
                null
        );
        var initialNode = new ProcessNodeEntity(
                11,
                7,
                1,
                "Start",
                null,
                "start",
                "test/start",
                1,
                new AuthoredElementValues(),
                Map.of(),
                null,
                null,
                null,
                false
        );
        var attachmentSet = new ProcessInstanceAttachmentSetEntity(
                1,
                "Dokumente",
                "documents",
                42L,
                null
        );
        var taskAttachmentSet = new ProcessInstanceAttachmentSetEntity(
                2,
                "Weitere Dokumente",
                "documents",
                42L,
                99L
        );
        var firstAttachment = new ProcessInstanceAttachmentEntity(
                UUID.randomUUID(),
                "first.pdf",
                "uploaded-first.pdf",
                "person-1",
                1,
                1,
                42L,
                null,
                5,
                "/attachments/first.pdf",
                null,
                null
        );
        var thirdAttachment = new ProcessInstanceAttachmentEntity(
                UUID.randomUUID(),
                "third.pdf",
                "uploaded-third.pdf",
                "person-2",
                3,
                2,
                42L,
                99L,
                5,
                "/attachments/third.pdf",
                null,
                null
        );
        var secondAttachment = new ProcessInstanceAttachmentEntity(
                UUID.randomUUID(),
                "second.pdf",
                "uploaded-second.pdf",
                null,
                2,
                1,
                42L,
                null,
                5,
                "/attachments/second.pdf",
                null,
                null
        );

        var taskRepository = mock(ProcessInstanceTaskRepository.class);
        when(taskRepository.getLatestTasksByProcessInstanceId(42L)).thenReturn(List.of());

        var nodeRepository = mock(ProcessNodeRepository.class);
        when(nodeRepository.findAllByProcessId(7)).thenReturn(List.of(initialNode));
        when(nodeRepository.findById(11)).thenReturn(Optional.of(initialNode));

        var attachmentRepository = mock(ProcessInstanceAttachmentRepository.class);
        when(attachmentRepository.findAllByProcessInstanceId(42L)).thenReturn(List.of(secondAttachment, thirdAttachment, firstAttachment));

        var attachmentSetRepository = mock(ProcessInstanceAttachmentSetRepository.class);
        when(attachmentSetRepository.findAllByProcessInstanceId(42L)).thenReturn(List.of(attachmentSet, taskAttachmentSet));

        var currentTask = new ProcessInstanceTaskEntity().setId(99L);

        var data = new ProcessDataService(
                taskRepository,
                nodeRepository,
                attachmentRepository,
                attachmentSetRepository
        ).foldProcessInstanceData(instance, null, currentTask);

        var metadata = data.getProcessMetadata();
        assertFalse(metadata.containsKey("attachments"));
        assertEquals(42L, metadata.get("processInstanceId"));
        assertEquals(99L, metadata.get("currentTaskId"));

        @SuppressWarnings("unchecked")
        var attachmentSets = (Map<String, Object>) metadata.get("attachmentSets");
        @SuppressWarnings("unchecked")
        var documentsSet = (Map<String, Object>) attachmentSets.get("documents");
        assertEquals("Dokumente", documentsSet.get("name"));
        assertEquals("documents", documentsSet.get("dataKey"));

        @SuppressWarnings("unchecked")
        var attachments = (List<Map<String, Object>>) documentsSet.get("attachments");
        assertEquals(3, attachments.size());
        assertEquals("first.pdf", attachments.get(0).get("filename"));
        assertEquals("uploaded-first.pdf", attachments.get(0).get("originalFilename"));
        assertEquals("person-1", attachments.get(0).get("group"));
        assertNull(attachments.get(1).get("group"));
        assertEquals("/attachments/second.pdf", attachments.get(1).get("storagePathFromRoot"));
        assertEquals("third.pdf", attachments.get(2).get("filename"));
        assertEquals("uploaded-third.pdf", attachments.get(2).get("originalFilename"));
        assertEquals("person-2", attachments.get(2).get("group"));

        @SuppressWarnings("unchecked")
        var sets = (List<Map<String, Object>>) documentsSet.get("sets");
        assertEquals(2, sets.size());
        assertEquals(99L, sets.get(1).get("processInstanceTaskId"));

        @SuppressWarnings("unchecked")
        var firstSetAttachments = (List<Map<String, Object>>) sets.getFirst().get("attachments");
        assertEquals("first.pdf", firstSetAttachments.get(0).get("filename"));
        assertEquals("uploaded-first.pdf", firstSetAttachments.get(0).get("originalFilename"));
        assertEquals("person-1", firstSetAttachments.get(0).get("group"));
        assertEquals("second.pdf", firstSetAttachments.get(1).get("filename"));
    }
}
