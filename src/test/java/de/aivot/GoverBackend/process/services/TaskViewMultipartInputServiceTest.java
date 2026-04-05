package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.av.services.AVService;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.process.entities.ProcessInstanceAttachmentEntity;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TaskViewMultipartInputServiceTest {
    @Test
    void normalizeInputs_ReplacesTransientFileItemsWithPersistedAttachmentUris() throws Exception {
        var attachmentService = new TestProcessInstanceAttachmentService();
        var service = new TaskViewMultipartInputService(
                attachmentService,
                new TestAVService()
        );

        var inputs = new AuthoredElementValues();
        inputs.put("documents", List.of(
                Map.of(
                        "name", "report.pdf",
                        "uri", "blob:report",
                        "size", 3
                ),
                Map.of(
                        "name", "existing.pdf",
                        "uri", "process-instance-attachment:existing",
                        "size", 7
                )
        ));

        var file = new MockMultipartFile(
                "files",
                "report.pdf",
                "application/pdf",
                "pdf".getBytes(StandardCharsets.UTF_8)
        );

        var normalized = service.normalizeInputs(
                inputs,
                new MultipartFile[]{file},
                List.of("blob:report"),
                42L,
                9L,
                "staff-user"
        );

        @SuppressWarnings("unchecked")
        var documents = (List<Map<String, Object>>) normalized.get("documents");
        assertEquals(2, documents.size());
        assertEquals("report.pdf", documents.get(0).get("name"));
        assertEquals(3, documents.get(0).get("size"));
        assertEquals(
                TaskViewMultipartInputService.buildAttachmentUri(attachmentService.createdAttachments().getFirst().getKey()),
                documents.get(0).get("uri")
        );
        assertEquals("process-instance-attachment:existing", documents.get(1).get("uri"));

        assertEquals(1, attachmentService.createdAttachments().size());
        var createdAttachment = attachmentService.createdAttachments().getFirst();
        assertEquals(42L, createdAttachment.getProcessInstanceId());
        assertEquals(9L, createdAttachment.getProcessInstanceTaskId());
        assertEquals("staff-user", createdAttachment.getUploadedByUserId());
    }

    @Test
    void normalizeInputs_RejectsMissingMultipartDataForTransientFiles() {
        var service = new TaskViewMultipartInputService(
                new TestProcessInstanceAttachmentService(),
                new TestAVService()
        );

        var inputs = new AuthoredElementValues();
        inputs.put("documents", List.of(Map.of(
                "name", "report.pdf",
                "uri", "blob:report",
                "size", 3
        )));

        var exception = assertThrows(ResponseException.class, () -> service.normalizeInputs(
                inputs,
                null,
                null,
                42L,
                9L,
                "staff-user"
        ));

        assertTrue(exception.getMessage().contains("keine Binärdaten"));
    }

    private static final class TestProcessInstanceAttachmentService extends ProcessInstanceAttachmentService {
        private final List<ProcessInstanceAttachmentEntity> createdAttachments = new ArrayList<>();

        private TestProcessInstanceAttachmentService() {
            super(null, null, null, null);
        }

        @Override
        public ProcessInstanceAttachmentEntity create(ProcessInstanceAttachmentEntity entity) {
            var createdAttachment = entity
                    .setKey(UUID.nameUUIDFromBytes(("attachment-" + createdAttachments.size()).getBytes(StandardCharsets.UTF_8)))
                    .setStorageProviderId(1)
                    .setStoragePathFromRoot("/attachments/" + entity.getFileName());
            createdAttachments.add(createdAttachment);
            return createdAttachment;
        }

        private List<ProcessInstanceAttachmentEntity> createdAttachments() {
            return createdAttachments;
        }
    }

    private static final class TestAVService extends AVService {
        private TestAVService() {
            super(null, null);
        }

        @Override
        public void testMultipartFiles(MultipartFile[] files) {
            // Skip AV checks for the focused unit test.
        }
    }
}
