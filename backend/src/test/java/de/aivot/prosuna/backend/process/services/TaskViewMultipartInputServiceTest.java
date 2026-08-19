package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.av.services.AVService;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.form.input.FileUploadInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElementValue;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentSetEntity;
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
        var layout = createLayout("documents", "report");

        var inputs = new AuthoredElementValues();
        inputs.put("documents", List.of(
                Map.of(
                        "name", "report.pdf",
                        "uri", "blob:report",
                        "size", 3
                ),
                Map.of(
                        "name", "existing.pdf",
                        "originalFileName", "original-existing.pdf",
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
        assertEquals("report.pdf", documents.get(0).get("originalFileName"));
        assertEquals(3, documents.get(0).get("size"));
        assertEquals(
                FileUploadMultipartInputService.buildAttachmentUri(attachmentService.createdAttachments().getFirst().getKey()),
                documents.get(0).get("uri")
        );
        assertEquals("original-existing.pdf", documents.get(1).get("originalFileName"));
        assertEquals("process-instance-attachment:existing", documents.get(1).get("uri"));

        assertEquals(1, attachmentService.createdAttachments().size());
        var createdAttachment = attachmentService.createdAttachments().getFirst();
        assertEquals(42L, createdAttachment.getProcessInstanceId());
        assertEquals(9L, createdAttachment.getProcessInstanceTaskId());
        assertEquals("staff-user", createdAttachment.getUploadedByUserId());
        assertEquals("report.pdf", createdAttachment.getOriginalFileName());
        assertNull(createdAttachment.getGroup());
        assertEquals(1, normalizationResult.createdFileItems().size());
        assertEquals("report.pdf", normalizationResult.createdFileItems().getFirst().getName());
        assertEquals("report.pdf", normalizationResult.createdFileItems().getFirst().getOriginalFileName());
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
        var layout = createLayout("documents", "report");

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
    void normalizeInputs_UsesConfiguredSubmittedFileNameAndIndexesAllMultipleUploads() throws Exception {
        var attachmentService = new TestProcessInstanceAttachmentService();
        var attachmentSetService = new TestProcessInstanceAttachmentSetService();
        var service = new FileUploadMultipartInputService(
                attachmentService,
                attachmentSetService,
                new TestAVService()
        );
        var layout = createLayout("documents", "evidence");
        var uploadElement = (FileUploadInputElement) layout.getChildren().getFirst();
        uploadElement.setIsMultifile(true);

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
        assertEquals("evidence-1.pdf", documents.get(0).get("name"));
        assertEquals("evidence-2.pdf", documents.get(1).get("name"));
        assertEquals("report.pdf", documents.get(0).get("originalFileName"));
        assertEquals("invoice.pdf", documents.get(1).get("originalFileName"));
        assertEquals("evidence-1.pdf", attachmentService.createdAttachments().get(0).getFileName());
        assertEquals("evidence-2.pdf", attachmentService.createdAttachments().get(1).getFileName());
        assertEquals("report.pdf", attachmentService.createdAttachments().get(0).getOriginalFileName());
        assertEquals("invoice.pdf", attachmentService.createdAttachments().get(1).getOriginalFileName());
        assertNull(attachmentService.createdAttachments().get(0).getGroup());
        assertNull(attachmentService.createdAttachments().get(1).getGroup());
        assertEquals(1, attachmentService.createdAttachments().get(0).getPosition());
        assertEquals(2, attachmentService.createdAttachments().get(1).getPosition());
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
    void normalizeInputs_TreatsHashAsLiteralForSingleUploadOutsideReplicatingContainer() throws Exception {
        var attachmentService = new TestProcessInstanceAttachmentService();
        var attachmentSetService = new TestProcessInstanceAttachmentSetService();
        var service = new FileUploadMultipartInputService(
                attachmentService,
                attachmentSetService,
                new TestAVService()
        );
        var layout = createLayout("documents", "# evidence");

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
        assertEquals("# evidence.pdf", documents.getFirst().get("name"));
        assertEquals("# evidence.pdf", attachmentService.createdAttachments().getFirst().getFileName());
    }

    @Test
    void normalizeInputs_ReplacesHashWithIndexForMultiUpload() throws Exception {
        var attachmentService = new TestProcessInstanceAttachmentService();
        var attachmentSetService = new TestProcessInstanceAttachmentSetService();
        var service = new FileUploadMultipartInputService(
                attachmentService,
                attachmentSetService,
                new TestAVService()
        );
        var layout = createLayout("documents", "# evidence");
        var uploadElement = (FileUploadInputElement) layout.getChildren().getFirst();
        uploadElement.setIsMultifile(true);

        var inputs = new AuthoredElementValues();
        inputs.put("documents", List.of(
                createFileItem("report.pdf", "blob:report", 3),
                createFileItem("invoice.pdf", "blob:invoice", 7)
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
        assertEquals("1 evidence.pdf", documents.get(0).get("name"));
        assertEquals("2 evidence.pdf", documents.get(1).get("name"));
        assertEquals("1 evidence.pdf", attachmentService.createdAttachments().get(0).getFileName());
        assertEquals("2 evidence.pdf", attachmentService.createdAttachments().get(1).getFileName());
    }

    @Test
    void normalizeInputs_UsesReplicatingContainerRowIdsAsGroupForSingleUploads() throws Exception {
        var attachmentService = new TestProcessInstanceAttachmentService();
        var attachmentSetService = new TestProcessInstanceAttachmentSetService();
        var service = new FileUploadMultipartInputService(
                attachmentService,
                attachmentSetService,
                new TestAVService()
        );
        var upload = createUpload("birthCertificate", "Geburtsurkunde");
        var dogs = new ReplicatingContainerLayoutElement();
        dogs.setId("dogs");
        dogs.setChildren(List.of(upload));
        var persons = new ReplicatingContainerLayoutElement();
        persons.setId("persons");
        persons.setChildren(List.of(dogs));
        var layout = new GroupLayoutElement();
        layout.setId("root");
        layout.setChildren(List.of(persons));

        var inputs = new AuthoredElementValues();
        inputs.put("persons", List.of(
                createReplicatingRow("person-1", Map.of("dogs", List.of(createReplicatingRow("dog-1", Map.of()), createReplicatingRow("dog-2", Map.of())))),
                createReplicatingRow("person-2", Map.of("dogs", List.of(createReplicatingRow("dog-1", Map.of())))),
                createReplicatingRow("person-3", Map.of("dogs", List.of(
                        createReplicatingRow("dog-1", Map.of()),
                        createReplicatingRow("dog-2", Map.of("birthCertificate", List.of(createFileItem("birth.pdf", "blob:birth", 1))))
                )))
        ));

        var normalized = service.normalizeInputs(
                layout,
                inputs,
                new MultipartFile[]{
                        new MockMultipartFile("files", "birth.pdf", "application/pdf", "1".getBytes(StandardCharsets.UTF_8))
                },
                List.of("blob:birth"),
                42L,
                null,
                null
        ).inputs();

        @SuppressWarnings("unchecked")
        var personsValue = (List<?>) normalized.get("persons");
        @SuppressWarnings("unchecked")
        var dogsValue = (List<?>) getReplicatingRowValues(personsValue.get(2)).get("dogs");
        @SuppressWarnings("unchecked")
        var files = (List<Map<String, Object>>) getReplicatingRowValues(dogsValue.get(1)).get("birthCertificate");
        assertEquals("Geburtsurkunde.pdf", files.getFirst().get("name"));
        assertEquals("Geburtsurkunde.pdf", attachmentService.createdAttachments().getFirst().getFileName());
        assertEquals("person-3/dog-2", attachmentService.createdAttachments().getFirst().getGroup());
    }

    @Test
    void normalizeInputs_UsesReplicatingContainerRowIdsAsGroupAndOnlyFileIndicesForMultiUploads() throws Exception {
        var attachmentService = new TestProcessInstanceAttachmentService();
        var attachmentSetService = new TestProcessInstanceAttachmentSetService();
        var service = new FileUploadMultipartInputService(
                attachmentService,
                attachmentSetService,
                new TestAVService()
        );
        var upload = createUpload("xray", "# Röntgenbild");
        upload.setIsMultifile(true);
        var dogs = new ReplicatingContainerLayoutElement();
        dogs.setId("dogs");
        dogs.setChildren(List.of(upload));
        var persons = new ReplicatingContainerLayoutElement();
        persons.setId("persons");
        persons.setChildren(List.of(dogs));
        var layout = new GroupLayoutElement();
        layout.setId("root");
        layout.setChildren(List.of(persons));

        var inputs = new AuthoredElementValues();
        inputs.put("persons", List.of(
                createReplicatingRow("person-1", Map.of("dogs", List.of(
                        createReplicatingRow("dog-1", Map.of()),
                        createReplicatingRow("dog-2", Map.of()),
                        createReplicatingRow("dog-3", Map.of("xray", List.of(
                                createFileItem("first.pdf", "blob:first", 1),
                                createFileItem("second.pdf", "blob:second", 1)
                        )))
                )))
        ));

        var normalized = service.normalizeInputs(
                layout,
                inputs,
                new MultipartFile[]{
                        new MockMultipartFile("files", "first.pdf", "application/pdf", "1".getBytes(StandardCharsets.UTF_8)),
                        new MockMultipartFile("files", "second.pdf", "application/pdf", "2".getBytes(StandardCharsets.UTF_8))
                },
                List.of("blob:first", "blob:second"),
                42L,
                null,
                null
        ).inputs();

        @SuppressWarnings("unchecked")
        var personsValue = (List<?>) normalized.get("persons");
        @SuppressWarnings("unchecked")
        var dogsValue = (List<?>) getReplicatingRowValues(personsValue.getFirst()).get("dogs");
        @SuppressWarnings("unchecked")
        var files = (List<Map<String, Object>>) getReplicatingRowValues(dogsValue.get(2)).get("xray");
        assertEquals("1 Röntgenbild.pdf", files.get(0).get("name"));
        assertEquals("2 Röntgenbild.pdf", files.get(1).get("name"));
        assertEquals("1 Röntgenbild.pdf", attachmentService.createdAttachments().get(0).getFileName());
        assertEquals("2 Röntgenbild.pdf", attachmentService.createdAttachments().get(1).getFileName());
        assertEquals("person-1/dog-3", attachmentService.createdAttachments().get(0).getGroup());
        assertEquals("person-1/dog-3", attachmentService.createdAttachments().get(1).getGroup());
    }

    @Test
    void normalizeInputs_RejectsUploadInsideReplicatingContainerWithoutRowId() {
        var service = new FileUploadMultipartInputService(
                new TestProcessInstanceAttachmentService(),
                new TestProcessInstanceAttachmentSetService(),
                new TestAVService()
        );
        var upload = createUpload("documents", "document");

        var repeating = new ReplicatingContainerLayoutElement();
        repeating.setId("rows");
        repeating.setChildren(List.of(upload));

        var layout = new GroupLayoutElement();
        layout.setId("root");
        layout.setChildren(List.of(repeating));

        var inputs = new AuthoredElementValues();
        inputs.put("rows", List.of(
                createReplicatingRow(Map.of("documents", List.of(createFileItem("first.pdf", "blob:first", 1))))
        ));

        var exception = assertThrows(ResponseException.class, () -> service.normalizeInputs(
                layout,
                inputs,
                new MultipartFile[]{
                        new MockMultipartFile("files", "first.pdf", "application/pdf", "1".getBytes(StandardCharsets.UTF_8))
                },
                List.of("blob:first"),
                42L,
                null,
                null
        ));

        assertTrue(exception.getMessage().contains("fehlt eine ID"));
    }

    @Test
    void normalizeInputs_RejectsUploadWithoutConfiguredSubmittedFileName() {
        var service = new FileUploadMultipartInputService(
                new TestProcessInstanceAttachmentService(),
                new TestProcessInstanceAttachmentSetService(),
                new TestAVService()
        );
        var layout = createLayout("documents", null);

        var inputs = new AuthoredElementValues();
        inputs.put("documents", List.of(createFileItem("report.pdf", "blob:report", 3)));

        var exception = assertThrows(ResponseException.class, () -> service.normalizeInputs(
                layout,
                inputs,
                new MultipartFile[]{
                        new MockMultipartFile("files", "report.pdf", "application/pdf", "pdf".getBytes(StandardCharsets.UTF_8))
                },
                List.of("blob:report"),
                42L,
                9L,
                "staff-user"
        ));

        assertTrue(exception.getMessage().contains("Dateiname bei Einreichung"));
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

        var firstUpload = createUpload("firstDocuments", "first");
        firstUpload.setDestinationKey("case.documents");
        firstUpload.setLabel("Case documents");

        var secondUpload = createUpload("secondDocuments", "second");
        secondUpload.setDestinationKey("case.documents");

        var fallbackUpload = createUpload("fallback.documents", "fallback");

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
        assertEquals(1, attachmentService.createdAttachments().get(0).getPosition());
        assertEquals(2, attachmentService.createdAttachments().get(1).getPosition());
        assertEquals(1, attachmentService.createdAttachments().get(2).getPosition());
    }

    @Test
    void normalizeInputs_ContinuesAttachmentPositionsAcrossReplicatingContainerRows() throws Exception {
        var attachmentService = new TestProcessInstanceAttachmentService();
        var attachmentSetService = new TestProcessInstanceAttachmentSetService();
        var service = new FileUploadMultipartInputService(
                attachmentService,
                attachmentSetService,
                new TestAVService()
        );

        var upload = createUpload("documents", "document");
        upload.setDestinationKey("case.documents");

        var repeating = new ReplicatingContainerLayoutElement();
        repeating.setId("rows");
        repeating.setChildren(List.of(upload));

        var layout = new GroupLayoutElement();
        layout.setId("root");
        layout.setChildren(List.of(repeating));

        var inputs = new AuthoredElementValues();
        inputs.put("rows", List.of(
                createReplicatingRow("row-1", Map.of("documents", List.of(createFileItem("first.pdf", "blob:first", 1)))),
                createReplicatingRow("row-2", Map.of("documents", List.of(createFileItem("second.pdf", "blob:second", 1)))),
                createReplicatingRow("row-3", Map.of("documents", List.of(createFileItem("third.pdf", "blob:third", 1))))
        ));

        service.normalizeInputs(
                layout,
                inputs,
                new MultipartFile[]{
                        new MockMultipartFile("files", "first.pdf", "application/pdf", "1".getBytes(StandardCharsets.UTF_8)),
                        new MockMultipartFile("files", "second.pdf", "application/pdf", "2".getBytes(StandardCharsets.UTF_8)),
                        new MockMultipartFile("files", "third.pdf", "application/pdf", "3".getBytes(StandardCharsets.UTF_8))
                },
                List.of("blob:first", "blob:second", "blob:third"),
                42L,
                null,
                null
        );

        assertEquals(1, attachmentSetService.createdSets().size());
        var attachmentSetId = attachmentSetService.createdSets().getFirst().getId();
        assertEquals(attachmentSetId, attachmentService.createdAttachments().get(0).getAttachmentSetId());
        assertEquals(attachmentSetId, attachmentService.createdAttachments().get(1).getAttachmentSetId());
        assertEquals(attachmentSetId, attachmentService.createdAttachments().get(2).getAttachmentSetId());
        assertEquals(List.of(1, 2, 3), attachmentService.createdAttachments()
                .stream()
                .map(ProcessInstanceAttachmentEntity::getPosition)
                .toList());
        assertEquals(List.of("row-1", "row-2", "row-3"), attachmentService.createdAttachments()
                .stream()
                .map(ProcessInstanceAttachmentEntity::getGroup)
                .toList());
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

    private static ReplicatingContainerLayoutElementValue createReplicatingRow(Map<?, ?> values) {
        return createReplicatingRow(null, values);
    }

    private static ReplicatingContainerLayoutElementValue createReplicatingRow(String id, Map<?, ?> values) {
        var authoredValues = new AuthoredElementValues();
        for (var entry : values.entrySet()) {
            if (entry.getKey() instanceof String key) {
                authoredValues.put(key, entry.getValue());
            }
        }
        return new ReplicatingContainerLayoutElementValue()
                .setId(id)
                .setValues(authoredValues);
    }

    private static AuthoredElementValues getReplicatingRowValues(Object row) {
        return assertInstanceOf(ReplicatingContainerLayoutElementValue.class, row).getValues();
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
