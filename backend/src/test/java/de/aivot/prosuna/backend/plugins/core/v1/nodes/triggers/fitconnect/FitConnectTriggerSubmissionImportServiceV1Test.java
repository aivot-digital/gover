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
import dev.fitko.fitconnect.core.validation.api.Severity;
import dev.fitko.fitconnect.rest.model.event.Event;
import dev.fitko.fitconnect.rest.model.event.problems.data.DataJsonSyntaxViolation;
import dev.fitko.fitconnect.rest.model.metadata.ContentStructure;
import dev.fitko.fitconnect.rest.model.metadata.data.Data;
import dev.fitko.fitconnect.rest.model.metadata.data.MimeType;
import dev.fitko.fitconnect.rest.model.metadata.data.SubmissionSchema;
import dev.fitko.fitconnect.rest.model.metadata.v1.MetadataV1;
import dev.fitko.fitconnect.rest.model.submission.PublicService;
import dev.fitko.fitconnect.rest.model.submission.SubmissionForPickup;
import dev.fitko.fitconnect.sdk.api.Attachment;
import dev.fitko.fitconnect.sdk.api.ReceivedSubmission;
import dev.fitko.fitconnect.sdk.api.diagnostics.ReceiveIssue;
import dev.fitko.fitconnect.sdk.api.diagnostics.ReceiveReport;
import dev.fitko.fitconnect.sdk.api.diagnostics.ReceiveStage;
import dev.fitko.fitconnect.sdk.api.event.CaseEvent;
import dev.fitko.fitconnect.sdk.api.event.TransferLog;
import dev.fitko.fitconnect.sdk.clients.Organisation;
import dev.fitko.fitconnect.sdk.clients.OrganisationCases;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
        var attachment = Attachment.builder()
                .fromBytes(attachmentBytes)
                .mimeType("application/pdf")
                .fileName("proof.pdf")
                .description("Nachweis")
                .withCustomId(attachmentId)
                .build();
        fixture.receivedAttachments().add(attachment);
        var metadata = fixture.metadata();
        metadata.setSchema("https://schema.fitko.de/fit-connect/schemas/metadata/1.6.0/metadata.schema.json");

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
        var submission = (Map<String, Object>) importedPayload.get(FitConnectTriggerNodeV1.INITIAL_DATA_KEY_SUBMISSION);
        assertEquals(Instant.parse("2026-09-02T11:59:00Z"), submission.get("submittedAt"));
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

        var order = inOrder(fixture.processInstanceService(), fixture.organisation());
        order.verify(fixture.processInstanceService()).save(any(ProcessInstanceEntity.class));
        order.verify(fixture.organisation()).accept(fixture.receivedSubmission());
        order.verify(fixture.processInstanceService()).save(any(ProcessInstanceEntity.class));
    }

    @Test
    void existingInboundReferenceIsIdempotent() throws Exception {
        var fixture = createFixture("{}");
        when(fixture.processInstanceService().retrieveByInboundReference(anyString()))
                .thenReturn(Optional.of(new ProcessInstanceEntity()));

        fixture.service().importSubmission(null, fixture.node(), fixture.config(), reference(), STARTED_AT);

        verify(fixture.processInstanceService(), never()).create(any(ProcessInstanceEntity.class));
        verify(fixture.organisationFactory(), never()).create(any());
        verify(fixture.organisation(), never()).accept(any(ReceivedSubmission.class));
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
        verify(fixture.organisation()).reject(eq(fixture.receivedSubmission()), anyList());
        verify(fixture.organisation(), never()).accept(any(ReceivedSubmission.class));
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
        verify(fixture.organisation()).reject(eq(fixture.receivedSubmission()), anyList());
        verify(fixture.organisation(), never()).accept(any(ReceivedSubmission.class));
        assertEquals(List.of(ProcessInstanceStatus.Failed), fixture.savedStatuses());
        assertNotNull(fixture.savedInboundReferences().getFirst());
    }

    @Test
    void sdkReceiveErrorsAreRejectedBeforeImport() throws Exception {
        var fixture = createFixture("{}");
        var problem = new DataJsonSyntaxViolation("Ungültiges JSON");
        var report = new ReceiveReport(List.of(new ReceiveIssue(
                Severity.ERROR,
                ReceiveStage.DATA,
                "Die Nutzdaten konnten nicht validiert werden.",
                "Die sendende Stelle muss die Nutzdaten korrigieren.",
                problem
        )));
        var rejectedSubmission = receivedSubmission(
                "{}",
                fixture.metadata(),
                fixture.receivedAttachments(),
                report
        );
        when(fixture.organisation().receive(any(SubmissionForPickup.class))).thenReturn(rejectedSubmission);

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
        verify(fixture.organisation()).reject(
                eq(rejectedSubmission),
                argThat(problems -> problems.size() == 1 && problems.getFirst() == problem)
        );
        verify(fixture.organisation(), never()).accept(any(ReceivedSubmission.class));
        assertEquals(List.of(ProcessInstanceStatus.Failed), fixture.savedStatuses());
        assertNotNull(fixture.savedInboundReferences().getFirst());
    }

    @Test
    void transientRetrievalFailureReleasesInboundReferenceForRetry() throws Exception {
        var fixture = createFixture("{}");
        when(fixture.organisation().receive(any(SubmissionForPickup.class)))
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
        var organisationFactory = mock(FitConnectTriggerOrganisationFactoryV1.class);
        var organisation = mock(Organisation.class);
        var cases = mock(OrganisationCases.class);
        var receivedAttachments = new ArrayList<Attachment>();
        var metadata = new MetadataV1();
        metadata.setSchema("https://schema.fitko.de/fit-connect/schemas/metadata/1.6.0/metadata.schema.json");
        metadata.setContentStructure(new ContentStructure(
                new Data(
                        null,
                        null,
                        new SubmissionSchema(
                                URI.create("https://example.test/schema"),
                                MimeType.APPLICATION_JSON
                        )
                ),
                List.of()
        ));
        var receivedSubmission = receivedSubmission(rawPayload, metadata, receivedAttachments, ReceiveReport.EMPTY);
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

        when(organisationFactory.create(any())).thenReturn(organisation);
        when(organisation.receive(any(SubmissionForPickup.class))).thenReturn(receivedSubmission);
        when(organisation.cases()).thenReturn(cases);
        when(cases.logOf(any(SubmissionForPickup.class))).thenReturn(new TransferLog(List.of(
                CaseEvent.builder()
                        .event(Event.SUBMIT_SUBMISSION)
                        .issueTime(Date.from(Instant.parse("2026-09-02T11:59:00Z")))
                        .problems(List.of())
                        .build()
        )));
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
                organisationFactory,
                JsonMapper.builder().build()
        );

        return new Fixture(
                service,
                processInstanceService,
                attachmentService,
                organisationFactory,
                organisation,
                receivedSubmission,
                metadata,
                receivedAttachments,
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

    private static ReceivedSubmission receivedSubmission(String rawPayload,
                                                         MetadataV1 metadata,
                                                         List<Attachment> attachments,
                                                         ReceiveReport report) {
        return ReceivedSubmission
                .builder()
                .data(rawPayload.getBytes(StandardCharsets.UTF_8))
                .metadata(metadata)
                .submissionId(SUBMISSION_ID)
                .caseId(CASE_ID)
                .destinationId(DESTINATION_ID)
                .serviceType(new PublicService("Test service", "urn:de:service:test"))
                .attachments(attachments)
                .report(report)
                .build();
    }

    private record Fixture(
            FitConnectTriggerSubmissionImportServiceV1 service,
            ProcessInstanceService processInstanceService,
            ProcessInstanceAttachmentService attachmentService,
            FitConnectTriggerOrganisationFactoryV1 organisationFactory,
            Organisation organisation,
            ReceivedSubmission receivedSubmission,
            MetadataV1 metadata,
            List<Attachment> receivedAttachments,
            ProcessNodeEntity node,
            FitConnectTriggerConfigV1 config,
            List<ProcessInstanceStatus> savedStatuses,
            List<Map<String, Object>> savedPayloads,
            List<String> savedInboundReferences
    ) {
    }
}
