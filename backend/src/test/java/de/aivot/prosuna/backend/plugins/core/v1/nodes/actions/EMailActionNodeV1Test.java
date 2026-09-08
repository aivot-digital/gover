package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import de.aivot.prosuna.backend.department.entities.VDepartmentShadowedEntity;
import de.aivot.prosuna.backend.department.services.VDepartmentShadowedService;
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
import de.aivot.prosuna.backend.mail.enums.MailTemplate;
import de.aivot.prosuna.backend.mail.models.MailSendOptions;
import de.aivot.prosuna.backend.mail.services.MailService;
import de.aivot.prosuna.backend.models.lib.MailAttachmentBytes;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentSetEntity;
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
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.process.services.TemplateRenderService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import de.aivot.prosuna.backend.system.services.SystemService;
import de.aivot.prosuna.backend.theme.entities.ThemeEntity;
import de.aivot.prosuna.backend.theme.services.ThemeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import static org.mockito.Mockito.*;

class EMailActionNodeV1Test {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final Integer NODE_ID = 17;
    private static final Integer DEPARTMENT_ID = 7;
    private static final Integer THEME_ID = 8;
    private static final Long PROCESS_INSTANCE_ID = 99L;
    private static final Long TASK_ID = 123L;

    private MailService mailService;
    private TemplateRenderService templateRenderService;
    private ProcessInstanceAttachmentService processInstanceAttachmentService;
    private ProcessInstanceAttachmentSetService processInstanceAttachmentSetService;
    private StorageService storageService;
    private AssignmentContextAssigneeResolverService assignmentResolver;
    private ProcessService processService;
    private VDepartmentShadowedService vDepartmentShadowedService;
    private ThemeService themeService;
    private SystemService systemService;
    private ThemeEntity departmentTheme;
    private VDepartmentShadowedEntity department;
    private EMailActionNodeV1 node;

    @BeforeEach
    void setUp() throws Exception {
        mailService = mock(MailService.class);
        templateRenderService = mock(TemplateRenderService.class);
        processInstanceAttachmentService = mock(ProcessInstanceAttachmentService.class);
        processInstanceAttachmentSetService = mock(ProcessInstanceAttachmentSetService.class);
        storageService = mock(StorageService.class);
        assignmentResolver = mock(AssignmentContextAssigneeResolverService.class);
        processService = mock(ProcessService.class);
        vDepartmentShadowedService = mock(VDepartmentShadowedService.class);
        themeService = mock(ThemeService.class);
        systemService = mock(SystemService.class);
        departmentTheme = new ThemeEntity().setId(THEME_ID);
        department = new VDepartmentShadowedEntity()
                .setId(DEPARTMENT_ID)
                .setThemeId(THEME_ID)
                .setDefaultMailSignature("Fachbereich Muster");

        when(mailService.isSendingConfigured()).thenReturn(true);
        when(processService.retrieve(PROCESS_ID)).thenReturn(Optional.of(
                new ProcessEntity().setId(PROCESS_ID).setDepartmentId(DEPARTMENT_ID)
        ));
        when(vDepartmentShadowedService.retrieve(DEPARTMENT_ID)).thenReturn(Optional.of(department));
        when(themeService.retrieve(THEME_ID)).thenReturn(Optional.of(departmentTheme));

        node = new EMailActionNodeV1(
                mailService,
                templateRenderService,
                processInstanceAttachmentService,
                processInstanceAttachmentSetService,
                storageService,
                assignmentResolver,
                processService,
                vDepartmentShadowedService,
                themeService,
                systemService
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

        verify(mailService).sendMail(
                same(departmentTheme),
                eq("customer@example.test"),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq("Nachricht für Ada"),
                eq(MailTemplate.ProcessEmail),
                org.mockito.ArgumentMatchers.<Map<String, Object>>argThat(mailContext ->
                        "Nachricht für Ada".equals(mailContext.get("title"))
                                && "Hallo **Ada**".equals(mailContext.get("messageText"))
                                && mailContext.get("messageHtml").toString().contains("<strong>Ada</strong>")
                                && department.equals(mailContext.get("department"))
                ),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(MailSendOptions.defaults())
        );
        assertEquals("Nachricht für Ada", result.getNodeData().get("subject"));
        assertTrue(result.getNodeData().get("content").toString().contains("<strong>Ada</strong>"));
        assertArrayEquals(
                new String[]{"customer@example.test"},
                (String[]) result.getNodeData().get("to")
        );
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
        verifyNoInteractions(mailService);
    }

    @Test
    void staffTaskUsesRenderedDefaultsAndSendsEditedValues() throws Exception {
        var configuration = configuration("manual");
        var processData = new ProcessExecutionData().addProcessData(Map.of("name", "Ada"));
        var context = staffContext(configuration, processData, processInstance(), task());
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

        var view = node.getStaffTaskView(context);
        var layout = (GroupLayoutElement) view.layout();
        assertTrue(Boolean.TRUE.equals(layout.findChild("subject", TextInputElement.class).orElseThrow().getRequired()));
        assertTrue(Boolean.TRUE.equals(layout.findChild("body", RichTextInputElement.class).orElseThrow().getRequired()));
        assertEquals("send", view.events().getFirst().event());

        var defaults = view.data();
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
        verify(mailService).sendMail(
                same(departmentTheme),
                eq("customer@example.test"),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq("Finaler Betreff"),
                eq(MailTemplate.ProcessEmail),
                org.mockito.ArgumentMatchers.<Map<String, Object>>argThat(mailContext ->
                        "Finaler Inhalt".equals(mailContext.get("messageText"))
                                && mailContext.get("messageHtml").toString().contains("Finaler Inhalt")
                ),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(MailSendOptions.defaults())
        );
        assertEquals("Finaler Betreff", result.getNodeData().get("subject"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendsBccAndProcessAttachmentsThroughTheCentralMailService() throws Exception {
        var configuration = configuration("automatic");
        configuration.bcc = "{{ $.bcc }}";
        configuration.attachmentSetDataKeys = List.of("documents");
        var processData = new ProcessExecutionData();
        stubAutomaticMessage(configuration, processData);
        when(templateRenderService.interpolate(same(processData), eq(configuration.bcc)))
                .thenReturn("audit@example.test");

        var attachmentSet = new ProcessInstanceAttachmentSetEntity().setId(11);
        var attachment = new ProcessInstanceAttachmentEntity()
                .setFileName("notice.pdf")
                .setStorageProviderId(12)
                .setStoragePathFromRoot("/process/notice.pdf");
        var bytes = "attachment".getBytes(StandardCharsets.UTF_8);
        when(processInstanceAttachmentSetService.findAllByProcessInstanceIdAndDataKey(
                PROCESS_INSTANCE_ID,
                "documents"
        )).thenReturn(List.of(attachmentSet));
        when(processInstanceAttachmentService.findAllByAttachmentSetId(11)).thenReturn(List.of(attachment));
        when(storageService.getDocumentContent(12, "/process/notice.pdf"))
                .thenReturn(new ByteArrayInputStream(bytes));

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(initContext(configuration, processData, processInstance(), task()))
        );

        var attachmentsCaptor = ArgumentCaptor.forClass(Optional.class);
        verify(mailService).sendMail(
                same(departmentTheme),
                eq("customer@example.test"),
                eq(Optional.empty()),
                eq(Optional.of("audit@example.test")),
                eq("Nachricht für Ada"),
                eq(MailTemplate.ProcessEmail),
                any(),
                eq(Optional.empty()),
                attachmentsCaptor.capture(),
                eq(MailSendOptions.defaults())
        );
        var attachments = (Optional<Collection<MailAttachmentBytes>>) attachmentsCaptor.getValue();
        var mailAttachment = attachments.orElseThrow().iterator().next();
        assertEquals("notice.pdf", mailAttachment.filename());
        assertEquals(MediaType.APPLICATION_PDF, mailAttachment.contentType());
        assertArrayEquals(bytes, mailAttachment.bytes());
        assertArrayEquals(
                new String[]{"audit@example.test"},
                (String[]) result.getNodeData().get("bcc")
        );
    }

    @Test
    void fallsBackToTheSystemThemeWhenTheDepartmentHasNoTheme() throws Exception {
        var configuration = configuration("automatic");
        var processData = new ProcessExecutionData();
        stubAutomaticMessage(configuration, processData);
        department.setThemeId(null);
        var systemTheme = new ThemeEntity().setId(0);
        when(systemService.retrieveDefaultTheme()).thenReturn(systemTheme);

        node.init(initContext(configuration, processData, processInstance(), task()));

        verify(mailService).sendMail(
                same(systemTheme),
                anyString(),
                any(),
                any(),
                anyString(),
                eq(MailTemplate.ProcessEmail),
                any(),
                any(),
                any(),
                eq(MailSendOptions.defaults())
        );
        verify(themeService, never()).retrieve(anyInt());
    }

    @Test
    void rejectsSendingWhenSmtpIsNotConfigured() {
        var configuration = configuration("automatic");
        var processData = new ProcessExecutionData();
        stubAutomaticMessage(configuration, processData);
        when(mailService.isSendingConfigured()).thenReturn(false);

        var exception = assertThrows(
                ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> node.init(initContext(configuration, processData, processInstance(), task()))
        );

        assertEquals("Der E-Mail-Versand ist nicht konfiguriert.", exception.getMessage());
        verifyNoInteractions(processService);
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

    private void stubAutomaticMessage(
            EMailActionNodeV1.EMailActionNodeConfig configuration,
            ProcessExecutionData processData
    ) {
        when(templateRenderService.interpolate(
                same(processData),
                eq(configuration.messageConfig.automaticContent.subject)
        )).thenReturn("Nachricht für Ada");
        when(templateRenderService.interpolate(
                same(processData),
                eq(configuration.messageConfig.automaticContent.content)
        )).thenReturn("Hallo **Ada**");
        when(templateRenderService.interpolate(same(processData), eq(configuration.to)))
                .thenReturn("customer@example.test");
    }

    private static ProcessInstanceEntity processInstance() {
        var processInstance = mock(ProcessInstanceEntity.class);
        when(processInstance.getId()).thenReturn(PROCESS_INSTANCE_ID);
        when(processInstance.getProcessId()).thenReturn(PROCESS_ID);
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
