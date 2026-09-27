package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.form.content.HeadlineContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.AssignmentContextInputElementValue;
import de.aivot.prosuna.backend.elements.models.elements.form.input.AssignmentContextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.DomainAndUserSelectInputElementValue;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.process.dtos.ProcessAssignmentOptionDTO;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeConfigurationValidationPhase;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidAssignment;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultInstanceAssigned;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskAssigned;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeConfigurationValidationContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUIStaff;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.services.AssignmentContextAssigneeResolverService;
import de.aivot.prosuna.backend.process.services.ProcessAssignmentService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InstanceAssignmentActionNodeV1Test {
    private final AssignmentContextAssigneeResolverService resolver = mock(AssignmentContextAssigneeResolverService.class);
    private final ProcessAssignmentService assignments = mock(ProcessAssignmentService.class);
    private final InstanceAssignmentActionNodeV1 node = new InstanceAssignmentActionNodeV1(resolver, assignments);

    @BeforeEach
    void setUp() throws Exception {
        when(assignments.runtimeInstanceOptions(eq(99L), anyList()))
                .thenReturn(List.of(option("recipient"), option("dispatcher")));
        when(resolver.resolveAssignee(anyInt(), anyInt(), anyLong(), anyInt(), anyLong(),
                nullable(Integer.class), nullable(String.class), any(AssignmentContextInputElementValue.class),
                anyList(), anySet())).thenReturn(Optional.of("recipient"));
    }

    @Test
    void automaticModeAssignsEligiblePersonAndExposesOutput() throws Exception {
        var result = assertInstanceOf(ProcessNodeExecutionResultInstanceAssigned.class,
                node.init(initContext(config("automatic"))));

        assertEquals("recipient", result.getAssignedUserId());
        assertEquals("success", result.getViaPort());
        assertEquals(Map.of("assignedUserId", "recipient"), result.getNodeData());
        assertEquals(Map.of("case", "data"), result.getProcessData());
        verify(assignments).runtimeInstanceOptions(99L, List.of());
        verify(resolver).resolveAssignee(eq(42), eq(3), eq(99L), eq(12), eq(17L),
                isNull(), eq("previous"), any(AssignmentContextInputElementValue.class),
                eq(List.of("process_instance.read")), eq(Set.of("recipient", "dispatcher")));
    }

    @Test
    void manualModeAssignsTaskToAuthorizedDispatcher() throws Exception {
        when(resolver.resolveAssignee(anyInt(), anyInt(), anyLong(), anyInt(), anyLong(),
                nullable(Integer.class), nullable(String.class), any(AssignmentContextInputElementValue.class),
                anyList(), anySet())).thenReturn(Optional.of("dispatcher"));

        var result = assertInstanceOf(ProcessNodeExecutionResultTaskAssigned.class,
                node.init(initContext(config("manual"))));

        assertEquals("dispatcher", result.getAssignedUserId());
        assertEquals(Map.of("case", "data"), result.getProcessData());
        verify(assignments).runtimeInstanceOptions(99L,
                List.of("process_instance.read", "process_instance.edit_task", "process_instance.reassign"));
        verify(resolver).resolveAssignee(anyInt(), anyInt(), anyLong(), anyInt(), anyLong(),
                nullable(Integer.class), nullable(String.class), any(AssignmentContextInputElementValue.class),
                eq(List.of("process_instance.read", "process_instance.edit_task", "process_instance.reassign")),
                anySet());
    }

    @Test
    void automaticModeFailsWhenNoCandidateCanBeResolved() throws Exception {
        when(resolver.resolveAssignee(anyInt(), anyInt(), anyLong(), anyInt(), anyLong(),
                nullable(Integer.class), nullable(String.class), any(AssignmentContextInputElementValue.class),
                anyList(), anySet())).thenReturn(Optional.empty());

        assertThrows(ProcessNodeExecutionExceptionInvalidAssignment.class,
                () -> node.init(initContext(config("automatic"))));
    }

    @Test
    void manualTaskUsesCurrentInstanceOptionsWithoutPreselection() throws Exception {
        when(assignments.instanceOptions("dispatcher", 99L))
                .thenReturn(List.of(option("recipient"), option("previous")));
        var config = config("manual");
        config.taskDescription = "<p>Wählen Sie die zuständige Person für diesen Vorgang aus.</p>";

        var view = node.getStaffTaskView(staffContext(config));
        var layout = (GroupLayoutElement) view.layout();
        var field = layout
                .findChild("assignedUserId", SelectInputElement.class).orElseThrow();

        assertEquals("Aufgabenbeschreibung", assertInstanceOf(HeadlineContentElement.class,
                layout.getChildren().get(0)).getContent());
        assertEquals(config.taskDescription, assertInstanceOf(RichTextContentElement.class,
                layout.getChildren().get(1)).getContent());
        assertSame(field, layout.getChildren().get(2));
        assertTrue(field.getRequired());
        assertEquals(List.of("recipient", "previous"), field.getOptions().stream().map(o -> o.getValue()).toList());
        assertNull(view.data().getLiteral("assignedUserId"));
        assertEquals("assign", view.events().getFirst().event());
    }

    @Test
    void manualTaskKeepsAssignmentOpenWhenNoRecipientIsAvailable() throws Exception {
        when(assignments.instanceOptions("dispatcher", 99L)).thenReturn(List.of());

        var view = node.getStaffTaskView(staffContext(config("manual")));
        var layout = (GroupLayoutElement) view.layout();
        var field = layout
                .findChild("assignedUserId", SelectInputElement.class).orElseThrow();

        assertEquals(List.of(field), layout.getChildren());
        assertTrue(field.getDisabled());
        assertTrue(view.events().isEmpty());
    }

    @Test
    void manualTaskOmitsBlankDescription() throws Exception {
        var config = config("manual");
        config.taskDescription = "  \n  ";

        var layout = (GroupLayoutElement) node.getStaffTaskView(staffContext(config)).layout();

        assertEquals(1, layout.getChildren().size());
        assertInstanceOf(SelectInputElement.class, layout.getChildren().getFirst());
    }

    @Test
    void manualSubmissionRechecksSelectionAgainstCurrentOptions() throws Exception {
        when(assignments.instanceOptions("dispatcher", 99L)).thenReturn(List.of(option("recipient")));
        var context = staffContext(config("manual"));

        assertThrows(ResponseException.class, () -> node.onEventFromStaffTaskView(
                context, new AuthoredElementValues(), "assign"));
        assertThrows(ResponseException.class, () -> node.onEventFromStaffTaskView(
                context, new AuthoredElementValues().putLiteral("assignedUserId", "unauthorized"), "assign"));

        var result = assertInstanceOf(ProcessNodeExecutionResultInstanceAssigned.class,
                node.onEventFromStaffTaskView(context,
                        new AuthoredElementValues().putLiteral("assignedUserId", "recipient"), "assign")
                        .orElseThrow());
        assertEquals("recipient", result.getAssignedUserId());
        assertEquals("success", result.getViaPort());
        assertEquals(Map.of("assignedUserId", "recipient"), result.getNodeData());
    }

    @Test
    void manualTaskRejectsOtherCallerEvenWithAssignmentOptionsAccess() throws Exception {
        var context = new ProcessNodeExecutionContextUIStaff<>(mock(ProcessNodeExecutionLogger.class),
                processNode(), instance(), task(), null, new UserEntity().setId("other"), config("manual"),
                new ProcessExecutionData());

        assertThrows(ResponseException.class, () -> node.getStaffTaskView(context));
        assertThrows(ResponseException.class, () -> node.onEventFromStaffTaskView(context,
                new AuthoredElementValues().putLiteral("assignedUserId", "recipient"), "assign"));
        verify(assignments, never()).instanceOptions("other", 99L);
    }

    @Test
    void configurationRequiresOnlyTheSelectedModeContext() throws Exception {
        var layout = node.getConfigurationLayout(new ProcessNodeDefinitionConfigurationLayoutContext(
                null, new ProcessEntity().setId(42), new ProcessVersionEntity().setProcessVersion(3), processNode()));
        assertTrue(layout.findChild(InstanceAssignmentActionNodeV1.Config.MODE_FIELD_ID).isPresent());
        var automaticField = layout.findChild(InstanceAssignmentActionNodeV1.Config.AUTOMATIC_CONTEXT_FIELD_ID,
                AssignmentContextInputElement.class).orElseThrow();
        var manualField = layout.findChild(InstanceAssignmentActionNodeV1.Config.MANUAL_CONTEXT_FIELD_ID,
                AssignmentContextInputElement.class).orElseThrow();
        var descriptionField = layout.findChild(InstanceAssignmentActionNodeV1.Config.TASK_DESCRIPTION_FIELD_ID,
                RichTextInputElement.class).orElseThrow();
        assertNotNull(automaticField.getVisibility());
        assertNotNull(manualField.getVisibility());
        var descriptionVisibility = assertInstanceOf(NoCodeExpression.class,
                descriptionField.getVisibility().getNoCode());
        var descriptionVisibilityOperands = descriptionVisibility.getOperands().stream().toList();
        assertEquals(InstanceAssignmentActionNodeV1.Config.MODE_FIELD_ID,
                assertInstanceOf(NoCodeReference.class, descriptionVisibilityOperands.getFirst()).getElementId());
        assertEquals("manual", assertInstanceOf(NoCodeStaticValue.class,
                descriptionVisibilityOperands.get(1)).getValue());
        assertTrue(automaticField.getDisableProcessInstanceAssigneeOption());
        assertTrue(automaticField.getDisableAssignmentContextRepeatExecutionAssigneePreferenceOptions());
        assertEquals(List.of("process_instance.read"),
                automaticField.getProcessAccessConstraint().getRequiredPermissions());
        assertEquals(List.of("process_instance.read", "process_instance.edit_task", "process_instance.reassign"),
                manualField.getProcessAccessConstraint().getRequiredPermissions());
        assertEquals("automatic", node.getInitialConfiguration().getLiteral("mode"));

        var automatic = config("automatic");
        automatic.automaticAssignmentContext = null;
        var automaticErrors = node.validateConfiguration(validationContext(automatic));
        assertTrue(automaticErrors.containsKey(InstanceAssignmentActionNodeV1.Config.AUTOMATIC_CONTEXT_FIELD_ID));

        var manual = config("manual");
        manual.manualAssignmentContext = null;
        var manualErrors = node.validateConfiguration(validationContext(manual));
        assertTrue(manualErrors.containsKey(InstanceAssignmentActionNodeV1.Config.MANUAL_CONTEXT_FIELD_ID));

        var valid = config("manual");
        valid.automaticAssignmentContext = null;
        assertNull(node.validateConfiguration(validationContext(valid)));

        var exported = node.cleanConfigurationForExport(new AuthoredElementValues()
                .putLiteral("mode", "manual")
                .putLiteral(InstanceAssignmentActionNodeV1.Config.AUTOMATIC_CONTEXT_FIELD_ID, "user:recipient")
                .putLiteral(InstanceAssignmentActionNodeV1.Config.MANUAL_CONTEXT_FIELD_ID, "user:dispatcher"));
        assertEquals(Set.of("mode"), exported.keySet());
    }

    @Test
    void restrictedPreferencesFailConfigurationValidationAndExecution() {
        var automatic = config("automatic");
        automatic.automaticAssignmentContext.setGeneralAssigneePreference(
                AssignmentContextInputElementValue.GENERAL_ASSIGNEE_PREFERENCE_PROCESS_INSTANCE_ASSIGNEE);
        assertEquals(List.of("Die Bevorzugung der dem Vorgang zugewiesenen Person ist hier nicht zulässig."),
                node.validateConfiguration(validationContext(automatic))
                        .get(InstanceAssignmentActionNodeV1.Config.AUTOMATIC_CONTEXT_FIELD_ID));
        assertThrows(ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> node.init(initContext(automatic)));

        var manual = config("manual");
        manual.manualAssignmentContext.setRepeatExecutionAssigneePreference(
                AssignmentContextInputElementValue.REPEAT_EXECUTION_ASSIGNEE_PREFERENCE_PREVIOUS_ITERATION_ASSIGNEE);
        assertEquals(List.of("Eine Bevorzugung bei erneuter Ausführung ist hier nicht zulässig."),
                node.validateConfiguration(validationContext(manual))
                        .get(InstanceAssignmentActionNodeV1.Config.MANUAL_CONTEXT_FIELD_ID));
        assertThrows(ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> node.init(initContext(manual)));

        automatic.mode = "manual";
        automatic.manualAssignmentContext = new AssignmentContextInputElementValue()
                .setDomainAndUserSelection(List.of(new DomainAndUserSelectInputElementValue("user", "dispatcher")));
        assertNull(node.validateConfiguration(validationContext(automatic)));
    }

    private static ProcessNodeConfigurationValidationContext<InstanceAssignmentActionNodeV1.Config> validationContext(
            InstanceAssignmentActionNodeV1.Config config) {
        return new ProcessNodeConfigurationValidationContext<>(processNode(), config,
                new DerivedRuntimeElementData(), ProcessNodeConfigurationValidationPhase.Authoring);
    }

    private static ProcessNodeExecutionInitContext<InstanceAssignmentActionNodeV1.Config> initContext(
            InstanceAssignmentActionNodeV1.Config config) {
        return new ProcessNodeExecutionInitContext<>(mock(ProcessNodeExecutionLogger.class), processNode(),
                instance(), task(), null, new ProcessExecutionData().addProcessData(Map.of("case", "data")), config);
    }

    private static ProcessNodeExecutionContextUIStaff<InstanceAssignmentActionNodeV1.Config> staffContext(
            InstanceAssignmentActionNodeV1.Config config) {
        return new ProcessNodeExecutionContextUIStaff<>(mock(ProcessNodeExecutionLogger.class), processNode(),
                instance(), task(), null, new UserEntity().setId("dispatcher"), config,
                new ProcessExecutionData().addProcessData(Map.of("case", "data")));
    }

    private static ProcessNodeEntity processNode() {
        return new ProcessNodeEntity().setId(12).setProcessId(42).setProcessVersion(3);
    }

    private static ProcessInstanceEntity instance() {
        return new ProcessInstanceEntity().setId(99L).setAssignedUserId("previous");
    }

    private static ProcessInstanceTaskEntity task() {
        return new ProcessInstanceTaskEntity().setId(17L).setProcessData(Map.of("case", "data"))
                .setRuntimeData(Map.of()).setAssignedUserId("dispatcher");
    }

    private static InstanceAssignmentActionNodeV1.Config config(String mode) {
        var config = new InstanceAssignmentActionNodeV1.Config();
        config.mode = mode;
        var circle = new AssignmentContextInputElementValue()
                .setDomainAndUserSelection(List.of(new DomainAndUserSelectInputElementValue("user", "recipient")));
        config.automaticAssignmentContext = circle;
        config.manualAssignmentContext = circle;
        return config;
    }

    private static ProcessAssignmentOptionDTO option(String id) {
        return new ProcessAssignmentOptionDTO(id, id, null);
    }
}
