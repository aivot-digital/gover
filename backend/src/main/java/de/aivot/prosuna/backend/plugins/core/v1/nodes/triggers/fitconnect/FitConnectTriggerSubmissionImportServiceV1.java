package de.aivot.prosuna.backend.plugins.core.v1.nodes.triggers.fitconnect;

import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessTestClaimEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.services.FileUploadMultipartInputService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentSetService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceService;
import dev.fitko.fitconnect.api.domain.model.attachment.Attachment;
import dev.fitko.fitconnect.api.domain.model.event.problems.data.DataJsonSyntaxViolation;
import dev.fitko.fitconnect.api.domain.model.event.problems.metadata.UnsupportedDataSchema;
import dev.fitko.fitconnect.api.domain.subscriber.ReceivedSubmission;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Imports one callback submission into a paused process instance and acknowledges it afterwards. */
@Service
public class FitConnectTriggerSubmissionImportServiceV1 {
    private static final Logger logger = LoggerFactory.getLogger(FitConnectTriggerSubmissionImportServiceV1.class);
    private static final String INBOUND_REFERENCE_PREFIX = "fit-connect:";

    private final ProcessInstanceService processInstanceService;
    private final ProcessInstanceAttachmentService processInstanceAttachmentService;
    private final ProcessInstanceAttachmentSetService processInstanceAttachmentSetService;
    private final FitConnectTriggerSubscriberClientFactoryV1 subscriberClientFactory;
    private final JsonMapper jsonMapper;

    public FitConnectTriggerSubmissionImportServiceV1(
            ProcessInstanceService processInstanceService,
            ProcessInstanceAttachmentService processInstanceAttachmentService,
            ProcessInstanceAttachmentSetService processInstanceAttachmentSetService,
            FitConnectTriggerSubscriberClientFactoryV1 subscriberClientFactory,
            JsonMapper jsonMapper) {
        this.processInstanceService = processInstanceService;
        this.processInstanceAttachmentService = processInstanceAttachmentService;
        this.processInstanceAttachmentSetService = processInstanceAttachmentSetService;
        this.subscriberClientFactory = subscriberClientFactory;
        this.jsonMapper = jsonMapper;
    }

    public void importSubmission(@Nullable ProcessTestClaimEntity testClaim,
                                 @Nonnull ProcessNodeEntity node,
                                 @Nonnull FitConnectTriggerConfigV1 config,
                                 @Nonnull FitConnectTriggerCallbackPayloadV1.SubmissionReference reference,
                                 @Nonnull Instant startedAt) throws ResponseException {
        var inboundReference = createInboundReference(reference);
        if (processInstanceService.retrieveByInboundReference(inboundReference).isPresent()) {
            return;
        }

        var instance = createReservation(testClaim, node, reference, inboundReference, startedAt);
        if (instance == null) {
            return;
        }

        var terminalAtFitConnect = false;
        try {
            var subscriberClient = subscriberClientFactory.create(config);
            var receivedSubmission = subscriberClient.requestSubmission(reference.submissionId());
            validateReceivedSubmission(receivedSubmission, reference);

            var payload = parsePayload(receivedSubmission);
            if (Boolean.TRUE.equals(config.copyToProcessData) && !(payload instanceof Map<?, ?>)) {
                receivedSubmission.rejectSubmission(List.of(new UnsupportedDataSchema()));
                terminalAtFitConnect = true;
                throw ResponseException.badRequest(
                        "Die FIT-Connect-Einreichung enthält kein JSON-Objekt und kann deshalb nicht in die Vorgangsdaten kopiert werden."
                );
            }

            var attachmentImport = importAttachments(instance, node, receivedSubmission);
            var nodeData = createNodeData(
                    reference,
                    receivedSubmission,
                    payload,
                    attachmentImport,
                    startedAt
            );

            instance.setInitialPayload(nodeData);
            processInstanceService.save(instance);

            receivedSubmission.acceptSubmission();
            terminalAtFitConnect = true;

            instance.setStatus(ProcessInstanceStatus.Created);
            processInstanceService.save(instance);
        } catch (Exception e) {
            var terminalException = e instanceof TerminalSubmissionException;
            markFailed(instance, terminalAtFitConnect || terminalException);
            if (e instanceof TerminalSubmissionException terminalSubmissionException) {
                throw terminalSubmissionException.responseException();
            }
            if (e instanceof ResponseException responseException) {
                throw responseException;
            }
            throw ResponseException.internalServerError(
                    "Die FIT-Connect-Einreichung konnte nicht in einen Vorgang importiert werden.",
                    e
            );
        }
    }

    @Nullable
    private ProcessInstanceEntity createReservation(
            @Nullable ProcessTestClaimEntity testClaim,
            @Nonnull ProcessNodeEntity node,
            @Nonnull FitConnectTriggerCallbackPayloadV1.SubmissionReference reference,
            @Nonnull String inboundReference,
            @Nonnull Instant startedAt) throws ResponseException {
        var initialPayload = createEmptyNodeData(reference, startedAt);
        var instance = new ProcessInstanceEntity(
                null,
                null,
                null,
                node.getProcessId(),
                node.getProcessVersion(),
                ProcessInstanceStatus.Paused,
                null,
                null,
                List.of(),
                new IdentityDataMap(),
                startedAt,
                startedAt,
                null,
                null,
                initialPayload,
                node.getId(),
                null,
                testClaim != null ? testClaim.getId() : null
        ).setInboundReference(inboundReference);

        try {
            return processInstanceService.create(instance);
        } catch (ResponseException e) {
            if (processInstanceService.retrieveByInboundReference(inboundReference).isPresent()) {
                return null;
            }
            throw e;
        }
    }

    @Nonnull
    private Object parsePayload(@Nonnull ReceivedSubmission submission) throws ResponseException, TerminalSubmissionException {
        try {
            return jsonMapper.readValue(submission.getDataAsBytes(), Object.class);
        } catch (RuntimeException e) {
            try {
                submission.rejectSubmission(List.of(new DataJsonSyntaxViolation()));
            } catch (Exception rejectionException) {
                e.addSuppressed(rejectionException);
                throw ResponseException.badRequest("Die FIT-Connect-Einreichung enthält kein gültiges JSON.", e);
            }
            throw new TerminalSubmissionException(
                    ResponseException.badRequest("Die FIT-Connect-Einreichung enthält kein gültiges JSON.", e)
            );
        }
    }

    private void validateReceivedSubmission(
            @Nonnull ReceivedSubmission submission,
            @Nonnull FitConnectTriggerCallbackPayloadV1.SubmissionReference reference) throws ResponseException {
        if (!reference.destinationId().equals(submission.getDestinationId()) ||
                !reference.submissionId().equals(submission.getSubmissionId()) ||
                !reference.caseId().equals(submission.getCaseId())) {
            throw ResponseException.internalServerError(
                    "Die abgerufene FIT-Connect-Einreichung stimmt nicht mit dem Callback überein."
            );
        }
    }

    @Nonnull
    private AttachmentImport importAttachments(@Nonnull ProcessInstanceEntity instance,
                                               @Nonnull ProcessNodeEntity node,
                                               @Nonnull ReceivedSubmission submission) throws ResponseException, IOException {
        var attachmentSet = processInstanceAttachmentSetService.create(
                new ProcessInstanceAttachmentSetEntity()
                        .setName("FIT-Connect-Anhänge")
                        .setDataKey(node.getDataKey())
                        .setProcessInstanceId(instance.getId())
                        .setProcessInstanceTaskId(null)
        );

        var attachmentData = new ArrayList<Map<String, Object>>();
        var fileItems = new ArrayList<Object>();
        var attachments = submission.getAttachments() == null ? List.<Attachment>of() : submission.getAttachments();
        var position = 1;
        for (var fitConnectAttachment : attachments) {
            var fileName = normalizeFileName(fitConnectAttachment.getFileName(), fitConnectAttachment.getAttachmentId());
            final byte[] content;
            try (var inputStream = fitConnectAttachment.getDataAsInputStream()) {
                content = inputStream.readAllBytes();
            }

            var processAttachment = processInstanceAttachmentService.create(
                    ProcessInstanceAttachmentEntity.of(
                            fileName,
                            position++,
                            instance.getId(),
                            null,
                            content
                    ).setAttachmentSetId(attachmentSet.getId())
            );

            var item = new LinkedHashMap<String, Object>();
            item.put("fitConnectAttachmentId", fitConnectAttachment.getAttachmentId() == null ? null : fitConnectAttachment.getAttachmentId().toString());
            item.put("key", processAttachment.getKey().toString());
            item.put("filename", processAttachment.getFileName());
            item.put("originalFilename", processAttachment.getOriginalFileName());
            item.put("description", fitConnectAttachment.getDescription());
            item.put("mimeType", fitConnectAttachment.getMimeType());
            item.put("purpose", fitConnectAttachment.getPurpose() == null ? null : fitConnectAttachment.getPurpose().value());
            item.put("size", content.length);
            item.put("storageProviderId", processAttachment.getStorageProviderId());
            item.put("storagePathFromRoot", processAttachment.getStoragePathFromRoot());
            attachmentData.add(item);
            fileItems.add(FileUploadMultipartInputService.buildAttachmentItem(processAttachment, content.length));
        }

        return new AttachmentImport(List.copyOf(attachmentData), List.copyOf(fileItems));
    }

    @Nonnull
    private Map<String, Object> createNodeData(
            @Nonnull FitConnectTriggerCallbackPayloadV1.SubmissionReference reference,
            @Nonnull ReceivedSubmission receivedSubmission,
            @Nonnull Object payload,
            @Nonnull AttachmentImport attachmentImport,
            @Nonnull Instant startedAt) throws ResponseException {
        var nodeData = new LinkedHashMap<String, Object>();
        nodeData.put(FitConnectTriggerNodeV1.INITIAL_DATA_KEY_PAYLOAD, payload);
        nodeData.put(FitConnectTriggerNodeV1.INITIAL_DATA_KEY_SUBMISSION, createSubmissionData(reference, receivedSubmission));
        nodeData.put(FitConnectTriggerNodeV1.INITIAL_DATA_KEY_METADATA, normalizeMetadata(receivedSubmission));
        nodeData.put(FitConnectTriggerNodeV1.INITIAL_DATA_KEY_ATTACHMENTS, attachmentImport.attachments());
        nodeData.put(FitConnectTriggerNodeV1.INITIAL_DATA_KEY_FILES, attachmentImport.files());
        nodeData.put(FitConnectTriggerNodeV1.INITIAL_DATA_KEY_STARTED, startedAt);
        return nodeData;
    }

    @Nonnull
    private Map<String, Object> createEmptyNodeData(
            @Nonnull FitConnectTriggerCallbackPayloadV1.SubmissionReference reference,
            @Nonnull Instant startedAt) {
        var nodeData = new LinkedHashMap<String, Object>();
        nodeData.put(FitConnectTriggerNodeV1.INITIAL_DATA_KEY_PAYLOAD, null);
        nodeData.put(FitConnectTriggerNodeV1.INITIAL_DATA_KEY_SUBMISSION, createSubmissionData(reference, null));
        nodeData.put(FitConnectTriggerNodeV1.INITIAL_DATA_KEY_METADATA, Map.of());
        nodeData.put(FitConnectTriggerNodeV1.INITIAL_DATA_KEY_ATTACHMENTS, List.of());
        nodeData.put(FitConnectTriggerNodeV1.INITIAL_DATA_KEY_FILES, List.of());
        nodeData.put(FitConnectTriggerNodeV1.INITIAL_DATA_KEY_STARTED, startedAt);
        return nodeData;
    }

    @Nonnull
    private Map<String, Object> createSubmissionData(
            @Nonnull FitConnectTriggerCallbackPayloadV1.SubmissionReference reference,
            @Nullable ReceivedSubmission receivedSubmission) {
        var data = new LinkedHashMap<String, Object>();
        data.put("destinationId", reference.destinationId().toString());
        data.put("submissionId", reference.submissionId().toString());
        data.put("caseId", reference.caseId().toString());
        data.put("submittedAt", receivedSubmission == null || receivedSubmission.getSubmittedAt() == null ?
                null : receivedSubmission.getSubmittedAt().toInstant());

        var service = new LinkedHashMap<String, Object>();
        var serviceType = receivedSubmission == null ? null : receivedSubmission.getServiceType();
        service.put("identifier", serviceType == null ? null : serviceType.getIdentifier());
        service.put("name", serviceType == null ? null : serviceType.getName());
        data.put("service", service);

        data.put("region", receivedSubmission == null ? null : receivedSubmission.getRegion().orElse(null));
        data.put("dataMimeType", receivedSubmission == null ? null : receivedSubmission.getDataMimeType());
        data.put("dataSchemaUri", receivedSubmission == null || receivedSubmission.getDataSchemaUri() == null ?
                null : receivedSubmission.getDataSchemaUri().toString());
        return data;
    }

    @Nonnull
    private Map<String, Object> normalizeMetadata(@Nonnull ReceivedSubmission submission) throws ResponseException {
        try {
            if (submission.getMetadata() == null) {
                return Map.of();
            }
            var metadata = jsonMapper.convertValue(submission.getMetadata(), Map.class);
            return metadata == null ? Map.of() : metadata;
        } catch (Exception e) {
            throw ResponseException.internalServerError(
                    "Die FIT-Connect-Metadaten konnten nicht für die Prozessinstanz aufbereitet werden.",
                    e
            );
        }
    }

    @Nonnull
    private String normalizeFileName(@Nullable String rawFileName,
                                     @Nullable java.util.UUID attachmentId) {
        var fileName = rawFileName == null ? null : rawFileName.trim();
        if (fileName == null || fileName.isEmpty()) {
            fileName = "fit-connect-attachment-" + (attachmentId == null ? "unknown" : attachmentId) + ".dat";
        }
        return fileName.length() <= 255 ? fileName : fileName.substring(0, 255);
    }

    private void markFailed(@Nonnull ProcessInstanceEntity instance,
                            boolean terminalAtFitConnect) {
        try {
            instance.setStatus(ProcessInstanceStatus.Failed);
            if (!terminalAtFitConnect) {
                instance.setInboundReference(null);
            }
            processInstanceService.save(instance);
        } catch (Exception e) {
            logger
                    .atError()
                    .setMessage("Failed to persist FIT-Connect import failure")
                    .setCause(e)
                    .addKeyValue("processInstanceId", instance.getId())
                    .log();
        }
    }

    @Nonnull
    private static String createInboundReference(
            @Nonnull FitConnectTriggerCallbackPayloadV1.SubmissionReference reference) {
        return INBOUND_REFERENCE_PREFIX + reference.destinationId() + ":" + reference.submissionId();
    }

    private record AttachmentImport(
            @Nonnull List<Map<String, Object>> attachments,
            @Nonnull List<Object> files
    ) {
    }

    private static final class TerminalSubmissionException extends Exception {
        private final ResponseException responseException;

        private TerminalSubmissionException(@Nonnull ResponseException responseException) {
            super(responseException);
            this.responseException = responseException;
        }

        @Nonnull
        private ResponseException responseException() {
            return responseException;
        }
    }
}
