package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.form.input.AssignmentContextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.AssignmentContextInputElementValue;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RadioInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RadioInputElementOption;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.uiPresets.SemiAutomaticMessageConfig;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
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
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

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

class EMailActionNodeV1Test {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final Integer NODE_ID = 17;
    private static final Long PROCESS_INSTANCE_ID = 99L;
    private static final Long TASK_ID = 123L;

    private ProsunaConfig prosunaConfig;
    private TemplateRenderService templateRenderService;
    private JavaMailSenderImpl mailSender;
    private AssignmentContextAssigneeResolverService assignmentResolver;
    private EMailActionNodeV1 node;

    @BeforeEach
    void setUp() {
        prosunaConfig = mock(ProsunaConfig.class);
        templateRenderService = mock(TemplateRenderService.class);
        mailSender = mock(JavaMailSenderImpl.class);
        assignmentResolver = mock(AssignmentContextAssigneeResolverService.class);
        node = new EMailActionNodeV1(
                prosunaConfig,
                templateRenderService,
                mock(ProcessInstanceAttachmentService.class),
                mock(ProcessInstanceAttachmentSetService.class),
                mock(StorageService.class),
                mailSender,
                assignmentResolver
        );
    }

    @Test
    void configurationUsesSharedSemiAutomaticMessageLayout() throws Exception {
        var processNode = processNode();
        var layout = node.getConfigurationLayout(new ProcessNodeDefinitionConfigurationLayoutContext(
                null,
                mock(ProcessEntity.class),
                mock(ProcessVersionEntity.class),
                processNode
        ));

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
    void automaticModeRendersAndSendsSharedMessageContent() throws Exception {
        var configuration = configuration("automatic");
        var processData = new ProcessExecutionData().addProcessData(Map.of("name", "Ada"));
        var mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(prosunaConfig.getFromMail()).thenReturn("service@example.test");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateRenderService.interpolate(
                same(processData),
                eq(configuration.messageConfig.automaticContent.subject)
        )).thenReturn("  Nachricht für Ada  ");
        when(templateRenderService.interpolate(
                same(processData),
                eq(configuration.messageConfig.automaticContent.content)
        )).thenReturn("  Hallo **Ada**  ");
        when(templateRenderService.interpolate(same(processData), eq(configuration.to)))
                .thenReturn("customer@example.test");

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(initContext(configuration, processData, processInstance(), task()))
        );

        verify(mailSender).send(same(mimeMessage));
        assertEquals("Nachricht für Ada", mimeMessage.getSubject());
        assertEquals(
                "customer@example.test",
                mimeMessage.getRecipients(Message.RecipientType.TO)[0].toString()
        );
        assertEquals("Nachricht für Ada", result.getNodeData().get("subject"));
    }

    @Test
    void manualModeAssignsStaffWithoutSending() throws Exception {
        var configuration = configuration("manual");
        var processData = new ProcessExecutionData().addProcessData(Map.of("name", "Ada"));
        var processInstance = processInstance();
        var processNode = processNode();
        var task = task();

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

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskAssigned.class,
                node.init(initContext(configuration, processData, processInstance, task, processNode))
        );

        assertEquals("staff-1", result.getAssignedUserId());
        assertEquals(Map.of("name", "Ada"), result.getProcessData());
        assertEquals(Map.of(), result.getRuntimeData());
    }

    @Test
    void staffTaskUsesRenderedDefaultsAndSendsEditedValues() throws Exception {
        var configuration = configuration("manual");
        var processData = new ProcessExecutionData().addProcessData(Map.of("name", "Ada"));
        var context = staffContext(configuration, processData, processInstance(), task());
        var mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(prosunaConfig.getFromMail()).thenReturn("service@example.test");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateRenderService.interpolate(
                same(processData),
                eq(configuration.messageConfig.manualContent.subject)
        )).thenReturn("Entwurf für Ada");
        when(templateRenderService.interpolate(
                same(processData),
                eq(configuration.messageConfig.manualContent.content)
        )).thenReturn("Hallo Ada");
        when(templateRenderService.interpolate(same(processData), eq(configuration.to)))
                .thenReturn("customer@example.test");

        var layout = node.getStaffTaskView(context);
        assertTrue(Boolean.TRUE.equals(layout.findChild("subject", TextInputElement.class).orElseThrow().getRequired()));
        assertTrue(Boolean.TRUE.equals(layout.findChild("body", RichTextInputElement.class).orElseThrow().getRequired()));
        assertEquals("send", node.getStaffTaskViewEvents(context).getFirst().event());

        var defaults = node.createDefaultStaffTaskViewData(context);
        assertEquals("Entwurf für Ada", defaults.get("subject"));
        assertEquals("Hallo Ada", defaults.get("body"));

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.onEventFromStaffTaskView(
                        context,
                        authored("subject", "  Finaler Betreff  ", "body", "  Finaler Inhalt  "),
                        "send"
                ).orElseThrow()
        );
        verify(mailSender).send(same(mimeMessage));
        assertEquals("Finaler Betreff", mimeMessage.getSubject());
        assertEquals("Finaler Betreff", result.getNodeData().get("subject"));
    }

    @Test
    void blankStaffMessageAndInvalidExecutionTypeAreRejected() {
        var manualContext = staffContext(
                configuration("manual"),
                new ProcessExecutionData(),
                processInstance(),
                task()
        );
        assertThrows(
                ResponseException.class,
                () -> node.onEventFromStaffTaskView(
                        manualContext,
                        authored("subject", " ", "body", ""),
                        "send"
                )
        );

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
    }

    @Test
    void exportRemovesSharedAssignment() {
        var configuration = authored(
                EMailActionNodeV1.EMailActionNodeConfig.RECIPIENT_FIELD_ID,
                "customer@example.test",
                SemiAutomaticMessageConfig.ManualContent.ASSIGNMENT_FIELD_ID,
                Map.of("user", "staff-1")
        );

        var cleaned = node.cleanConfigurationForExport(configuration);

        assertEquals(
                "customer@example.test",
                cleaned.get(EMailActionNodeV1.EMailActionNodeConfig.RECIPIENT_FIELD_ID)
        );
        assertFalse(cleaned.containsKey(SemiAutomaticMessageConfig.ManualContent.ASSIGNMENT_FIELD_ID));
    }

    private static EMailActionNodeV1.EMailActionNodeConfig configuration(String executionType) {
        var configuration = new EMailActionNodeV1.EMailActionNodeConfig();
        configuration.to = "{{ $.recipient }}";
        configuration.messageConfig = new SemiAutomaticMessageConfig.LayoutConfig();
        configuration.messageConfig.executionType = executionType;
        configuration.messageConfig.automaticContent = new SemiAutomaticMessageConfig.AutomaticContent();
        configuration.messageConfig.automaticContent.subject = "Nachricht für {{ $.name }}";
        configuration.messageConfig.automaticContent.content = "Hallo **{{ $.name }}**";
        configuration.messageConfig.manualContent = new SemiAutomaticMessageConfig.ManualContent();
        configuration.messageConfig.manualContent.subject = "Entwurf für {{ $.name }}";
        configuration.messageConfig.manualContent.content = "Hallo {{ $.name }}";
        configuration.messageConfig.manualContent.assignmentContext = new AssignmentContextInputElementValue();
        return configuration;
    }

    private static ProcessInstanceEntity processInstance() {
        var processInstance = mock(ProcessInstanceEntity.class);
        when(processInstance.getId()).thenReturn(PROCESS_INSTANCE_ID);
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

    private static ProcessNodeExecutionInitContext<EMailActionNodeV1.EMailActionNodeConfig> initContext(
            EMailActionNodeV1.EMailActionNodeConfig configuration,
            ProcessExecutionData processData,
            ProcessInstanceEntity processInstance,
            ProcessInstanceTaskEntity task
    ) {
        return initContext(configuration, processData, processInstance, task, processNode());
    }

    @SuppressWarnings("unchecked")
    private static ProcessNodeExecutionInitContext<EMailActionNodeV1.EMailActionNodeConfig> initContext(
            EMailActionNodeV1.EMailActionNodeConfig configuration,
            ProcessExecutionData processData,
            ProcessInstanceEntity processInstance,
            ProcessInstanceTaskEntity task,
            ProcessNodeEntity processNode
    ) {
        var context = (ProcessNodeExecutionInitContext<EMailActionNodeV1.EMailActionNodeConfig>)
                mock(ProcessNodeExecutionInitContext.class);
        when(context.getConfigurationOfExecutingNode()).thenReturn(configuration);
        when(context.getCurrentProcessExecutionData()).thenReturn(processData);
        when(context.getThisProcessInstance()).thenReturn(processInstance);
        when(context.getThisTask()).thenReturn(task);
        when(context.getThisNode()).thenReturn(processNode);
        return context;
    }

    @SuppressWarnings("unchecked")
    private static ProcessNodeExecutionContextUIStaff<EMailActionNodeV1.EMailActionNodeConfig> staffContext(
            EMailActionNodeV1.EMailActionNodeConfig configuration,
            ProcessExecutionData processData,
            ProcessInstanceEntity processInstance,
            ProcessInstanceTaskEntity task
    ) {
        var context = (ProcessNodeExecutionContextUIStaff<EMailActionNodeV1.EMailActionNodeConfig>)
                mock(ProcessNodeExecutionContextUIStaff.class);
        when(context.getConfigurationOfExecutingNode()).thenReturn(configuration);
        when(context.getCurrentProcessExecutionData()).thenReturn(processData);
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
}
