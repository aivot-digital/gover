package de.aivot.prosuna.backend.plugins.core.v1.nodes.triggers.fitconnect;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentSetService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceService;
import dev.fitko.fitconnect.api.domain.model.attachment.Attachment;
import dev.fitko.fitconnect.api.domain.model.metadata.v1.MetadataV1;
import dev.fitko.fitconnect.api.domain.subscriber.ReceivedSubmission;
import dev.fitko.fitconnect.client.SubscriberClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FitConnectTriggerSubmissionImportServiceV1Test {
    private static final UUID DESTINATION_ID = UUID.fromString("d12caea8-f372-4eb1-b102-b0a228253a11");
    private static final UUID SUBMISSION_ID = UUID.fromString("f39ab143-d91a-474a-b69f-b00f1a1873c2");
    private static final UUID CASE_ID = UUID.fromString("9eec7d3e-dc66-4f82-9f52-1520bf96a32e");
    private static final Instant STARTED_AT = Instant.parse("2026-09-02T12:00:00Z");

    @Test
    void importsPayloadAndAttachmentsBeforeAcceptingSubmission() throws Exception {
        var fixture = createFixture("{\"applicant\":\"Ada\"}");
        var attachmentId = UUID.fromString("bd75e803-2c76-4c2e-9841-29b2e70e945f");
        var attachmentBytes = "proof".getBytes(StandardCharsets.UTF_8);
        var attachment = Attachment.fromByteArray(
                attachmentBytes,
                "application/pdf",
                "proof.pdf",
                "Nachweis",
                attachmentId
        );
        when(fixture.receivedSubmission().getAttachments()).thenReturn(List.of(attachment));
        var metadata = new MetadataV1();
        metadata.setSchema("https://schema.fitko.de/fit-connect/schemas/metadata/1.6.0/metadata.schema.json");
        when(fixture.receivedSubmission().getMetadata()).thenReturn(metadata);

        fixture.service().importSubmission(
                null,
                fixture.node(),
                fixture.config(),
                reference(),
                STARTED_AT
        );

        assertEquals(List.of(ProcessInstanceStatus.Paused, ProcessInstanceStatus.Created), fixture.savedStatuses());
        var importedPayload = fixture.savedPayloads().getFirst();
        assertEquals(Map.of("applicant", "Ada"), importedPayload.get(FitConnectTriggerNodeV1.INITIAL_DATA_KEY_PAYLOAD));
        assertEquals(STARTED_AT, importedPayload.get(FitConnectTriggerNodeV1.INITIAL_DATA_KEY_STARTED));
        @SuppressWarnings("unchecked")
        var importedMetadata = (Map<String, Object>) importedPayload.get(FitConnectTriggerNodeV1.INITIAL_DATA_KEY_METADATA);
        assertEquals(metadata.getSchema(), importedMetadata.get("$schema"));

        @SuppressWarnings("unchecked")
        var attachments = (List<Map<String, Object>>) importedPayload.get(FitConnectTriggerNodeV1.INITIAL_DATA_KEY_ATTACHMENTS);
        assertEquals(1, attachments.size());
        assertEquals(attachmentId.toString(), attachments.getFirst().get("fitConnectAttachmentId"));
        assertEquals("proof.pdf", attachments.getFirst().get("filename"));
        assertEquals(attachmentBytes.length, attachments.getFirst().get("size"));

        var attachmentCaptor = ArgumentCaptor.forClass(ProcessInstanceAttachmentEntity.class);
        verify(fixture.attachmentService()).create(attachmentCaptor.capture());
        assertEquals("proof.pdf", attachmentCaptor.getValue().getFileName());
        assertEquals(10L, attachmentCaptor.getValue().getProcessInstanceId());
        assertEquals(99, attachmentCaptor.getValue().getAttachmentSetId());

        var order = inOrder(fixture.processInstanceService(), fixture.receivedSubmission());
        order.verify(fixture.processInstanceService()).save(any(ProcessInstanceEntity.class));
        order.verify(fixture.receivedSubmission()).acceptSubmission();
        order.verify(fixture.processInstanceService()).save(any(ProcessInstanceEntity.class));
    }

    @Test
    void existingInboundReferenceIsIdempotent() throws Exception {
        var fixture = createFixture("{}");
        when(fixture.processInstanceService().retrieveByInboundReference(anyString()))
                .thenReturn(Optional.of(new ProcessInstanceEntity()));

        fixture.service().importSubmission(null, fixture.node(), fixture.config(), reference(), STARTED_AT);

        verify(fixture.processInstanceService(), never()).create(any(ProcessInstanceEntity.class));
        verify(fixture.subscriberClientFactory(), never()).create(any());
        verify(fixture.receivedSubmission(), never()).acceptSubmission();
    }

    @Test
    void invalidJsonIsRejectedAndKeptAsTerminalFailure() throws Exception {
        var fixture = createFixture("not-json");

        var exception = assertThrows(
                ResponseException.class,
                () -> fixture.service().importSubmission(
                        null,
                        fixture.node(),
                        fixture.config(),
                        reference(),
                        STARTED_AT
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(fixture.receivedSubmission()).rejectSubmission(anyList());
        verify(fixture.receivedSubmission(), never()).acceptSubmission();
        assertEquals(List.of(ProcessInstanceStatus.Failed), fixture.savedStatuses());
        assertNotNull(fixture.savedInboundReferences().getFirst());
    }

    @Test
    void nonObjectJsonIsRejectedWhenCopyingToProcessDataIsEnabled() throws Exception {
        var fixture = createFixture("[1, 2, 3]");
        fixture.config().copyToProcessData = true;

        var exception = assertThrows(
                ResponseException.class,
                () -> fixture.service().importSubmission(
                        null,
                        fixture.node(),
                        fixture.config(),
                        reference(),
                        STARTED_AT
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(fixture.receivedSubmission()).rejectSubmission(anyList());
        verify(fixture.receivedSubmission(), never()).acceptSubmission();
        assertEquals(List.of(ProcessInstanceStatus.Failed), fixture.savedStatuses());
        assertNotNull(fixture.savedInboundReferences().getFirst());
    }

    @Test
    void transientRetrievalFailureReleasesInboundReferenceForRetry() throws Exception {
        var fixture = createFixture("{}");
        when(fixture.subscriberClient().requestSubmission(SUBMISSION_ID))
                .thenThrow(new IllegalStateException("temporarily unavailable"));

        var exception = assertThrows(
                ResponseException.class,
                () -> fixture.service().importSubmission(
                        null,
                        fixture.node(),
                        fixture.config(),
                        reference(),
                        STARTED_AT
                )
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        assertEquals(List.of(ProcessInstanceStatus.Failed), fixture.savedStatuses());
        assertEquals(1, fixture.savedInboundReferences().size());
        assertNull(fixture.savedInboundReferences().getFirst());
    }

    private static Fixture createFixture(String rawPayload) throws Exception {
        var processInstanceService = mock(ProcessInstanceService.class);
        var attachmentService = mock(ProcessInstanceAttachmentService.class);
        var attachmentSetService = mock(ProcessInstanceAttachmentSetService.class);
        var subscriberClientFactory = mock(FitConnectTriggerSubscriberClientFactoryV1.class);
        var subscriberClient = mock(SubscriberClient.class);
        var receivedSubmission = mock(ReceivedSubmission.class);
        var savedStatuses = new ArrayList<ProcessInstanceStatus>();
        var savedPayloads = new ArrayList<Map<String, Object>>();
        var savedInboundReferences = new ArrayList<String>();

        when(processInstanceService.retrieveByInboundReference(anyString())).thenReturn(Optional.empty());
        when(processInstanceService.create(any(ProcessInstanceEntity.class))).thenAnswer(invocation -> {
            var instance = invocation.getArgument(0, ProcessInstanceEntity.class);
            instance.setId(10L);
            return instance;
        });
        when(processInstanceService.save(any(ProcessInstanceEntity.class))).thenAnswer(invocation -> {
            var instance = invocation.getArgument(0, ProcessInstanceEntity.class);
            savedStatuses.add(instance.getStatus());
            savedPayloads.add(instance.getInitialPayload());
            savedInboundReferences.add(instance.getInboundReference());
            return instance;
        });

        when(attachmentSetService.create(any(ProcessInstanceAttachmentSetEntity.class)))
                .thenAnswer(invocation -> invocation
                        .getArgument(0, ProcessInstanceAttachmentSetEntity.class)
                        .setId(99));
        when(attachmentService.create(any(ProcessInstanceAttachmentEntity.class)))
                .thenAnswer(invocation -> invocation
                        .getArgument(0, ProcessInstanceAttachmentEntity.class)
                        .setKey(UUID.fromString("8135f27f-d7e0-4bc0-95bd-8d57e65b8daf"))
                        .setStorageProviderId(7)
                        .setStoragePathFromRoot("fit-connect/proof.pdf"));

        when(subscriberClientFactory.create(any())).thenReturn(subscriberClient);
        when(subscriberClient.requestSubmission(SUBMISSION_ID)).thenReturn(receivedSubmission);
        when(receivedSubmission.getDestinationId()).thenReturn(DESTINATION_ID);
        when(receivedSubmission.getSubmissionId()).thenReturn(SUBMISSION_ID);
        when(receivedSubmission.getCaseId()).thenReturn(CASE_ID);
        when(receivedSubmission.getDataAsBytes()).thenReturn(rawPayload.getBytes(StandardCharsets.UTF_8));
        when(receivedSubmission.getSubmittedAt()).thenReturn(Date.from(Instant.parse("2026-09-02T11:59:00Z")));
        when(receivedSubmission.getRegion()).thenReturn(Optional.empty());
        when(receivedSubmission.getDataMimeType()).thenReturn("application/json");
        when(receivedSubmission.getDataSchemaUri()).thenReturn(URI.create("https://example.test/schema"));
        when(receivedSubmission.getAttachments()).thenReturn(List.of());

        var node = new ProcessNodeEntity()
                .setId(5)
                .setProcessId(1)
                .setProcessVersion(3)
                .setDataKey("fitConnect");
        var config = new FitConnectTriggerConfigV1();
        var service = new FitConnectTriggerSubmissionImportServiceV1(
                processInstanceService,
                attachmentService,
                attachmentSetService,
                subscriberClientFactory,
                JsonMapper.builder().build()
        );

        return new Fixture(
                service,
                processInstanceService,
                attachmentService,
                subscriberClientFactory,
                subscriberClient,
                receivedSubmission,
                node,
                config,
                savedStatuses,
                savedPayloads,
                savedInboundReferences
        );
    }

    private static FitConnectTriggerCallbackPayloadV1.SubmissionReference reference() {
        return new FitConnectTriggerCallbackPayloadV1.SubmissionReference(
                DESTINATION_ID,
                SUBMISSION_ID,
                CASE_ID
        );
    }

    private record Fixture(
            FitConnectTriggerSubmissionImportServiceV1 service,
            ProcessInstanceService processInstanceService,
            ProcessInstanceAttachmentService attachmentService,
            FitConnectTriggerSubscriberClientFactoryV1 subscriberClientFactory,
            SubscriberClient subscriberClient,
            ReceivedSubmission receivedSubmission,
            ProcessNodeEntity node,
            FitConnectTriggerConfigV1 config,
            List<ProcessInstanceStatus> savedStatuses,
            List<Map<String, Object>> savedPayloads,
            List<String> savedInboundReferences
    ) {
    }
}
