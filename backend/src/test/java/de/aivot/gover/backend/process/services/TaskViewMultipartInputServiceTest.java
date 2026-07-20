package de.aivot.gover.backend.process.services;

import de.aivot.gover.backend.av.services.AVService;
import de.aivot.gover.backend.elements.models.AuthoredElementValues;
import de.aivot.gover.backend.elements.models.elements.form.input.FileUploadInputElement;
import de.aivot.gover.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FileUploadMultipartInputServiceTest {
    @Test
    void normalizeInputs_ReplacesTransientFileItemsWithPersistedAttachmentUris() throws Exception {
        var attachmentService = new TestProcessInstanceAttachmentService();
        var attachmentSetService = new TestProcessInstanceAttachmentSetService();
        var service = new FileUploadMultipartInputService(
                attachmentService,
                attachmentSetService,
                new TestAVService()
        );
        var layout = createLayout("documents", null);

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

        var normalizationResult = service.normalizeInputs(
                layout,
                inputs,
                new MultipartFile[]{file},
                List.of("blob:report"),
                42L,
                9L,
                "staff-user"
        );
        var normalized = normalizationResult.inputs();

        @SuppressWarnings("unchecked")
        var documents = (List<Map<String, Object>>) normalized.get("documents");
        assertEquals(2, documents.size());
        assertEquals("report.pdf", documents.get(0).get("name"));
        assertEquals(3, documents.get(0).get("size"));
        assertEquals(
                FileUploadMultipartInputService.buildAttachmentUri(attachmentService.createdAttachments().getFirst().getKey()),
                documents.get(0).get("uri")
        );
        assertEquals("process-instance-attachment:existing", documents.get(1).get("uri"));

        assertEquals(1, attachmentService.createdAttachments().size());
        var createdAttachment = attachmentService.createdAttachments().getFirst();
        assertEquals(42L, createdAttachment.getProcessInstanceId());
        assertEquals(9L, createdAttachment.getProcessInstanceTaskId());
        assertEquals("staff-user", createdAttachment.getUploadedByUserId());
        assertEquals(1, normalizationResult.createdFileItems().size());
        assertEquals("report.pdf", normalizationResult.createdFileItems().getFirst().getName());
        assertEquals(3, normalizationResult.createdFileItems().getFirst().getSize());
        assertEquals(
                FileUploadMultipartInputService.buildAttachmentUri(createdAttachment.getKey()),
                normalizationResult.createdFileItems().getFirst().getUri()
        );
        assertEquals(1, attachmentSetService.createdSets().size());
        assertEquals(attachmentSetService.createdSets().getFirst().getId(), createdAttachment.getAttachmentSetId());
        assertEquals("documents", attachmentSetService.createdSets().getFirst().getDataKey());
        assertEquals(9L, attachmentSetService.createdSets().getFirst().getProcessInstanceTaskId());
    }

    @Test
    void normalizeInputs_RejectsMissingMultipartDataForTransientFiles() {
        var service = new FileUploadMultipartInputService(
                new TestProcessInstanceAttachmentService(),
                new TestProcessInstanceAttachmentSetService(),
                new TestAVService()
        );
        var layout = createLayout("documents", null);

        var inputs = new AuthoredElementValues();
        inputs.put("documents", List.of(Map.of(
                "name", "report.pdf",
                "uri", "blob:report",
                "size", 3
        )));

        var exception = assertThrows(ResponseException.class, () -> service.normalizeInputs(
                layout,
                inputs,
                null,
                null,
                42L,
                9L,
                "staff-user"
        ));

        assertTrue(exception.getMessage().contains("keine Binärdaten"));
    }

    @Test
    void normalizeInputs_UsesConfiguredSubmittedFileNameAndSuffixesMultipleUploads() throws Exception {
        var attachmentService = new TestProcessInstanceAttachmentService();
        var attachmentSetService = new TestProcessInstanceAttachmentSetService();
        var service = new FileUploadMultipartInputService(
                attachmentService,
                attachmentSetService,
                new TestAVService()
        );
        var layout = createLayout("documents", "evidence");

        var inputs = new AuthoredElementValues();
        inputs.put("documents", List.of(
                Map.of(
                        "name", "report.pdf",
                        "uri", "blob:report",
                        "size", 3
                ),
                Map.of(
                        "name", "invoice.pdf",
                        "uri", "blob:invoice",
                        "size", 7
                )
        ));

        var normalized = service.normalizeInputs(
                layout,
                inputs,
                new MultipartFile[]{
                        new MockMultipartFile("files", "report.pdf", "application/pdf", "pdf".getBytes(StandardCharsets.UTF_8)),
                        new MockMultipartFile("files", "invoice.pdf", "application/pdf", "invoice".getBytes(StandardCharsets.UTF_8))
                },
                List.of("blob:report", "blob:invoice"),
                42L,
                9L,
                "staff-user"
        ).inputs();

        @SuppressWarnings("unchecked")
        var documents = (List<Map<String, Object>>) normalized.get("documents");
        assertEquals("evidence.pdf", documents.get(0).get("name"));
        assertEquals("evidence-2.pdf", documents.get(1).get("name"));
        assertEquals("evidence.pdf", attachmentService.createdAttachments().get(0).getFileName());
        assertEquals("evidence-2.pdf", attachmentService.createdAttachments().get(1).getFileName());
        assertEquals(1, attachmentSetService.createdSets().size());
        assertEquals(attachmentSetService.createdSets().getFirst().getId(), attachmentService.createdAttachments().get(0).getAttachmentSetId());
        assertEquals(attachmentSetService.createdSets().getFirst().getId(), attachmentService.createdAttachments().get(1).getAttachmentSetId());
    }

    @Test
    void normalizeInputs_UsesOriginalExtensionForConfiguredSubmittedFileName() throws Exception {
        var attachmentService = new TestProcessInstanceAttachmentService();
        var attachmentSetService = new TestProcessInstanceAttachmentSetService();
        var service = new FileUploadMultipartInputService(
                attachmentService,
                attachmentSetService,
                new TestAVService()
        );
        var layout = createLayout("documents", "evidence.png");

        var inputs = new AuthoredElementValues();
        inputs.put("documents", List.of(Map.of(
                "name", "report.pdf",
                "uri", "blob:report",
                "size", 3
        )));

        var normalized = service.normalizeInputs(
                layout,
                inputs,
                new MultipartFile[]{
                        new MockMultipartFile("files", "report.pdf", "application/pdf", "pdf".getBytes(StandardCharsets.UTF_8))
                },
                List.of("blob:report"),
                42L,
                9L,
                "staff-user"
        ).inputs();

        @SuppressWarnings("unchecked")
        var documents = (List<Map<String, Object>>) normalized.get("documents");
        assertEquals("evidence.pdf", documents.getFirst().get("name"));
        assertEquals("evidence.pdf", attachmentService.createdAttachments().getFirst().getFileName());
    }

    @Test
    void normalizeInputs_GroupsUploadsByDestinationKeyOrElementId() throws Exception {
        var attachmentService = new TestProcessInstanceAttachmentService();
        var attachmentSetService = new TestProcessInstanceAttachmentSetService();
        var service = new FileUploadMultipartInputService(
                attachmentService,
                attachmentSetService,
                new TestAVService()
        );

        var firstUpload = createUpload("firstDocuments", null);
        firstUpload.setDestinationKey("case.documents");
        firstUpload.setLabel("Case documents");

        var secondUpload = createUpload("secondDocuments", null);
        secondUpload.setDestinationKey("case.documents");

        var fallbackUpload = createUpload("fallback.documents", null);

        var layout = new GroupLayoutElement();
        layout.setId("root");
        layout.setChildren(List.of(firstUpload, secondUpload, fallbackUpload));

        var inputs = new AuthoredElementValues();
        inputs.put("firstDocuments", List.of(createFileItem("first.pdf", "blob:first", 1)));
        inputs.put("secondDocuments", List.of(createFileItem("second.pdf", "blob:second", 1)));
        inputs.put("fallback.documents", List.of(createFileItem("fallback.pdf", "blob:fallback", 1)));

        service.normalizeInputs(
                layout,
                inputs,
                new MultipartFile[]{
                        new MockMultipartFile("files", "first.pdf", "application/pdf", "1".getBytes(StandardCharsets.UTF_8)),
                        new MockMultipartFile("files", "second.pdf", "application/pdf", "2".getBytes(StandardCharsets.UTF_8)),
                        new MockMultipartFile("files", "fallback.pdf", "application/pdf", "3".getBytes(StandardCharsets.UTF_8))
                },
                List.of("blob:first", "blob:second", "blob:fallback"),
                42L,
                null,
                null
        );

        assertEquals(2, attachmentSetService.createdSets().size());
        var destinationSet = attachmentSetService.createdSets().get(0);
        var fallbackSet = attachmentSetService.createdSets().get(1);
        assertEquals("case_documents", destinationSet.getDataKey());
        assertEquals("Case documents", destinationSet.getName());
        assertEquals("fallback_documents", fallbackSet.getDataKey());

        assertEquals(destinationSet.getId(), attachmentService.createdAttachments().get(0).getAttachmentSetId());
        assertEquals(destinationSet.getId(), attachmentService.createdAttachments().get(1).getAttachmentSetId());
        assertEquals(fallbackSet.getId(), attachmentService.createdAttachments().get(2).getAttachmentSetId());
    }

    private static GroupLayoutElement createLayout(String elementId, String submittedFileName) {
        var uploadElement = createUpload(elementId, submittedFileName);

        var layout = new GroupLayoutElement();
        layout.setId("root");
        layout.setChildren(List.of(uploadElement));
        return layout;
    }

    private static FileUploadInputElement createUpload(String elementId, String submittedFileName) {
        var uploadElement = new FileUploadInputElement();
        uploadElement.setId(elementId);
        uploadElement.setSubmittedFileName(submittedFileName);
        uploadElement.setExtensions(List.of("pdf"));
        return uploadElement;
    }

    private static Map<String, Object> createFileItem(String name, String uri, int size) {
        return Map.of(
                "name", name,
                "uri", uri,
                "size", size
        );
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

    private static final class TestProcessInstanceAttachmentSetService extends ProcessInstanceAttachmentSetService {
        private final List<ProcessInstanceAttachmentSetEntity> createdSets = new ArrayList<>();

        private TestProcessInstanceAttachmentSetService() {
            super(null);
        }

        @Override
        public ProcessInstanceAttachmentSetEntity create(ProcessInstanceAttachmentSetEntity entity) {
            var createdSet = entity.setId(createdSets.size() + 1);
            createdSets.add(createdSet);
            return createdSet;
        }

        private List<ProcessInstanceAttachmentSetEntity> createdSets() {
            return createdSets;
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
