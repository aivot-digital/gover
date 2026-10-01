package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import de.aivot.prosuna.backend.communication.models.CommunicationMessageCallToAction;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.department.services.VDepartmentShadowedService;
import de.aivot.prosuna.backend.department.entities.VDepartmentShadowedEntity;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.EffectiveElementValues;
import de.aivot.prosuna.backend.elements.models.elements.form.input.FileUploadInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.FileUploadInputElementItem;
import de.aivot.prosuna.backend.elements.models.elements.form.input.IdentityConfigElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.IdentityConfigElementSlot;
import de.aivot.prosuna.backend.elements.models.elements.form.input.ProcessIdentityIdInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RadioInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.prosuna.backend.elements.services.AuthoredInputValueService;
import de.aivot.prosuna.backend.elements.uiPresets.SemiAutomaticMessageConfig;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEventEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeConfigurationValidationPhase;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.filters.ProcessInstanceAttachmentFilter;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodeCustomerView;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinitionMetadata;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.ProcessNodeOutput;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskAssignedCustomer;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUICustomer;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeConfigurationValidationContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUIStaff;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.prosuna.backend.process.services.AssignmentContextAssigneeResolverService;
import de.aivot.prosuna.backend.process.services.FileUploadMultipartInputService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.prosuna.backend.submission.services.ElementDataTransformService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FormRequestActionNodeV1Test {
    private static final long PROCESS_INSTANCE_ID = 99L;
    private static final long PROCESS_INSTANCE_TASK_ID = 456L;
    private static final String RECIPIENT_IDENTITY_ID = "applicant";

    private ProcessInstanceAttachmentService processInstanceAttachmentService;
    private ProsunaConfig prosunaConfig;
    private VDepartmentShadowedService vDepartmentShadowedService;
    private ProcessInstanceHistoryEventRepository historyRepository;
    private AssignmentContextAssigneeResolverService assigneeResolver;
    private FormRequestActionNodeV1 node;

    @BeforeEach
    void setUp() {
        processInstanceAttachmentService = mock(ProcessInstanceAttachmentService.class);
        vDepartmentShadowedService = mock(VDepartmentShadowedService.class);
        historyRepository = mock(ProcessInstanceHistoryEventRepository.class);
        assigneeResolver = mock(AssignmentContextAssigneeResolverService.class);
        prosunaConfig = new ProsunaConfig();
        prosunaConfig.setProsunaHostname("https://example.test");
        node = new FormRequestActionNodeV1(
                assigneeResolver,
                prosunaConfig,
                new ElementDataTransformService(),
                new AuthoredInputValueService(JsonMapperTestUtils.createMapper()),
                processInstanceAttachmentService,
                vDepartmentShadowedService
        );
    }

    @Test
    void configurationLayoutExplainsTheAutomaticallyAppendedFormLink() throws Exception {
        var processNode = mock(ProcessNodeEntity.class);
        when(processNode.getProcessId()).thenReturn(42);
        when(processNode.getProcessVersion()).thenReturn(3);
        var layout = node.getConfigurationLayout(new ProcessNodeDefinitionConfigurationLayoutContext(
                null,
                mock(ProcessEntity.class),
                mock(ProcessVersionEntity.class),
                processNode
        ));

        var recipient = layout.findChild(
                FormRequestActionNodeV1.NodeConfig.RECIPIENT_IDENTITY_ID_FIELD_ID,
                ProcessIdentityIdInputElement.class
        ).orElseThrow();
        assertEquals("Identität", recipient.getLabel());
        assertEquals("Wählen Sie die Identität aus, an welche die Nachricht gesendet wird.", recipient.getHint());
        assertEquals("existing", node.getInitialConfiguration().getLiteral(FormRequestActionNodeV1.NodeConfig.RECIPIENT_MODE_FIELD_ID));
        assertEquals(2, layout.findChild(
                FormRequestActionNodeV1.NodeConfig.RECIPIENT_MODE_FIELD_ID,
                RadioInputElement.class
        ).orElseThrow().getOptions().size());
        var newIdentities = layout.findChild(
                FormRequestActionNodeV1.NodeConfig.NEW_IDENTITIES_FIELD_ID,
                IdentityConfigElement.class
        ).orElseThrow();
        assertEquals(1, newIdentities.getMaxSlots());
        assertEquals(false, newIdentities.getOptionalSlotsAllowed());
        assertNotNull(newIdentities.getVisibility());
        assertNotNull(recipient.getVisibility());

        var automaticContent = layout.findChild(
                SemiAutomaticMessageConfig.AutomaticContent.CONTENT_FIELD_ID,
                RichTextInputElement.class
        ).orElseThrow();
        assertEquals(
                "Der Text der Nachricht. Der Link, unter welchem die Identität das Formular aufrufen kann, wird automatisch an das Ende angefügt.",
                automaticContent.getHint()
        );
    }

    @Test
    void customerAssignmentMessageContainsAFormCallToAction() {
        var configuration = new FormRequestActionNodeV1.NodeConfig();
        configuration.recipientIdentityId = RECIPIENT_IDENTITY_ID;
        configuration.messageConfig = new SemiAutomaticMessageConfig.LayoutConfig();
        configuration.messageConfig.executionType = SemiAutomaticMessageConfig.LayoutConfig.EXECUTION_TYPE_AUTOMATIC;
        configuration.messageConfig.automaticContent = new SemiAutomaticMessageConfig.AutomaticContent();
        configuration.messageConfig.automaticContent.signatureDepartmentId = 17;
        var signatureDepartment = new VDepartmentShadowedEntity().setId(17).setName("Bürgerbüro");
        when(vDepartmentShadowedService.retrieve(17)).thenReturn(java.util.Optional.of(signatureDepartment));
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
        assertSame(signatureDepartment, message.signatureDepartment());
        assertEquals(RECIPIENT_IDENTITY_ID, result.getIdentityId());
        assertEquals(RECIPIENT_IDENTITY_ID, result.getCommunicationRequest().recipientIdentityId());
        assertEquals(
                List.of(new CommunicationMessageCallToAction(
                        "Daten einreichen",
                        "https://example.test/process/instance-access/tasks/task-access"
                )),
                message.callToActions()
        );
    }

    @Test
    void newRecipientModeInvitesByEmailAndCollectsIdentityBeforeForm() throws Exception {
        var configuration = new FormRequestActionNodeV1.NodeConfig();
        configuration.recipientMode = "new";
        configuration.recipientEmailAddress = "  invitee@example.test  ";
        configuration.newIdentities = List.of(new IdentityConfigElementSlot()
                .setId("representative")
                .setTitle("Vertretung")
                .setAllowsMail(true)
                .setIsOptional(false));
        configuration.uiDefinition = new GroupLayoutElement();
        configuration.uiDefinition.setId("form");

        var identities = new IdentityDataMap();
        identities.put("other", new IdentityData(
                "session", "other", IdentityType.Email, null, null, null,
                "other@example.test", Map.of(), null, Map.of()
        ));
        var instance = new ProcessInstanceEntity()
                .setAccessKey("instance-access")
                .setIdentities(identities);
        var task = new ProcessInstanceTaskEntity().setAccessKey("task-access");
        var result = assertInstanceOf(ProcessNodeExecutionResultTaskAssignedCustomer.class,
                ReflectionTestUtils.invokeMethod(node, "createCustomerAssignmentResult",
                        instance, task, configuration, "Daten ergänzen", "Bitte ergänzen"));

        assertNull(result.getIdentityId());
        assertNull(result.getCommunicationRequest().recipientIdentityId());
        assertEquals("invitee@example.test", result.getCommunicationRequest().recipientEmailAddress());
        assertEquals(List.of(new CommunicationMessageCallToAction(
                "Daten einreichen", "https://example.test/process/instance-access/tasks/task-access"
        )), result.getCommunicationRequest().message().callToActions());
        assertEquals(1, instance.getIdentities().size());

        var customerContext = context(configuration, Map.of());
        when(customerContext.getThisProcessInstance()).thenReturn(instance);
        ProcessNodeCustomerView customerView = node.getCustomerTaskView(customerContext);
        assertNull(customerView.requiredExistingIdentityId());
        assertSame(configuration.newIdentities.getFirst(), customerView.requiredNewIdentitySlot());

        var completed = assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class,
                node.onEventFromCustomerTaskView(
                        customerContext,
                        new AuthoredElementValues(),
                        new DerivedRuntimeElementData().setEffectiveValues(new EffectiveElementValues()),
                        "submit"
                ).orElseThrow());
        assertEquals("representative", completed.getNodeData().get("recipientIdentityId"));

        var metadata = node.getMetadata(mock(ProcessNodeEntity.class), configuration, ProcessNodeDefinitionMetadata.empty());
        assertEquals(1, metadata.forwardedIdentities().size());
        assertEquals("representative", metadata.forwardedIdentities().getFirst().identityId());
        assertEquals("Vertretung", metadata.forwardedIdentities().getFirst().label());
    }

    @Test
    void customerTaskViewPrefillsMappedFieldsFromTaskProcessData() throws Exception {
        var mappedField = new TextInputElement();
        mappedField.setId("name");
        mappedField.setDestinationKey("applicant.name");
        var unmappedField = new TextInputElement();
        unmappedField.setId("internalNote");
        var layout = new GroupLayoutElement();
        layout.setId("form");
        layout.setChildren(List.of(mappedField, unmappedField));

        var configuration = new FormRequestActionNodeV1.NodeConfig();
        configuration.recipientIdentityId = RECIPIENT_IDENTITY_ID;
        configuration.uiDefinition = layout;

        var view = node.getCustomerTaskView(context(configuration, Map.of(
                "applicant", Map.of("name", "Ada"),
                "internalNote", "Nicht freigeben"
        )));

        assertEquals("Ada", view.data().getLiteral("name"));
        assertFalse(view.data().containsKey("internalNote"));
        assertEquals(RECIPIENT_IDENTITY_ID, view.requiredExistingIdentityId());
    }

    @Test
    void customerTaskViewKeepsSavedInputsAheadOfMappedProcessData() throws Exception {
        var nameField = new TextInputElement();
        nameField.setId("name");
        nameField.setDestinationKey("applicant.name");
        var cityField = new TextInputElement();
        cityField.setId("city");
        cityField.setDestinationKey("applicant.city");
        var layout = new GroupLayoutElement();
        layout.setId("form");
        layout.setChildren(List.of(nameField, cityField));

        var configuration = new FormRequestActionNodeV1.NodeConfig();
        configuration.recipientIdentityId = RECIPIENT_IDENTITY_ID;
        configuration.uiDefinition = layout;
        var savedData = new AuthoredElementValues().putLiteral("name", null);

        var view = node.getCustomerTaskView(context(
                configuration,
                Map.of("applicant", Map.of("name", "Ada", "city", "Berlin")),
                Map.of(ProcessNodeDefinition.CUSTOMER_TASK_VIEW_DATA_RUNTIME_KEY, savedData)
        ));

        assertTrue(view.data().containsKey("name"));
        assertNull(view.data().getLiteral("name"));
        assertEquals("Berlin", view.data().getLiteral("city"));
    }

    @Test
    void customerTaskViewWithoutMappingHasNoPrefilledData() throws Exception {
        var field = new TextInputElement();
        field.setId("name");
        var layout = new GroupLayoutElement();
        layout.setId("form");
        layout.setChildren(List.of(field));
        var configuration = new FormRequestActionNodeV1.NodeConfig();
        configuration.uiDefinition = layout;

        var view = node.getCustomerTaskView(context(configuration, Map.of("name", "Ada")));

        assertTrue(view.data().isEmpty());
    }

    @Test
    void getMetadata_ExposesFormAndPreservesPreviousMetadataForExistingRecipient() {
        var previousOrigin = mock(ProcessNodeEntity.class);
        var previousLayout = new GroupLayoutElement();
        previousLayout.setName("Vorherige Oberfläche");
        var previousField = new TextInputElement();
        previousField.setId("previousField");
        previousLayout.setChildren(List.of(previousField));
        var previousMetadata = ProcessNodeDefinitionMetadata.empty().withLayout(previousLayout, previousOrigin);

        var formField = new TextInputElement();
        formField.setId("name");
        formField.setDestinationKey("applicant.name");
        var form = new GroupLayoutElement();
        form.setName("Angeforderte Daten");
        form.setChildren(List.of(formField));
        var configuration = new FormRequestActionNodeV1.NodeConfig();
        configuration.recipientMode = "existing";
        configuration.uiDefinition = form;
        var processNode = mock(ProcessNodeEntity.class);

        var metadata = node.getMetadata(processNode, configuration, previousMetadata);

        assertEquals(2, metadata.reusableUiDefinitions().size());
        assertEquals("Vorherige Oberfläche", metadata.reusableUiDefinitions().get(0).label());
        assertSame(previousOrigin, metadata.reusableUiDefinitions().get(0).origin());
        assertEquals("Angeforderte Daten", metadata.reusableUiDefinitions().get(1).label());
        assertSame(form, metadata.reusableUiDefinitions().get(1).uiDefinition());
        assertSame(processNode, metadata.reusableUiDefinitions().get(1).origin());
        assertEquals("applicant.name", metadata.forwardedProcessDataKeys().getFirst().processDataKey());
        assertEquals(1, previousMetadata.reusableUiDefinitions().size());
        assertTrue(previousMetadata.forwardedProcessDataKeys().isEmpty());
    }

    @Test
    void getMetadata_ExposesFormAlongsideNewRecipientIdentity() {
        var form = new GroupLayoutElement();
        var formField = new TextInputElement();
        formField.setId("name");
        form.setChildren(List.of(formField));
        var configuration = new FormRequestActionNodeV1.NodeConfig();
        configuration.recipientMode = "new";
        configuration.newIdentities = List.of(new IdentityConfigElementSlot()
                .setId("representative")
                .setTitle("Vertretung"));
        configuration.uiDefinition = form;
        var processNode = mock(ProcessNodeEntity.class);

        var metadata = node.getMetadata(processNode, configuration, ProcessNodeDefinitionMetadata.empty());

        assertEquals(1, metadata.reusableUiDefinitions().size());
        assertSame(form, metadata.reusableUiDefinitions().getFirst().uiDefinition());
        assertSame(processNode, metadata.reusableUiDefinitions().getFirst().origin());
        assertEquals("representative", metadata.forwardedIdentities().getFirst().identityId());
    }

    @Test
    void getMetadata_SkipsMissingOrEmptyForm() {
        var configuration = new FormRequestActionNodeV1.NodeConfig();
        configuration.recipientMode = "existing";
        var processNode = mock(ProcessNodeEntity.class);

        assertTrue(node.getMetadata(processNode, configuration, ProcessNodeDefinitionMetadata.empty())
                .reusableUiDefinitions().isEmpty());

        configuration.uiDefinition = new GroupLayoutElement();
        assertTrue(node.getMetadata(processNode, configuration, ProcessNodeDefinitionMetadata.empty())
                .reusableUiDefinitions().isEmpty());
    }

    @Test
    void newRecipientModeRejectsMissingOrOptionalIdentityAndInvalidEmail() {
        var configuration = new FormRequestActionNodeV1.NodeConfig();
        configuration.recipientMode = "new";
        configuration.recipientEmailAddress = "invalid";
        configuration.newIdentities = List.of(new IdentityConfigElementSlot()
                .setId("representative")
                .setTitle("Vertretung")
                .setAllowsMail(true)
                .setIsOptional(true));
        var errors = node.validateConfiguration(new ProcessNodeConfigurationValidationContext<>(
                mock(ProcessNodeEntity.class), configuration, DerivedRuntimeElementData.empty(),
                ProcessNodeConfigurationValidationPhase.Authoring
        ));
        assertNotNull(errors);
        assertTrue(errors.containsKey(FormRequestActionNodeV1.NodeConfig.NEW_IDENTITIES_FIELD_ID));
        assertTrue(errors.containsKey(FormRequestActionNodeV1.NodeConfig.RECIPIENT_EMAIL_ADDRESS_FIELD_ID));

        configuration.newIdentities = List.of();
        var missingSlotErrors = node.validateConfiguration(new ProcessNodeConfigurationValidationContext<>(
                mock(ProcessNodeEntity.class), configuration, DerivedRuntimeElementData.empty(),
                ProcessNodeConfigurationValidationPhase.Authoring
        ));
        assertTrue(missingSlotErrors.containsKey(FormRequestActionNodeV1.NodeConfig.NEW_IDENTITIES_FIELD_ID));
    }

    @Test
    @SuppressWarnings("unchecked")
    void newRecipientModeReusesExistingIdentityOnRepeatedAutomaticExecution() throws Exception {
        var configuration = new FormRequestActionNodeV1.NodeConfig();
        configuration.recipientMode = "new";
        configuration.recipientEmailAddress = "invitee@example.test";
        configuration.newIdentities = List.of(new IdentityConfigElementSlot()
                .setId("applicant").setTitle("Neue Person").setAllowsMail(true));
        configuration.uiDefinition = new GroupLayoutElement();
        configuration.uiDefinition.setId("form");
        configuration.messageConfig = new SemiAutomaticMessageConfig.LayoutConfig();
        configuration.messageConfig.executionType = SemiAutomaticMessageConfig.LayoutConfig.EXECUTION_TYPE_AUTOMATIC;
        configuration.messageConfig.automaticContent = new SemiAutomaticMessageConfig.AutomaticContent();
        configuration.messageConfig.automaticContent.subject = "Daten ergänzen";
        configuration.messageConfig.automaticContent.content = "Bitte ergänzen";
        var identities = new IdentityDataMap();
        var identity = new IdentityData(
                "session", "applicant", IdentityType.Email, null, null, null,
                "existing@example.test", Map.of(), null, Map.of()
        );
        identities.put("applicant", identity);
        var instance = new ProcessInstanceEntity()
                .setAccessKey("instance-access")
                .setIdentities(identities);
        var task = new ProcessInstanceTaskEntity().setAccessKey("task-access");
        var context = mock(ProcessNodeExecutionInitContext.class);
        when(context.getLogger()).thenReturn(mock(ProcessNodeExecutionLogger.class));
        when(context.getConfigurationOfExecutingNode()).thenReturn(configuration);
        when(context.getThisProcessInstance()).thenReturn(instance);
        when(context.getThisTask()).thenReturn(task);

        var result = assertInstanceOf(ProcessNodeExecutionResultTaskAssignedCustomer.class, node.init(context));

        assertNull(result.getClearCurrentlyAssignedUser());
        assertEquals("applicant", result.getIdentityId());
        assertEquals("applicant", result.getCommunicationRequest().recipientIdentityId());
        assertNull(result.getCommunicationRequest().recipientEmailAddress());
        assertEquals(1, instance.getIdentities().size());
        assertSame(identity, instance.getIdentities().get("applicant"));

        var customerContext = context(configuration, Map.of());
        when(customerContext.getThisProcessInstance()).thenReturn(instance);
        var view = node.getCustomerTaskView(customerContext);
        assertEquals("applicant", view.requiredExistingIdentityId());
        assertNull(view.requiredNewIdentitySlot());

        var completed = assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class,
                node.onEventFromCustomerTaskView(
                        customerContext,
                        new AuthoredElementValues(),
                        new DerivedRuntimeElementData().setEffectiveValues(new EffectiveElementValues()),
                        "submit"
                ).orElseThrow());
        assertEquals("applicant", completed.getNodeData().get("recipientIdentityId"));
        assertSame(identity, instance.getIdentities().get("applicant"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void newRecipientModeHidesInvitationEmailAndSendsToExistingIdentityInManualExecution() throws Exception {
        var configuration = new FormRequestActionNodeV1.NodeConfig();
        configuration.recipientMode = "new";
        configuration.recipientEmailAddress = "invitee@example.test";
        configuration.newIdentities = List.of(new IdentityConfigElementSlot()
                .setId("applicant").setTitle("Neue Person").setAllowsMail(true));
        configuration.messageConfig = new SemiAutomaticMessageConfig.LayoutConfig();
        configuration.messageConfig.executionType = SemiAutomaticMessageConfig.LayoutConfig.EXECUTION_TYPE_MANUAL;
        configuration.messageConfig.manualContent = new SemiAutomaticMessageConfig.ManualContent();
        configuration.messageConfig.manualContent.subject = "Daten ergänzen";
        configuration.messageConfig.manualContent.content = "Bitte ergänzen";
        var identities = new IdentityDataMap();
        identities.put("applicant", new IdentityData(
                "session", "applicant", IdentityType.Email, null, null, null,
                "existing@example.test", Map.of(), null, Map.of()
        ));
        var instance = new ProcessInstanceEntity()
                .setAccessKey("instance-access")
                .setIdentities(identities);
        var task = new ProcessInstanceTaskEntity()
                .setAccessKey("task-access")
                .setRuntimeData(Map.of());
        var context = mock(ProcessNodeExecutionContextUIStaff.class);
        when(context.getLogger()).thenReturn(mock(ProcessNodeExecutionLogger.class));
        when(context.getCallingUser()).thenReturn(new UserEntity().setId("staff-1").setFullName("Ada Beispiel"));
        when(context.getConfigurationOfExecutingNode()).thenReturn(configuration);
        when(context.getThisProcessInstance()).thenReturn(instance);
        when(context.getThisTask()).thenReturn(task);

        var view = node.getStaffTaskView(context);
        assertFalse(view.data().containsKey("recipientEmailAddress"));
        assertTrue(assertInstanceOf(GroupLayoutElement.class, view.layout())
                .findChild("recipientEmailAddress", TextInputElement.class).isEmpty());

        var result = assertInstanceOf(ProcessNodeExecutionResultTaskAssignedCustomer.class,
                node.onEventFromStaffTaskView(
                        context,
                        new AuthoredElementValues()
                                .putLiteral("subject", "Weitere Daten")
                                .putLiteral("body", "Bitte erneut ergänzen"),
                        "send"
                ).orElseThrow());
        assertEquals(Boolean.TRUE, result.getClearCurrentlyAssignedUser());
        assertEquals("applicant", result.getIdentityId());
        assertEquals("applicant", result.getCommunicationRequest().recipientIdentityId());
        assertNull(result.getCommunicationRequest().recipientEmailAddress());
    }

    @Test
    void getOutputs_ExposesRecipientAndFormSubmissionData() {
        assertEquals(
                List.of(
                        new ProcessNodeOutput(
                                "recipientIdentityId",
                                "Identität",
                                "Die ID der bestehenden oder beim Einreichen neu angegebenen Prozessidentität.",
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
        var processData = Map.<String, Object>of(
                "applicant", Map.of(
                        "name", "Vorheriger Name",
                        "reference", "bleibt erhalten"
                ),
                "status", "offen"
        );

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
        effectiveValues.put("rows", List.of(
                Map.of("id", "row-1", "values", Map.of("files", List.of(activeFile, foreignFile, activeFile)))
        ));
        var derived = new DerivedRuntimeElementData().setEffectiveValues(effectiveValues);

        var beforeSubmission = Instant.now();
        var result = node.onEventFromCustomerTaskView(
                context(configuration, processData),
                new AuthoredElementValues(),
                derived,
                "submit"
        ).orElseThrow();
        var afterSubmission = Instant.now();

        var completed = assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, result);
        assertEquals("submitted", completed.getViaPort());
        assertEquals(
                Map.of(
                        "applicant", Map.of(
                                "name", "Ada",
                                "reference", "bleibt erhalten"
                        ),
                        "status", "offen"
                ),
                completed.getProcessData()
        );

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
    void preparationViewsAndAutosaveDoNotCreateHistoryEvents() throws Exception {
        var configuration = historyConfiguration(false, "existing");
        var instance = historyInstance(true, "Antragstellende");
        when(assigneeResolver.resolveAssignee(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of("staff-1"));

        node.init(initContext(configuration, instance));
        var staffContext = staffContext(configuration, instance);
        node.getStaffTaskView(staffContext);
        node.onAutoSaveFromStaffTaskView(staffContext, new AuthoredElementValues());

        var customerContext = customerContext(configuration, instance);
        node.getCustomerTaskView(customerContext);
        node.onAutoSaveFromCustomerTaskView(customerContext, new AuthoredElementValues(), DerivedRuntimeElementData.empty());

        verifyNoInteractions(historyRepository);
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            true,  existing, true,  Antragstellende, Antragstellende, die Identität „Antragstellende“
            false, existing, true,  Antragstellende, Antragstellende, die Identität „Antragstellende“
            true,  existing, true,  ,               ,               die Identität „applicant“
            false, existing, true,  ,               ,               die Identität „applicant“
            true,  new,      false, ,               Vertretung,      die E-Mail-Adresse „invitee@example.test“
            false, new,      false, ,               Vertretung,      die E-Mail-Adresse „invitee@example.test“
            true,  ' new ',  false, ,               Vertretung,      die E-Mail-Adresse „invitee@example.test“
            false, ' new ',  false, ,               Vertretung,      die E-Mail-Adresse „invitee@example.test“
            true,  new,      true,  Gespeicherte Person, Gespeicherte Person, die Identität „Gespeicherte Person“
            false, new,      true,  Gespeicherte Person, Gespeicherte Person, die Identität „Gespeicherte Person“
            true,  new,      true,  ,               ,               die Identität „applicant“
            false, new,      true,  ,               ,               die Identität „applicant“
            """)
    void dispatchCreatesOneHistoryEvent(boolean automatic, String recipientMode, boolean hasIdentity,
                                       String identityTitle, String expectedTitle, String expectedRecipient) throws Exception {
        var configuration = historyConfiguration(automatic, recipientMode);
        var instance = historyInstance(hasIdentity, identityTitle);

        var result = automatic
                ? node.init(initContext(configuration, instance))
                : node.onEventFromStaffTaskView(staffContext(configuration, instance),
                        new AuthoredElementValues()
                                .putLiteral("subject", "  Daten ergänzen  ")
                                .putLiteral("body", "Bitte ergänzen"), "send").orElseThrow();

        assertInstanceOf(ProcessNodeExecutionResultTaskAssignedCustomer.class, result);
        assertEquals(hasIdentity ? RECIPIENT_IDENTITY_ID : null, result.getCommunicationRequest().recipientIdentityId());
        assertEquals(hasIdentity ? null : "invitee@example.test", result.getCommunicationRequest().recipientEmailAddress());
        var event = captureHistoryEvent();
        assertEquals(automatic ? "Automatischer Versand ausgelöst" : "Versand ausgelöst", event.getTitle());
        assertEquals("Der Versand der Aufforderung mit dem Betreff „Daten ergänzen“ an " + expectedRecipient
                + (automatic ? " wurde automatisch ausgelöst." : " wurde durch „Ada Beispiel“ ausgelöst."), event.getMessage());
        assertEquals(automatic ? null : "staff-1", event.getTriggeringUserId());
        assertEquals(automatic ? null : "staff-1", event.getConcernedUserId());
        assertEquals(RECIPIENT_IDENTITY_ID, event.getConcernedIdentityId());
        assertEquals(expectedTitle, event.getConcernedIdentityTitle());
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            existing, true,  Antragstellende,      Antragstellende,      Antragstellende
            existing, true,  ,                    ,                     applicant
            new,      false, ,                    Vertretung,           Vertretung
            new,      true,  Gespeicherte Person,  Gespeicherte Person,  Gespeicherte Person
            """)
    void customerSubmissionCreatesOneHistoryEvent(String recipientMode, boolean hasIdentity, String identityTitle,
                                                  String expectedTitle, String expectedDisplayName) throws Exception {
        var configuration = historyConfiguration(true, recipientMode);
        var instance = historyInstance(hasIdentity, identityTitle);

        var result = node.onEventFromCustomerTaskView(customerContext(configuration, instance),
                new AuthoredElementValues(), DerivedRuntimeElementData.empty(), "submit").orElseThrow();

        var completed = assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, result);
        assertEquals("submitted", completed.getViaPort());
        var event = captureHistoryEvent();
        assertEquals("Daten eingereicht", event.getTitle());
        assertEquals("Die angeforderten Daten wurden durch die Identität „" + expectedDisplayName + "“ eingereicht.", event.getMessage());
        assertNull(event.getTriggeringUserId());
        assertNull(event.getConcernedUserId());
        assertEquals(RECIPIENT_IDENTITY_ID, event.getConcernedIdentityId());
        assertEquals(expectedTitle, event.getConcernedIdentityTitle());
        assertEquals(RECIPIENT_IDENTITY_ID, event.getDetails().get("identityId"));
    }

    @Test
    void rejectedEventsAndInvalidMessagesDoNotCreateHistoryEvents() {
        var configuration = historyConfiguration(false, "existing");
        var instance = historyInstance(true, "Antragstellende");
        var staffContext = staffContext(configuration, instance);
        var customerContext = customerContext(configuration, instance);

        assertThrows(ProcessNodeExecutionExceptionUnknown.class,
                () -> node.onEventFromStaffTaskView(staffContext, new AuthoredElementValues(), "unknown"));
        assertThrows(ProcessNodeExecutionExceptionUnknown.class,
                () -> node.onEventFromCustomerTaskView(customerContext, new AuthoredElementValues(),
                        DerivedRuntimeElementData.empty(), "unknown"));
        assertThrows(ResponseException.class,
                () -> node.onEventFromStaffTaskView(staffContext, new AuthoredElementValues(), "send"));

        configuration.messageConfig.executionType = SemiAutomaticMessageConfig.LayoutConfig.EXECUTION_TYPE_AUTOMATIC;
        assertThrows(ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> node.onEventFromStaffTaskView(staffContext, new AuthoredElementValues(), "send"));
        configuration.messageConfig.automaticContent.subject = "";
        assertThrows(ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> node.init(initContext(configuration, instance)));

        verifyNoInteractions(historyRepository);
    }

    @Test
    void invalidInvitationDoesNotCreateDispatchHistory() {
        var configuration = historyConfiguration(true, "new");
        configuration.recipientEmailAddress = "invalid";
        var instance = historyInstance(false, null);

        assertThrows(ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> node.init(initContext(configuration, instance)));
        configuration.messageConfig.executionType = SemiAutomaticMessageConfig.LayoutConfig.EXECUTION_TYPE_MANUAL;
        assertThrows(ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> node.onEventFromStaffTaskView(staffContext(configuration, instance),
                        new AuthoredElementValues().putLiteral("subject", "Betreff").putLiteral("body", "Nachricht"), "send"));

        verifyNoInteractions(historyRepository);
    }

    @Test
    void failedAttachmentResolutionDoesNotCreateSubmissionHistory() throws Exception {
        var configuration = historyConfiguration(true, "existing");
        var files = new FileUploadInputElement();
        files.setId("files");
        configuration.uiDefinition.setChildren(List.of(files));
        var values = new EffectiveElementValues();
        values.put("files", List.of(new FileUploadInputElementItem()
                .setUri(FileUploadMultipartInputService.buildAttachmentUri(UUID.randomUUID()))));
        var derived = new DerivedRuntimeElementData().setEffectiveValues(values);
        var failure = ResponseException.internalServerError("Anlagen konnten nicht geladen werden.");
        when(processInstanceAttachmentService.list(any(ProcessInstanceAttachmentFilter.class))).thenThrow(failure);

        assertSame(failure, assertThrows(ResponseException.class,
                () -> node.onEventFromCustomerTaskView(customerContext(configuration, historyInstance(true, "Antragstellende")),
                        new AuthoredElementValues(), derived, "submit")));

        verifyNoInteractions(historyRepository);
    }

    @Test
    void cleanConfigurationForExportRemovesIdentityAndAssignment() {
        var configuration = new AuthoredElementValues();
        configuration.putLiteral(FormRequestActionNodeV1.NodeConfig.RECIPIENT_IDENTITY_ID_FIELD_ID, RECIPIENT_IDENTITY_ID);
        configuration.putLiteral(
                SemiAutomaticMessageConfig.ManualContent.ASSIGNMENT_FIELD_ID,
                Map.of("user", "staff-1")
        );
        configuration.putLiteral(SemiAutomaticMessageConfig.LayoutConfig.SIGNATURE_DEPARTMENT_FIELD_ID_1, 17);
        configuration.putLiteral(SemiAutomaticMessageConfig.LayoutConfig.SIGNATURE_DEPARTMENT_FIELD_ID_2, 29);
        configuration.putLiteral("portableValue", "kept");

        var cleaned = node.cleanConfigurationForExport(configuration);

        assertFalse(cleaned.containsKey(FormRequestActionNodeV1.NodeConfig.RECIPIENT_IDENTITY_ID_FIELD_ID));
        assertFalse(cleaned.containsKey(SemiAutomaticMessageConfig.ManualContent.ASSIGNMENT_FIELD_ID));
        assertFalse(cleaned.containsKey(SemiAutomaticMessageConfig.LayoutConfig.SIGNATURE_DEPARTMENT_FIELD_ID_1));
        assertFalse(cleaned.containsKey(SemiAutomaticMessageConfig.LayoutConfig.SIGNATURE_DEPARTMENT_FIELD_ID_2));
        assertEquals("kept", cleaned.getLiteral("portableValue"));
    }

    private static FormRequestActionNodeV1.NodeConfig historyConfiguration(boolean automatic, String recipientMode) {
        var configuration = new FormRequestActionNodeV1.NodeConfig();
        configuration.recipientMode = recipientMode;
        configuration.recipientIdentityId = RECIPIENT_IDENTITY_ID;
        configuration.recipientEmailAddress = "  invitee@example.test  ";
        configuration.newIdentities = List.of(new IdentityConfigElementSlot()
                .setId(RECIPIENT_IDENTITY_ID).setTitle("Vertretung").setAllowsMail(true));
        configuration.uiDefinition = new GroupLayoutElement();
        configuration.uiDefinition.setId("form");
        configuration.messageConfig = new SemiAutomaticMessageConfig.LayoutConfig();
        configuration.messageConfig.executionType = automatic
                ? SemiAutomaticMessageConfig.LayoutConfig.EXECUTION_TYPE_AUTOMATIC
                : SemiAutomaticMessageConfig.LayoutConfig.EXECUTION_TYPE_MANUAL;
        configuration.messageConfig.automaticContent = new SemiAutomaticMessageConfig.AutomaticContent();
        configuration.messageConfig.automaticContent.subject = "  Daten ergänzen  ";
        configuration.messageConfig.automaticContent.content = "Bitte ergänzen";
        configuration.messageConfig.manualContent = new SemiAutomaticMessageConfig.ManualContent();
        configuration.messageConfig.manualContent.subject = "Daten ergänzen";
        configuration.messageConfig.manualContent.content = "Bitte ergänzen";
        return configuration;
    }

    private static ProcessInstanceEntity historyInstance(boolean hasIdentity, String title) {
        var identities = new IdentityDataMap();
        if (hasIdentity) {
            identities.put(RECIPIENT_IDENTITY_ID, new IdentityData(
                    "session", RECIPIENT_IDENTITY_ID, IdentityType.Email, null, null, null,
                    "existing@example.test", Map.of(), null, Map.of(), title));
        }
        return new ProcessInstanceEntity().setId(PROCESS_INSTANCE_ID).setAccessKey("instance-access").setIdentities(identities);
    }

    private static ProcessInstanceTaskEntity historyTask() {
        return new ProcessInstanceTaskEntity().setId(PROCESS_INSTANCE_TASK_ID).setAccessKey("task-access")
                .setAssignedUserId("other-staff").setRuntimeData(Map.of()).setNodeData(Map.of()).setProcessData(Map.of());
    }

    private ProcessNodeExecutionInitContext<FormRequestActionNodeV1.NodeConfig> initContext(
            FormRequestActionNodeV1.NodeConfig configuration, ProcessInstanceEntity instance) {
        return new ProcessNodeExecutionInitContext<>(
                new ProcessNodeExecutionLogger(PROCESS_INSTANCE_ID, PROCESS_INSTANCE_TASK_ID, null, null, historyRepository),
                new ProcessNodeEntity().setId(1).setProcessId(2).setProcessVersion(3), instance, historyTask(), null,
                new ProcessExecutionData(), configuration);
    }

    private ProcessNodeExecutionContextUIStaff<FormRequestActionNodeV1.NodeConfig> staffContext(
            FormRequestActionNodeV1.NodeConfig configuration, ProcessInstanceEntity instance) {
        return new ProcessNodeExecutionContextUIStaff<>(
                new ProcessNodeExecutionLogger(PROCESS_INSTANCE_ID, PROCESS_INSTANCE_TASK_ID, "staff-1", null, historyRepository),
                new ProcessNodeEntity(), instance, historyTask(), null,
                new UserEntity().setId("staff-1").setFullName("Ada Beispiel"), configuration, new ProcessExecutionData());
    }

    private ProcessNodeExecutionContextUICustomer<FormRequestActionNodeV1.NodeConfig> customerContext(
            FormRequestActionNodeV1.NodeConfig configuration, ProcessInstanceEntity instance) {
        return new ProcessNodeExecutionContextUICustomer<>(
                new ProcessNodeExecutionLogger(PROCESS_INSTANCE_ID, PROCESS_INSTANCE_TASK_ID, null, RECIPIENT_IDENTITY_ID, historyRepository),
                new ProcessNodeEntity(), instance, historyTask(), null, RECIPIENT_IDENTITY_ID, configuration, null);
    }

    private ProcessInstanceEventEntity captureHistoryEvent() {
        var captor = ArgumentCaptor.forClass(ProcessInstanceEventEntity.class);
        verify(historyRepository).save(captor.capture());
        var event = captor.getValue();
        assertEquals(PROCESS_INSTANCE_ID, event.getProcessInstanceId());
        assertEquals(PROCESS_INSTANCE_TASK_ID, event.getProcessInstanceTaskId());
        assertEquals(ProcessNodeExecutionLogLevel.Info, event.getLevel());
        assertFalse(event.getTechnical());
        assertTrue(event.getAudit());
        assertTrue(event.getHistoryRelevant());
        assertNotNull(event.getTimestamp());
        return event;
    }

    @SuppressWarnings("unchecked")
    private static ProcessNodeExecutionContextUICustomer<FormRequestActionNodeV1.NodeConfig> context(
            FormRequestActionNodeV1.NodeConfig configuration,
            Map<String, Object> processData
    ) {
        return context(configuration, processData, Map.of());
    }

    @SuppressWarnings("unchecked")
    private static ProcessNodeExecutionContextUICustomer<FormRequestActionNodeV1.NodeConfig> context(
            FormRequestActionNodeV1.NodeConfig configuration,
            Map<String, Object> processData,
            Map<String, Object> runtimeData
    ) {
        var context = mock(ProcessNodeExecutionContextUICustomer.class);
        when(context.getLogger()).thenReturn(mock(ProcessNodeExecutionLogger.class));
        when(context.getConfigurationOfExecutingNode()).thenReturn(configuration);
        when(context.getThisProcessInstance()).thenReturn(
                new ProcessInstanceEntity().setId(PROCESS_INSTANCE_ID)
        );
        when(context.getThisTask()).thenReturn(
                new ProcessInstanceTaskEntity()
                        .setId(PROCESS_INSTANCE_TASK_ID)
                        .setRuntimeData(runtimeData)
                        .setProcessData(processData)
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
