package de.aivot.gover.backend.process.services;

import de.aivot.gover.backend.elements.models.AuthoredElementValues;
import de.aivot.gover.backend.identity.models.IdentityDataMap;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.enums.ProcessInstanceStatus;
import de.aivot.gover.backend.process.repositories.ProcessInstanceAttachmentRepository;
import de.aivot.gover.backend.process.repositories.ProcessInstanceAttachmentSetRepository;
import de.aivot.gover.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.gover.backend.process.repositories.ProcessNodeRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessDataServiceTest {
    @Test
    void foldProcessInstanceData_IncludesAttachmentSetsWithContainedAttachments() {
        var instance = new ProcessInstanceEntity(
                42L,
                "CASE-42",
                UUID.randomUUID(),
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
        when(attachmentRepository.findAllByProcessInstanceId(42L)).thenReturn(List.of(firstAttachment, secondAttachment, thirdAttachment));

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
        assertEquals("/attachments/second.pdf", attachments.get(1).get("storagePathFromRoot"));

        @SuppressWarnings("unchecked")
        var sets = (List<Map<String, Object>>) documentsSet.get("sets");
        assertEquals(2, sets.size());
        assertEquals(99L, sets.get(1).get("processInstanceTaskId"));
    }
}
