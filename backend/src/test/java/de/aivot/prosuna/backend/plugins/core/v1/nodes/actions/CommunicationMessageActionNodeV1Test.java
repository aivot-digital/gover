package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import de.aivot.prosuna.backend.department.entities.VDepartmentShadowedEntity;
import de.aivot.prosuna.backend.department.services.VDepartmentShadowedService;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.EffectiveElementValues;
import de.aivot.prosuna.backend.elements.models.elements.form.input.AssignmentContextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.AssignmentContextInputElementValue;
import de.aivot.prosuna.backend.elements.models.elements.form.input.ProcessIdentityIdInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.ProcessInstanceAttachmentSetSelectElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.uiPresets.SemiAutomaticMessageConfig;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskAssigned;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUIStaff;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.services.AssignmentContextAssigneeResolverService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentSetService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEventEntity;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceHistoryEventRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunicationMessageActionNodeV1Test {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final Integer NODE_ID = 17;
    private static final Long PROCESS_INSTANCE_ID = 99L;
    private static final Long TASK_ID = 123L;

    @Test
    void deadlineDefinitionsFollowTheAuthoredDispatchMode() {
        var node = createNode(mock(AssignmentContextAssigneeResolverService.class));
        var config = new CommunicationMessageActionNodeV1.Configuration();
        assertEquals(List.of(), node.getDeadlines(config).stream().map(definition -> definition.key()).toList());
        config.messageConfig = new SemiAutomaticMessageConfig.LayoutConfig();
        config.messageConfig.executionType = "manual";
        assertEquals(List.of("messageDispatch"), node.getDeadlines(config).stream().map(definition -> definition.key()).toList());
        assertEquals("message_dispatch_overdue", node.getDeadlines(config).getFirst().port().key());
        config.messageConfig.executionType = "automatic";
        assertEquals(List.of(), node.getDeadlines(config).stream().map(definition -> definition.key()).toList());
    }

    @Test
    void metadataExposesAutomaticAndSemiAutomaticExecutionAndTypedOutputs() {
        var node = createNode(mock(AssignmentContextAssigneeResolverService.class));

        assertArrayEquals(
                new ProcessNodeExecutionType[]{
                        ProcessNodeExecutionType.Automatic,
                        ProcessNodeExecutionType.SemiAutomatic
                },
                node.getExecutionTypes()
        );
        assertEquals(
                "Versendet eine Nachricht über den hinterlegten Zustellweg einer Identität – automatisch oder nach manueller Prüfung.",
                node.getAbstract()
        );
        assertEquals(
                List.of(
                        "string",
                        "number | null",
                        "string",
                        "string",
                        "Array<string>",
                        "string",
                        "Record<string, unknown>"
                ),
                node.getOutputs().stream().map(output -> output.typeDefinition()).toList()
        );
        assertEquals(
                List.of("identityId", "communicationProviderBindingId", "subject", "body", "attachmentSetDataKeys", "sentAt", "sendResult"),
                node.getOutputs().stream().map(output -> output.key()).toList()
        );
        var outputLabels = node.getOutputs().stream().collect(java.util.stream.Collectors.toMap(
                output -> output.key(), output -> output.label()
        ));
        assertEquals("Empfangende Identität", outputLabels.get("identityId"));
        assertEquals("Nachricht", outputLabels.get("body"));
        assertEquals("Vorbereitet am", outputLabels.get("sentAt"));
        assertEquals("Versandergebnis", outputLabels.get("sendResult"));
        var preparedAt = node.getOutputs().stream().filter(output -> output.key().equals("sentAt")).findFirst().orElseThrow();
        assertTrue(preparedAt.description().contains("Der tatsächliche Versand folgt danach"));
        var port = node.getPorts(configuration("automatic")).getFirst();
        assertEquals("output", port.key());
        assertEquals("Nachricht versendet", port.label());
        assertTrue(port.description().contains("über den hinterlegten Zustellweg versendet"));
    }

    @Test
    void configurationUsesSharedSemiAutomaticMessageLayout() throws Exception {
        var node = createNode(mock(AssignmentContextAssigneeResolverService.class));
        var processNode = mock(ProcessNodeEntity.class);
        when(processNode.getProcessId()).thenReturn(PROCESS_ID);
        when(processNode.getProcessVersion()).thenReturn(PROCESS_VERSION);

        var layout = node.getConfigurationLayout(new ProcessNodeDefinitionConfigurationLayoutContext(
                null,
                mock(ProcessEntity.class),
                mock(ProcessVersionEntity.class),
                processNode
        ));

        var identity = layout.findChild(
                CommunicationMessageActionNodeV1.Configuration.IDENTITY_ID_FIELD_ID,
                ProcessIdentityIdInputElement.class
        ).orElseThrow();
        assertEquals("Empfangende Identität", identity.getLabel());
        assertTrue(identity.getHint().contains("für diese Identität hinterlegten Zustellweg"));
        var attachments = layout.findChild(
                CommunicationMessageActionNodeV1.Configuration.ATTACHMENTS_FIELD_ID,
                ProcessInstanceAttachmentSetSelectElement.class
        ).orElseThrow();
        assertEquals("Anlagensätze für Anhänge", attachments.getLabel());
        assertTrue(attachments.getHint().contains("vollständig"));
        assertTrue(attachments.getHint().contains("Ohne Auswahl wird die Nachricht ohne Anhänge versendet"));
        assertTrue(layout.findChild(
                SemiAutomaticMessageConfig.GROUP_ID,
                GroupLayoutElement.class
        ).isPresent());

        var executionType = layout.findChild(
                SemiAutomaticMessageConfig.LayoutConfig.EXECUTION_TYPE_FIELD_ID,
                SelectInputElement.class
        ).orElseThrow();
        assertEquals(List.of(
                SelectInputElementOption.of("automatic", "Automatisch versenden"),
                SelectInputElementOption.of("manual", "Vor Versand prüfen und bearbeiten")
        ), executionType.getOptions());

        assertNotNull(layout.findChild(
                SemiAutomaticMessageConfig.AutomaticContent.GROUP_ID,
                GroupLayoutElement.class
        ).orElseThrow().getVisibility());
        assertNotNull(layout.findChild(
                SemiAutomaticMessageConfig.ManualContent.GROUP_ID,
                GroupLayoutElement.class
        ).orElseThrow().getVisibility());

        var assignment = layout.findChild(
                SemiAutomaticMessageConfig.ManualContent.ASSIGNMENT_FIELD_ID,
                AssignmentContextInputElement.class
        ).orElseThrow();
        assertEquals("Wählen Sie Organisationseinheiten, Teams oder Mitarbeiter:innen aus. Prosuna weist die Aufgabe einer berechtigten Person aus diesem Personenkreis zu.",
                assignment.getText());
        assertEquals(List.of("orgUnit", "team", "user"), assignment.getAllowedTypes());
        assertEquals(PROCESS_ID, assignment.getProcessAccessConstraint().getProcessId());
        assertEquals(PROCESS_VERSION, assignment.getProcessAccessConstraint().getProcessVersion());
        assertEquals(
                List.of(ProcessPermissionProvider.PROCESS_INSTANCE_EDIT_TASK),
                assignment.getProcessAccessConstraint().getRequiredPermissions()
        );
    }

    @Test
    void effectiveValuesMapIntoSharedNestedConfiguration() throws Exception {
        var values = new EffectiveElementValues();
        values.put(CommunicationMessageActionNodeV1.Configuration.IDENTITY_ID_FIELD_ID, "applicant");
        values.put(
                SemiAutomaticMessageConfig.LayoutConfig.EXECUTION_TYPE_FIELD_ID,
                SemiAutomaticMessageConfig.LayoutConfig.EXECUTION_TYPE_AUTOMATIC
        );
        values.put(SemiAutomaticMessageConfig.LayoutConfig.SIGNATURE_DEPARTMENT_FIELD_ID_1, 17);
        values.put(SemiAutomaticMessageConfig.LayoutConfig.SIGNATURE_DEPARTMENT_FIELD_ID_2, 29);
        values.put(SemiAutomaticMessageConfig.AutomaticContent.SUBJECT_FIELD_ID, "Subject");
        values.put(SemiAutomaticMessageConfig.AutomaticContent.CONTENT_FIELD_ID, "Content");

        var configuration = ElementPOJOMapper.mapToPOJO(
                values,
                CommunicationMessageActionNodeV1.Configuration.class
        );

        assertEquals("applicant", configuration.identityId);
        assertNotNull(configuration.messageConfig);
        assertEquals("automatic", configuration.messageConfig.executionType);
        assertNotNull(configuration.messageConfig.automaticContent);
        assertEquals(17, configuration.messageConfig.automaticContent.signatureDepartmentId);
        assertEquals("Subject", configuration.messageConfig.automaticContent.subject);
        assertEquals("Content", configuration.messageConfig.automaticContent.content);
        assertNotNull(configuration.messageConfig.manualContent);
        assertEquals(29, configuration.messageConfig.manualContent.signatureDepartmentId);
    }

    @Test
    void initAutomaticReturnsCommunicationRequestForConfiguredIdentity() throws Exception {
        var departmentService = mock(VDepartmentShadowedService.class);
        var node = createNode(mock(AssignmentContextAssigneeResolverService.class), departmentService);
        var configuration = configuration("automatic");
        configuration.messageConfig.automaticContent.signatureDepartmentId = 17;
        var signatureDepartment = new VDepartmentShadowedEntity().setId(17).setName("Bürgerbüro");
        when(departmentService.retrieve(17)).thenReturn(Optional.of(signatureDepartment));
        var processInstance = processInstance();
        var executionData = new ProcessExecutionData().addProcessData(Map.of("caseNumber", "123"));
        var context = initContext(configuration, executionData, processInstance, mock(ProcessInstanceTaskEntity.class));

        var result = assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, node.init(context));
        assertNull(result.getNewDeadlineKey());

        var communicationRequest = result.getCommunicationRequest();
        assertNotNull(communicationRequest);
        assertEquals("applicant", communicationRequest.recipientIdentityId());
        assertEquals("sendResult", communicationRequest.nodeDataOutputKey());
        assertEquals("Subject 123", communicationRequest.message().subject());
        assertEquals("Hello", communicationRequest.message().body());
        assertEquals("Hello", communicationRequest.message().markdownBody());
        assertEquals(result.getNodeData().get("sentAt"), communicationRequest.message().timestamp());
        assertSame(signatureDepartment, communicationRequest.message().signatureDepartment());
        assertEquals(5, result.getNodeData().get("communicationProviderBindingId"));
    }

    @ParameterizedTest
    @CsvSource({"' Antragstellende ', Antragstellende", ", applicant", "'', applicant", "'   ', applicant"})
    void preparingCommunicationRecordsOneHistoryEventWithoutClaimingSuccess(String title, String expected) throws Exception {
        var node = createNode(mock(AssignmentContextAssigneeResolverService.class));
        var instance = processInstance();
        instance.getIdentities().computeIfPresent("applicant", (_key, identity) -> identity.withTitle(title));
        var context = initContext(configuration("automatic"), new ProcessExecutionData(), instance, task());
        var repository = mock(ProcessInstanceHistoryEventRepository.class);
        when(context.getLogger()).thenReturn(new ProcessNodeExecutionLogger(PROCESS_INSTANCE_ID, TASK_ID, null, null, repository));

        var result = node.init(context);

        assertNotNull(result.getCommunicationRequest());
        var captor = ArgumentCaptor.forClass(ProcessInstanceEventEntity.class);
        verify(repository).save(captor.capture());
        var event = captor.getValue();
        assertEquals("Versand vorbereitet", event.getTitle());
        assertEquals("Der Versand der Nachricht mit dem Betreff „Subject 123“ an die Identität „" + expected + "“ wurde vorbereitet.", event.getMessage());
        assertTrue(event.getHistoryRelevant());
        assertTrue(event.getAudit());
        assertFalse(event.getTechnical());
        assertEquals("applicant", event.getConcernedIdentityId());
        assertEquals(title, event.getConcernedIdentityTitle());
    }

    @Test
    void initManualAssignsStaffWithoutSending() throws Exception {
        var assignmentResolver = mock(AssignmentContextAssigneeResolverService.class);
        var node = createNode(assignmentResolver);
        var configuration = configuration("manual");
        var processInstance = processInstance();
        var processNode = processNode();
        var task = task();
        var executionData = new ProcessExecutionData().addProcessData(Map.of("caseNumber", "123"));
        var context = initContext(configuration, executionData, processInstance, task, processNode);

        when(assignmentResolver.resolveAssignee(
                eq(PROCESS_ID),
                eq(PROCESS_VERSION),
                eq(PROCESS_INSTANCE_ID),
                eq(NODE_ID),
                eq(TASK_ID),
                nullable(Integer.class),
                nullable(String.class),
                same(configuration.messageConfig.manualContent.assignmentContext),
                eq(List.of(ProcessPermissionProvider.PROCESS_INSTANCE_EDIT_TASK))
        )).thenReturn(Optional.of("staff-1"));

        var result = assertInstanceOf(ProcessNodeExecutionResultTaskAssigned.class, node.init(context));
        assertEquals("messageDispatch", result.getNewDeadlineKey());

        assertEquals("staff-1", result.getAssignedUserId());
        assertEquals(Map.of("caseNumber", "123"), result.getProcessData());
        assertEquals(Map.of(), result.getRuntimeData());
        verify(assignmentResolver).resolveAssignee(
                any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void staffTaskProvidesResolvedDefaultsAndSendsEditedValues() throws Exception {
        var departmentService = mock(VDepartmentShadowedService.class);
        var node = createNode(mock(AssignmentContextAssigneeResolverService.class), departmentService);
        var configuration = configuration("manual");
        configuration.messageConfig.manualContent.signatureDepartmentId = 17;
        var signatureDepartment = new VDepartmentShadowedEntity().setId(17).setName("Bürgerbüro");
        when(departmentService.retrieve(17)).thenReturn(Optional.of(signatureDepartment));
        var executionData = new ProcessExecutionData().addProcessData(Map.of("name", "Ada"));
        var context = staffContext(configuration, executionData, processInstance(), task());
        var repository = mock(ProcessInstanceHistoryEventRepository.class);
        when(context.getLogger()).thenReturn(new ProcessNodeExecutionLogger(PROCESS_INSTANCE_ID, TASK_ID, "staff-1", null, repository));

        var view = node.getStaffTaskView(context);
        var layout = (GroupLayoutElement) view.layout();
        var subjectField = layout.findChild("subject", TextInputElement.class).orElseThrow();
        assertTrue(Boolean.TRUE.equals(subjectField.getRequired()));
        assertEquals("Betreff der Nachricht", subjectField.getLabel());
        var bodyField = layout.findChild("body", RichTextInputElement.class).orElseThrow();
        assertTrue(Boolean.TRUE.equals(bodyField.getRequired()));
        assertEquals("Nachricht", bodyField.getLabel());
        assertEquals("Prüfen und bearbeiten Sie die Nachricht vor dem Versand.", bodyField.getHint());
        assertEquals("send", view.events().getFirst().event());
        assertEquals("Nachricht versenden", view.events().getFirst().label());

        var defaults = view.data();
        assertEquals("Entwurf für Ada", defaults.getLiteral("subject"));
        assertEquals("Hallo Ada", defaults.getLiteral("body"));

        var update = authored("subject", "  Bearbeitet  ", "body", "  Finaler Inhalt  ");
        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.onEventFromStaffTaskView(context, update, "send").orElseThrow()
        );
        assertEquals("Bearbeitet", result.getCommunicationRequest().message().subject());
        assertEquals("Finaler Inhalt", result.getCommunicationRequest().message().body());
        assertSame(signatureDepartment, result.getCommunicationRequest().message().signatureDepartment());
        var events = ArgumentCaptor.forClass(ProcessInstanceEventEntity.class);
        verify(repository, times(2)).save(events.capture());
        var requestEvent = events.getAllValues().stream().filter(event -> event.getTitle().equals("Versand angefordert"))
                .findFirst().orElseThrow();
        assertEquals("Der Versand der Nachricht mit dem Betreff „Bearbeitet“ wurde durch „Ada Beispiel“ angefordert.", requestEvent.getMessage());
        assertEquals("staff-1", requestEvent.getConcernedUserId());
        assertTrue(requestEvent.getHistoryRelevant());
        assertTrue(requestEvent.getAudit());
        assertFalse(requestEvent.getTechnical());
    }

    @Test
    void staffTaskRejectsBlankValuesAndUnknownEvents() {
        var node = createNode(mock(AssignmentContextAssigneeResolverService.class));
        var context = staffContext(configuration("manual"), new ProcessExecutionData(), processInstance(), task());

        var validationException = assertThrows(
                ResponseException.class,
                () -> node.onEventFromStaffTaskView(
                        context,
                        authored("subject", " ", "body", ""),
                        "send"
                )
        );
        var validation = assertInstanceOf(DerivedRuntimeElementData.class, validationException.getDetails());
        assertEquals("Geben Sie einen Betreff für die Nachricht ein.", validation.getElementStates().get("subject").getError());
        assertEquals("Geben Sie eine Nachricht ein.", validation.getElementStates().get("body").getError());
        var actionException = assertThrows(
                ProcessNodeExecutionExceptionUnknown.class,
                () -> node.onEventFromStaffTaskView(context, authored(), "unknown 100%")
        );
        var expectedMessage = "Die ausgewählte Aktion ist für diese Aufgabe nicht verfügbar. Aktion: „unknown 100%“.";
        assertEquals(expectedMessage, actionException.getMessage());
        var controllerException = node.unavailableStaffTaskEvent("unknown 100%", authored());
        assertEquals(HttpStatus.BAD_REQUEST, controllerException.getStatus());
        assertEquals(expectedMessage, controllerException.getTitle());
        assertNull(controllerException.getDetails());
    }

    @Test
    void missingIdentityAndAutomaticMessageHaveReadableConfigurationErrors() {
        var node = createNode(mock(AssignmentContextAssigneeResolverService.class));
        var configuration = configuration("automatic");
        configuration.identityId = " ";
        var identityException = assertThrows(ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> node.init(initContext(configuration, new ProcessExecutionData(), processInstance(), task())));
        assertEquals("Wählen Sie eine empfangende Identität aus.", identityException.getMessage());

        configuration.identityId = "applicant";
        configuration.messageConfig.automaticContent.subject = null;
        var contentException = assertThrows(ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> node.init(initContext(configuration, new ProcessExecutionData(), processInstance(), task())));
        assertEquals("Hinterlegen Sie einen Betreff und eine Nachricht für den automatischen Versand.", contentException.getMessage());
    }

    @Test
    void selectedIdentityMissingFromTheInstanceIsNamedInTheError() {
        var node = createNode(mock(AssignmentContextAssigneeResolverService.class));
        var configuration = configuration("automatic");
        configuration.identityId = "missing-100%";

        var exception = assertThrows(ProcessNodeExecutionExceptionMissingValue.class,
                () -> node.init(initContext(configuration, new ProcessExecutionData(), processInstance(), task())));

        assertEquals("Die ausgewählte Identität „missing-100%“ ist im Vorgang nicht vorhanden.", exception.getMessage());
    }

    @Test
    void missingManualDefaultsHaveASafeStaffViewErrorWithTheOriginalConfigurationCause() {
        var node = createNode(mock(AssignmentContextAssigneeResolverService.class));
        var configuration = configuration("manual");
        configuration.messageConfig.manualContent.subject = null;
        var context = staffContext(configuration, new ProcessExecutionData(), processInstance(), task());

        var exception = assertThrows(ResponseException.class, () -> node.getStaffTaskView(context));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        assertEquals("Hinterlegen Sie einen vorbelegten Betreff und eine vorbelegte Nachricht für die manuelle Prüfung.", exception.getTitle());
        assertNull(exception.getDetails());
        assertInstanceOf(ProcessNodeExecutionExceptionInvalidConfiguration.class, exception.getCause());
        assertEquals(exception.getTitle(), exception.getCause().getMessage());
    }

    @Test
    void attachmentReadFailureKeepsItsUnknownExceptionTypeAndCauseWithoutExposingStorageDetails() throws Exception {
        var attachmentSets = mock(ProcessInstanceAttachmentSetService.class);
        var attachments = mock(ProcessInstanceAttachmentService.class);
        var storage = mock(StorageService.class);
        var node = new CommunicationMessageActionNodeV1(attachmentSets, attachments, storage,
                mock(AssignmentContextAssigneeResolverService.class), mock(VDepartmentShadowedService.class));
        var configuration = configuration("automatic");
        configuration.attachmentSetDataKeys = List.of("documents");
        when(attachmentSets.findAllByProcessInstanceIdAndDataKey(PROCESS_INSTANCE_ID, "documents"))
                .thenReturn(List.of(new ProcessInstanceAttachmentSetEntity().setId(11)));
        when(attachments.findAllByAttachmentSetId(11)).thenReturn(List.of(
                new ProcessInstanceAttachmentEntity().setFileName("notice-100%.pdf")
                        .setStorageProviderId(12).setStoragePathFromRoot("internal-path")
        ));
        var cause = new IOException("internal storage location 100%");
        when(storage.getDocumentContent(12, "internal-path")).thenReturn(new InputStream() {
            @Override
            public int read() throws IOException {
                throw cause;
            }
        });

        var exception = assertThrows(ProcessNodeExecutionExceptionUnknown.class,
                () -> node.init(initContext(configuration, new ProcessExecutionData(), processInstance(), task())));

        assertEquals("Die Anlage „notice-100%.pdf“ konnte für den Nachrichtenversand nicht geladen werden.", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void invalidExecutionTypeFailsAndExportRemovesSystemReferences() {
        var node = createNode(mock(AssignmentContextAssigneeResolverService.class));
        var invalidConfiguration = configuration("unexpected");

        var exception = assertThrows(
                ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> node.init(initContext(
                        invalidConfiguration,
                        new ProcessExecutionData(),
                        processInstance(),
                        task()
                ))
        );
        assertEquals("Wählen Sie, ob die Nachricht automatisch oder nach manueller Prüfung versendet werden soll.", exception.getMessage());

        var exported = authored(
                CommunicationMessageActionNodeV1.Configuration.IDENTITY_ID_FIELD_ID,
                "applicant",
                SemiAutomaticMessageConfig.ManualContent.ASSIGNMENT_FIELD_ID,
                Map.of("user", "staff-1"),
                SemiAutomaticMessageConfig.LayoutConfig.SIGNATURE_DEPARTMENT_FIELD_ID_1,
                17,
                SemiAutomaticMessageConfig.LayoutConfig.SIGNATURE_DEPARTMENT_FIELD_ID_2,
                29,
                "portableValue",
                "kept"
        );
        var cleaned = node.cleanConfigurationForExport(exported);
        assertFalse(cleaned.containsKey(CommunicationMessageActionNodeV1.Configuration.IDENTITY_ID_FIELD_ID));
        assertFalse(cleaned.containsKey(SemiAutomaticMessageConfig.ManualContent.ASSIGNMENT_FIELD_ID));
        assertFalse(cleaned.containsKey(SemiAutomaticMessageConfig.LayoutConfig.SIGNATURE_DEPARTMENT_FIELD_ID_1));
        assertFalse(cleaned.containsKey(SemiAutomaticMessageConfig.LayoutConfig.SIGNATURE_DEPARTMENT_FIELD_ID_2));
        assertEquals("kept", cleaned.getLiteral("portableValue"));
    }

    private static CommunicationMessageActionNodeV1.Configuration configuration(String executionType) {
        var configuration = new CommunicationMessageActionNodeV1.Configuration();
        configuration.identityId = "applicant";
        configuration.messageConfig = new SemiAutomaticMessageConfig.LayoutConfig();
        configuration.messageConfig.executionType = executionType;
        configuration.messageConfig.automaticContent = new SemiAutomaticMessageConfig.AutomaticContent();
        configuration.messageConfig.automaticContent.subject = "Subject 123";
        configuration.messageConfig.automaticContent.content = "Hello";
        configuration.messageConfig.manualContent = new SemiAutomaticMessageConfig.ManualContent();
        configuration.messageConfig.manualContent.subject = "Entwurf für Ada";
        configuration.messageConfig.manualContent.content = "Hallo Ada";
        configuration.messageConfig.manualContent.assignmentContext = new AssignmentContextInputElementValue();
        return configuration;
    }

    private static ProcessInstanceEntity processInstance() {
        var identity = new IdentityData(
                "session",
                "applicant",
                IdentityType.IdentityProvider,
                UUID.randomUUID(),
                "metadata",
                "provider-user-123",
                null,
                Map.of(),
                5,
                Map.of()
        );
        var identities = new IdentityDataMap();
        identities.put("applicant", identity);

        var processInstance = mock(ProcessInstanceEntity.class);
        when(processInstance.getId()).thenReturn(PROCESS_INSTANCE_ID);
        when(processInstance.getIdentities()).thenReturn(identities);
        return processInstance;
    }

    private static ProcessNodeEntity processNode() {
        var node = mock(ProcessNodeEntity.class);
        when(node.getProcessId()).thenReturn(PROCESS_ID);
        when(node.getProcessVersion()).thenReturn(PROCESS_VERSION);
        when(node.getId()).thenReturn(NODE_ID);
        return node;
    }

    private static ProcessInstanceTaskEntity task() {
        var task = mock(ProcessInstanceTaskEntity.class);
        when(task.getId()).thenReturn(TASK_ID);
        when(task.getRuntimeData()).thenReturn(Map.of());
        return task;
    }

    private static ProcessNodeExecutionInitContext<CommunicationMessageActionNodeV1.Configuration> initContext(
            CommunicationMessageActionNodeV1.Configuration configuration,
            ProcessExecutionData executionData,
            ProcessInstanceEntity processInstance,
            ProcessInstanceTaskEntity task
    ) {
        return initContext(configuration, executionData, processInstance, task, processNode());
    }

    @SuppressWarnings("unchecked")
    private static ProcessNodeExecutionInitContext<CommunicationMessageActionNodeV1.Configuration> initContext(
            CommunicationMessageActionNodeV1.Configuration configuration,
            ProcessExecutionData executionData,
            ProcessInstanceEntity processInstance,
            ProcessInstanceTaskEntity task,
            ProcessNodeEntity processNode
    ) {
        var context = (ProcessNodeExecutionInitContext<CommunicationMessageActionNodeV1.Configuration>)
                mock(ProcessNodeExecutionInitContext.class);
        when(context.getConfigurationOfExecutingNode()).thenReturn(configuration);
        when(context.getCurrentProcessExecutionData()).thenReturn(executionData);
        when(context.getThisProcessInstance()).thenReturn(processInstance);
        when(context.getThisTask()).thenReturn(task);
        when(context.getLogger()).thenReturn(mock(ProcessNodeExecutionLogger.class));
        when(context.getThisNode()).thenReturn(processNode);
        return context;
    }

    @SuppressWarnings("unchecked")
    private static ProcessNodeExecutionContextUIStaff<CommunicationMessageActionNodeV1.Configuration> staffContext(
            CommunicationMessageActionNodeV1.Configuration configuration,
            ProcessExecutionData executionData,
            ProcessInstanceEntity processInstance,
            ProcessInstanceTaskEntity task
    ) {
        var context = (ProcessNodeExecutionContextUIStaff<CommunicationMessageActionNodeV1.Configuration>)
                mock(ProcessNodeExecutionContextUIStaff.class);
        when(context.getCallingUser()).thenReturn(new UserEntity().setId("staff-1").setFullName("Ada Beispiel"));
        when(context.getConfigurationOfExecutingNode()).thenReturn(configuration);
        when(context.getCurrentProcessExecutionData()).thenReturn(executionData);
        when(context.getThisProcessInstance()).thenReturn(processInstance);
        when(context.getThisTask()).thenReturn(task);
        when(context.getLogger()).thenReturn(mock(ProcessNodeExecutionLogger.class));
        return context;
    }

    private static AuthoredElementValues authored(Object... entries) {
        var values = new AuthoredElementValues();
        for (var index = 0; index < entries.length; index += 2) {
            values.putLiteral((String) entries[index], entries[index + 1]);
        }
        return values;
    }

    private static CommunicationMessageActionNodeV1 createNode(
            AssignmentContextAssigneeResolverService assignmentResolver
    ) {
        return createNode(assignmentResolver, mock(VDepartmentShadowedService.class));
    }

    private static CommunicationMessageActionNodeV1 createNode(
            AssignmentContextAssigneeResolverService assignmentResolver,
            VDepartmentShadowedService departmentService
    ) {
        return new CommunicationMessageActionNodeV1(
                mock(ProcessInstanceAttachmentSetService.class),
                mock(ProcessInstanceAttachmentService.class),
                mock(StorageService.class),
                assignmentResolver,
                departmentService
        );
    }
}
