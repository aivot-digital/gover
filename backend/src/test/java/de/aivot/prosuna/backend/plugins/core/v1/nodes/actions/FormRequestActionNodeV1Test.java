package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import de.aivot.prosuna.backend.communication.models.CommunicationMessageCallToAction;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.EffectiveElementValues;
import de.aivot.prosuna.backend.elements.models.elements.form.input.FileUploadInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.FileUploadInputElementItem;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElementValue;
import de.aivot.prosuna.backend.elements.uiPresets.SemiAutomaticMessageConfig;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.filters.ProcessInstanceAttachmentFilter;
import de.aivot.prosuna.backend.process.models.ProcessNodeOutput;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskAssignedCustomer;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUICustomer;
import de.aivot.prosuna.backend.process.services.AssignmentContextAssigneeResolverService;
import de.aivot.prosuna.backend.process.services.FileUploadMultipartInputService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.prosuna.backend.process.services.TemplateRenderService;
import de.aivot.prosuna.backend.submission.services.ElementDataTransformService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FormRequestActionNodeV1Test {
    private static final long PROCESS_INSTANCE_ID = 99L;
    private static final long PROCESS_INSTANCE_TASK_ID = 456L;
    private static final String RECIPIENT_IDENTITY_ID = "applicant";

    private ProcessInstanceAttachmentService processInstanceAttachmentService;
    private ProsunaConfig prosunaConfig;
    private FormRequestActionNodeV1 node;

    @BeforeEach
    void setUp() {
        processInstanceAttachmentService = mock(ProcessInstanceAttachmentService.class);
        prosunaConfig = new ProsunaConfig();
        prosunaConfig.setProsunaHostname("https://example.test");
        node = new FormRequestActionNodeV1(
                mock(TemplateRenderService.class),
                mock(AssignmentContextAssigneeResolverService.class),
                prosunaConfig,
                new ElementDataTransformService(),
                processInstanceAttachmentService
        );
    }

    @Test
    void customerAssignmentMessageContainsAFormCallToAction() {
        var configuration = new FormRequestActionNodeV1.NodeConfig();
        configuration.recipientIdentityId = RECIPIENT_IDENTITY_ID;
        var processInstance = new ProcessInstanceEntity().setAccessKey("instance-access");
        var task = new ProcessInstanceTaskEntity().setAccessKey("task-access");

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskAssignedCustomer.class,
                ReflectionTestUtils.invokeMethod(
                        node,
                        "createCustomerAssignmentResult",
                        processInstance,
                        task,
                        configuration,
                        "Bitte Daten ergänzen",
                        "Hallo **Ada**"
                )
        );

        var message = result.getCommunicationRequest().message();
        assertEquals("Hallo **Ada**", message.body());
        assertEquals("Hallo **Ada**", message.htmlBody());
        assertEquals(
                List.of(new CommunicationMessageCallToAction(
                        "Daten einreichen",
                        "https://example.test/process/instance-access/tasks/task-access"
                )),
                message.callToActions()
        );
    }

    @Test
    void getOutputs_ExposesRecipientAndFormSubmissionData() {
        assertEquals(
                List.of(
                        new ProcessNodeOutput(
                                "recipientIdentityId",
                                "Identität",
                                "Die ID der Prozessidentität, an die die Formularanforderung gesendet wurde.",
                                "string"
                        ),
                        new ProcessNodeOutput(
                                "payload",
                                "Zugeordnete Formulardaten",
                                "Enthält alle Formulardaten welche über einen Datenschlüssel zugeordnet wurden.",
                                "Record<string, unknown>"
                        ),
                        new ProcessNodeOutput(
                                "unmapped",
                                "Formular-Rohdaten",
                                "Enthält alle Formulardaten unter der jeweiligen Element-ID des Feldes, unabhängig davon, ob ein Element über einen Datenschlüssel zugewiesen wurde oder nicht.",
                                "Record<string, unknown>"
                        ),
                        new ProcessNodeOutput(
                                "attachments",
                                "Anlagen",
                                "Eine Liste aller Anlagen, die über dieses Formular hochgeladen wurden.",
                                "Array<{ key: string; fileName: string; originalFileName: string; group: string | null; " +
                                        "storageProviderId: number; storagePathFromRoot: string; }>"
                        ),
                        new ProcessNodeOutput(
                                "started",
                                "Eingangszeitstempel",
                                "Der Zeitstempel des Dateneingangs an den Auslöser",
                                "string"
                        )
                ),
                node.getOutputs()
        );
        assertFalse(node.getOutputs().stream().anyMatch(output -> "customerSummaryFiles".equals(output.key())));
        assertFalse(node.getOutputs().stream().anyMatch(output -> "paymentDetails".equals(output.key())));
    }

    @Test
    void onEventFromCustomerTaskView_CreatesOutputsFromFinalEffectiveValues() throws Exception {
        var activeAttachmentKey = UUID.randomUUID();
        var removedAttachmentKey = UUID.randomUUID();
        var foreignAttachmentKey = UUID.randomUUID();
        var activeAttachment = attachment(
                activeAttachmentKey,
                PROCESS_INSTANCE_ID,
                PROCESS_INSTANCE_TASK_ID,
                "Nachweis.pdf"
        );
        var removedAttachment = attachment(
                removedAttachmentKey,
                PROCESS_INSTANCE_ID,
                PROCESS_INSTANCE_TASK_ID,
                "Entfernt.pdf"
        );
        var foreignAttachment = attachment(
                foreignAttachmentKey,
                PROCESS_INSTANCE_ID,
                999L,
                "Fremd.pdf"
        );
        when(processInstanceAttachmentService.list(any(ProcessInstanceAttachmentFilter.class)))
                .thenReturn(new PageImpl<>(List.of(activeAttachment, removedAttachment, foreignAttachment)));

        var nameField = new TextInputElement();
        nameField.setId("name");
        nameField.setDestinationKey("applicant.name");
        var filesField = new FileUploadInputElement();
        filesField.setId("files");
        var rows = new ReplicatingContainerLayoutElement();
        rows.setId("rows");
        rows.setChildren(List.of(filesField));
        var layout = new GroupLayoutElement();
        layout.setId("form");
        layout.setChildren(List.of(nameField, rows));

        var configuration = new FormRequestActionNodeV1.NodeConfig();
        configuration.recipientIdentityId = RECIPIENT_IDENTITY_ID;
        configuration.uiDefinition = layout;

        var activeFile = new FileUploadInputElementItem()
                .setName("Nachweis.pdf")
                .setOriginalFileName("upload.pdf")
                .setUri(FileUploadMultipartInputService.buildAttachmentUri(activeAttachmentKey))
                .setSize(100);
        var foreignFile = new FileUploadInputElementItem()
                .setName("Fremd.pdf")
                .setOriginalFileName("foreign.pdf")
                .setUri(FileUploadMultipartInputService.buildAttachmentUri(foreignAttachmentKey))
                .setSize(200);
        var effectiveValues = new EffectiveElementValues();
        effectiveValues.put("name", "Ada");
        var rowValues = new AuthoredElementValues();
        rowValues.put("files", List.of(activeFile, foreignFile, activeFile));
        effectiveValues.put("rows", List.of(
                new ReplicatingContainerLayoutElementValue()
                        .setId("row-1")
                        .setValues(rowValues)
        ));
        var derived = new DerivedRuntimeElementData().setEffectiveValues(effectiveValues);

        var beforeSubmission = Instant.now();
        var result = node.onEventFromCustomerTaskView(
                context(configuration),
                effectiveValues.toAuthoredElementValues(),
                derived,
                "submit"
        ).orElseThrow();
        var afterSubmission = Instant.now();

        var completed = assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, result);
        assertEquals("submitted", completed.getViaPort());
        assertNull(completed.getProcessData());

        var nodeData = completed.getNodeData();
        assertEquals(RECIPIENT_IDENTITY_ID, nodeData.get("recipientIdentityId"));
        assertEquals(Map.of("applicant", Map.of("name", "Ada")), nodeData.get("payload"));
        assertSame(effectiveValues, nodeData.get("unmapped"));

        var submittedAt = assertInstanceOf(Instant.class, nodeData.get("started"));
        assertFalse(submittedAt.isBefore(beforeSubmission));
        assertFalse(submittedAt.isAfter(afterSubmission));

        @SuppressWarnings("unchecked")
        var attachments = (List<Map<String, Object>>) nodeData.get("attachments");
        assertEquals(1, attachments.size());
        assertEquals(activeAttachmentKey, attachments.getFirst().get("key"));
        assertEquals("Nachweis.pdf", attachments.getFirst().get("fileName"));
        assertEquals("original-Nachweis.pdf", attachments.getFirst().get("originalFileName"));
        assertNull(attachments.getFirst().get("group"));
        assertEquals(7, attachments.getFirst().get("storageProviderId"));
        assertEquals("/attachments/Nachweis.pdf", attachments.getFirst().get("storagePathFromRoot"));

        var filter = ArgumentCaptor.forClass(ProcessInstanceAttachmentFilter.class);
        verify(processInstanceAttachmentService).list(filter.capture());
        assertEquals(PROCESS_INSTANCE_TASK_ID, filter.getValue().getProcessInstanceTaskId());
        assertTrue(attachments.stream().noneMatch(value -> removedAttachmentKey.equals(value.get("key"))));
        assertTrue(attachments.stream().noneMatch(value -> foreignAttachmentKey.equals(value.get("key"))));
    }

    @Test
    void cleanConfigurationForExportRemovesIdentityAndAssignment() {
        var configuration = new AuthoredElementValues();
        configuration.put(FormRequestActionNodeV1.NodeConfig.RECIPIENT_IDENTITY_ID_FIELD_ID, RECIPIENT_IDENTITY_ID);
        configuration.put(
                SemiAutomaticMessageConfig.ManualContent.ASSIGNMENT_FIELD_ID,
                Map.of("user", "staff-1")
        );
        configuration.put("portableValue", "kept");

        var cleaned = node.cleanConfigurationForExport(configuration);

        assertFalse(cleaned.containsKey(FormRequestActionNodeV1.NodeConfig.RECIPIENT_IDENTITY_ID_FIELD_ID));
        assertFalse(cleaned.containsKey(SemiAutomaticMessageConfig.ManualContent.ASSIGNMENT_FIELD_ID));
        assertEquals("kept", cleaned.get("portableValue"));
    }

    @SuppressWarnings("unchecked")
    private static ProcessNodeExecutionContextUICustomer<FormRequestActionNodeV1.NodeConfig> context(
            FormRequestActionNodeV1.NodeConfig configuration
    ) {
        var context = mock(ProcessNodeExecutionContextUICustomer.class);
        when(context.getConfigurationOfExecutingNode()).thenReturn(configuration);
        when(context.getThisProcessInstance()).thenReturn(
                new ProcessInstanceEntity().setId(PROCESS_INSTANCE_ID)
        );
        when(context.getThisTask()).thenReturn(
                new ProcessInstanceTaskEntity().setId(PROCESS_INSTANCE_TASK_ID)
        );
        return context;
    }

    private static ProcessInstanceAttachmentEntity attachment(
            UUID key,
            long processInstanceId,
            long processInstanceTaskId,
            String fileName
    ) {
        return new ProcessInstanceAttachmentEntity()
                .setKey(key)
                .setFileName(fileName)
                .setOriginalFileName("original-" + fileName)
                .setGroup(null)
                .setPosition(1)
                .setAttachmentSetId(1)
                .setProcessInstanceId(processInstanceId)
                .setProcessInstanceTaskId(processInstanceTaskId)
                .setStorageProviderId(7)
                .setStoragePathFromRoot("/attachments/" + fileName);
    }
}
