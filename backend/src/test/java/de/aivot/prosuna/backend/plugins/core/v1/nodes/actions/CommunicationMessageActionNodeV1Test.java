package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.EffectiveElementValues;
import de.aivot.prosuna.backend.elements.models.elements.form.input.AssignmentContextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.AssignmentContextInputElementValue;
import de.aivot.prosuna.backend.elements.models.elements.form.input.ProcessIdentityIdInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RadioInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RadioInputElementOption;
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
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskAssigned;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUIStaff;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.services.AssignmentContextAssigneeResolverService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentSetService;
import de.aivot.prosuna.backend.process.services.TemplateRenderService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunicationMessageActionNodeV1Test {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final Integer NODE_ID = 17;
    private static final Long PROCESS_INSTANCE_ID = 99L;
    private static final Long TASK_ID = 123L;

    @Test
    void metadataExposesAutomaticAndSemiAutomaticExecutionAndTypedOutputs() {
        var node = createNode(mock(TemplateRenderService.class), mock(AssignmentContextAssigneeResolverService.class));

        assertArrayEquals(
                new ProcessNodeExecutionType[]{
                        ProcessNodeExecutionType.Automatic,
                        ProcessNodeExecutionType.SemiAutomatic
                },
                node.getExecutionTypes()
        );
        assertFalse(node.getAbstract().isBlank());
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
    }

    @Test
    void configurationUsesSharedSemiAutomaticMessageLayout() throws Exception {
        var node = createNode(mock(TemplateRenderService.class), mock(AssignmentContextAssigneeResolverService.class));
        var processNode = mock(ProcessNodeEntity.class);
        when(processNode.getProcessId()).thenReturn(PROCESS_ID);
        when(processNode.getProcessVersion()).thenReturn(PROCESS_VERSION);

        var layout = node.getConfigurationLayout(new ProcessNodeDefinitionConfigurationLayoutContext(
                null,
                mock(ProcessEntity.class),
                mock(ProcessVersionEntity.class),
                processNode
        ));

        assertTrue(layout.findChild(
                CommunicationMessageActionNodeV1.Configuration.IDENTITY_ID_FIELD_ID,
                ProcessIdentityIdInputElement.class
        ).isPresent());
        assertTrue(layout.findChild(
                SemiAutomaticMessageConfig.GROUP_ID,
                GroupLayoutElement.class
        ).isPresent());

        var executionType = layout.findChild(
                SemiAutomaticMessageConfig.LayoutConfig.EXECUTION_TYPE_FIELD_ID,
                RadioInputElement.class
        ).orElseThrow();
        assertEquals(List.of(
                RadioInputElementOption.of("automatic", "Automatisch versenden"),
                RadioInputElementOption.of("manual", "Manuell bearbeiten und versenden")
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
        assertEquals("Subject", configuration.messageConfig.automaticContent.subject);
        assertEquals("Content", configuration.messageConfig.automaticContent.content);
    }

    @Test
    void initAutomaticReturnsCommunicationRequestForConfiguredIdentity() throws Exception {
        var templateRenderService = mock(TemplateRenderService.class);
        var node = createNode(templateRenderService, mock(AssignmentContextAssigneeResolverService.class));
        var configuration = configuration("automatic");
        var processInstance = processInstance();
        var executionData = new ProcessExecutionData().addProcessData(Map.of("caseNumber", "123"));
        var context = initContext(configuration, executionData, processInstance, mock(ProcessInstanceTaskEntity.class));

        when(templateRenderService.interpolate(
                same(executionData),
                eq(configuration.messageConfig.automaticContent.subject)
        )).thenReturn("  Subject 123  ");
        when(templateRenderService.interpolate(
                same(executionData),
                eq(configuration.messageConfig.automaticContent.content)
        )).thenReturn("  Hello  ");

        var result = assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, node.init(context));

        var communicationRequest = result.getCommunicationRequest();
        assertNotNull(communicationRequest);
        assertEquals("applicant", communicationRequest.recipientIdentityId());
        assertEquals("sendResult", communicationRequest.nodeDataOutputKey());
        assertEquals("Subject 123", communicationRequest.message().subject());
        assertEquals("Hello", communicationRequest.message().body());
        assertEquals("Hello", communicationRequest.message().htmlBody());
        assertEquals(result.getNodeData().get("sentAt"), communicationRequest.message().timestamp());
        assertEquals(5, result.getNodeData().get("communicationProviderBindingId"));
    }

    @Test
    void initManualAssignsStaffWithoutSending() throws Exception {
        var assignmentResolver = mock(AssignmentContextAssigneeResolverService.class);
        var node = createNode(mock(TemplateRenderService.class), assignmentResolver);
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

        assertEquals("staff-1", result.getAssignedUserId());
        assertEquals(Map.of("caseNumber", "123"), result.getProcessData());
        assertEquals(Map.of(), result.getRuntimeData());
        verify(assignmentResolver).resolveAssignee(
                any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void staffTaskProvidesRenderedDefaultsAndSendsEditedValues() throws Exception {
        var templateRenderService = mock(TemplateRenderService.class);
        var node = createNode(templateRenderService, mock(AssignmentContextAssigneeResolverService.class));
        var configuration = configuration("manual");
        var executionData = new ProcessExecutionData().addProcessData(Map.of("name", "Ada"));
        var context = staffContext(configuration, executionData, processInstance(), task());

        when(templateRenderService.interpolate(
                same(executionData),
                eq(configuration.messageConfig.manualContent.subject)
        )).thenReturn("Entwurf für Ada");
        when(templateRenderService.interpolate(
                same(executionData),
                eq(configuration.messageConfig.manualContent.content)
        )).thenReturn("Hallo Ada");

        var view = node.getStaffTaskView(context);
        var layout = (GroupLayoutElement) view.layout();
        assertTrue(Boolean.TRUE.equals(layout.findChild("subject", TextInputElement.class).orElseThrow().getRequired()));
        assertTrue(Boolean.TRUE.equals(layout.findChild("body", RichTextInputElement.class).orElseThrow().getRequired()));
        assertEquals("send", view.events().getFirst().event());

        var defaults = view.data();
        assertEquals("Entwurf für Ada", defaults.get("subject"));
        assertEquals("Hallo Ada", defaults.get("body"));

        var update = authored("subject", "  Bearbeitet  ", "body", "  Finaler Inhalt  ");
        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.onEventFromStaffTaskView(context, update, "send").orElseThrow()
        );
        assertEquals("Bearbeitet", result.getCommunicationRequest().message().subject());
        assertEquals("Finaler Inhalt", result.getCommunicationRequest().message().body());
    }

    @Test
    void staffTaskRejectsBlankValuesAndUnknownEvents() {
        var node = createNode(mock(TemplateRenderService.class), mock(AssignmentContextAssigneeResolverService.class));
        var context = staffContext(configuration("manual"), new ProcessExecutionData(), processInstance(), task());

        assertThrows(
                ResponseException.class,
                () -> node.onEventFromStaffTaskView(
                        context,
                        authored("subject", " ", "body", ""),
                        "send"
                )
        );
        assertThrows(
                ProcessNodeExecutionExceptionUnknown.class,
                () -> node.onEventFromStaffTaskView(context, authored(), "unknown")
        );
    }

    @Test
    void invalidExecutionTypeFailsAndExportRemovesAssignment() {
        var node = createNode(mock(TemplateRenderService.class), mock(AssignmentContextAssigneeResolverService.class));
        var invalidConfiguration = configuration("unexpected");

        assertThrows(
                ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> node.init(initContext(
                        invalidConfiguration,
                        new ProcessExecutionData(),
                        processInstance(),
                        task()
                ))
        );

        var exported = authored(
                CommunicationMessageActionNodeV1.Configuration.IDENTITY_ID_FIELD_ID,
                "applicant",
                SemiAutomaticMessageConfig.ManualContent.ASSIGNMENT_FIELD_ID,
                Map.of("user", "staff-1")
        );
        var cleaned = node.cleanConfigurationForExport(exported);
        assertEquals("applicant", cleaned.get(CommunicationMessageActionNodeV1.Configuration.IDENTITY_ID_FIELD_ID));
        assertFalse(cleaned.containsKey(SemiAutomaticMessageConfig.ManualContent.ASSIGNMENT_FIELD_ID));
    }

    private static CommunicationMessageActionNodeV1.Configuration configuration(String executionType) {
        var configuration = new CommunicationMessageActionNodeV1.Configuration();
        configuration.identityId = "applicant";
        configuration.messageConfig = new SemiAutomaticMessageConfig.LayoutConfig();
        configuration.messageConfig.executionType = executionType;
        configuration.messageConfig.automaticContent = new SemiAutomaticMessageConfig.AutomaticContent();
        configuration.messageConfig.automaticContent.subject = "Subject {{ $.caseNumber }}";
        configuration.messageConfig.automaticContent.content = "Hello";
        configuration.messageConfig.manualContent = new SemiAutomaticMessageConfig.ManualContent();
        configuration.messageConfig.manualContent.subject = "Entwurf für {{ $.name }}";
        configuration.messageConfig.manualContent.content = "Hallo {{ $.name }}";
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
        when(context.getConfigurationOfExecutingNode()).thenReturn(configuration);
        when(context.getCurrentProcessExecutionData()).thenReturn(executionData);
        when(context.getThisProcessInstance()).thenReturn(processInstance);
        when(context.getThisTask()).thenReturn(task);
        return context;
    }

    private static AuthoredElementValues authored(Object... entries) {
        var values = new AuthoredElementValues();
        for (var index = 0; index < entries.length; index += 2) {
            values.put((String) entries[index], entries[index + 1]);
        }
        return values;
    }

    private static CommunicationMessageActionNodeV1 createNode(
            TemplateRenderService templateRenderService,
            AssignmentContextAssigneeResolverService assignmentResolver
    ) {
        return new CommunicationMessageActionNodeV1(
                templateRenderService,
                mock(ProcessInstanceAttachmentSetService.class),
                mock(ProcessInstanceAttachmentService.class),
                mock(StorageService.class),
                assignmentResolver
        );
    }
}
