package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.communication.models.CommunicationMessageCallToAction;
import de.aivot.prosuna.backend.communication.utils.EmailAddressUtils;
import de.aivot.prosuna.backend.department.services.VDepartmentShadowedService;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.ComputedElementState;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.ElementValidationFunctions;
import de.aivot.prosuna.backend.elements.models.elements.ElementValueFunctions;
import de.aivot.prosuna.backend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.prosuna.backend.elements.models.elements.form.input.FileUploadInputElementItem;
import de.aivot.prosuna.backend.elements.models.elements.form.input.IdentityConfigElementOption;
import de.aivot.prosuna.backend.elements.models.elements.form.input.IdentityConfigElementSlot;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RadioInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RadioInputElementOption;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElementValue;
import de.aivot.prosuna.backend.elements.services.AuthoredInputValueService;
import de.aivot.prosuna.backend.elements.uiPresets.SemiAutomaticMessageConfig;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.elements.enums.InputMode;
import de.aivot.prosuna.backend.elements.enums.ValueFunctionType;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.plugins.core.v1.operators.text.NoCodeRegexMatchOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.NoCodeEqualsOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.*;
import de.aivot.prosuna.backend.process.filters.ProcessInstanceAttachmentFilter;
import de.aivot.prosuna.backend.process.models.*;
import de.aivot.prosuna.backend.process.models.executionResult.*;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeConfigurationValidationContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUICustomer;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUIStaff;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.services.AssignmentContextAssigneeResolverService;
import de.aivot.prosuna.backend.process.services.FileUploadMultipartInputService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.prosuna.backend.submission.services.ElementDataTransformService;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class FormRequestActionNodeV1 implements ProcessNodeDefinition<FormRequestActionNodeV1.NodeConfig> {
    public static final String NODE_KEY = "form_request";
    private static final String RECIPIENT_MODE_EXISTING = "existing";
    private static final String RECIPIENT_MODE_NEW = "new";
    private static final Pattern NEW_IDENTITY_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{1,32}$");

    private static final String PORT_SUBMITTED = "submitted";

    private static final String OUTPUT_RECIPIENT_IDENTITY_ID = "recipientIdentityId";
    private static final String OUTPUT_PAYLOAD = "payload";
    private static final String OUTPUT_UNMAPPED = "unmapped";
    private static final String OUTPUT_ATTACHMENTS = "attachments";
    private static final String OUTPUT_STARTED = "started";
    private static final String OUTPUT_ATTACHMENTS_TYPE_DEFINITION =
            "Array<{ key: string; fileName: string; originalFileName: string; group: string | null; " +
                    "storageProviderId: number; storagePathFromRoot: string; }>";

    private static final String STAFF_TASK_ROOT_ID = "root";
    private static final String STAFF_TASK_SUBJECT_FIELD_ID = "subject";
    private static final String STAFF_TASK_CONTENT_FIELD_ID = "body";
    private static final String STAFF_TASK_RECIPIENT_EMAIL_FIELD_ID = "recipientEmailAddress";
    private static final String STAFF_TASK_SEND_EVENT = "send";
    private static final String CUSTOMER_TASK_SUBMIT_EVENT = "submit";

    private final AssignmentContextAssigneeResolverService assignmentContextAssigneeResolverService;
    private final ProsunaConfig prosunaConfig;
    private final ElementDataTransformService elementDataTransformService;
    private final AuthoredInputValueService authoredInputValueService;
    private final ProcessInstanceAttachmentService processInstanceAttachmentService;
    private final VDepartmentShadowedService vDepartmentShadowedService;

    public FormRequestActionNodeV1(AssignmentContextAssigneeResolverService assignmentContextAssigneeResolverService,
                                   ProsunaConfig prosunaConfig,
                                   ElementDataTransformService elementDataTransformService,
                                   AuthoredInputValueService authoredInputValueService,
                                   ProcessInstanceAttachmentService processInstanceAttachmentService,
                                   VDepartmentShadowedService vDepartmentShadowedService) {
        this.assignmentContextAssigneeResolverService = assignmentContextAssigneeResolverService;
        this.prosunaConfig = prosunaConfig;
        this.elementDataTransformService = elementDataTransformService;
        this.authoredInputValueService = authoredInputValueService;
        this.processInstanceAttachmentService = processInstanceAttachmentService;
        this.vDepartmentShadowedService = vDepartmentShadowedService;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return NODE_KEY;
    }

    @Nonnull
    @Override
    public String getComponentVersion() {
        return "1.0.0";
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return CorePlugin.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public ProcessNodeType getType() {
        return ProcessNodeType.Action;
    }

    @Nonnull
    @Override
    public ProcessNodeExecutionType[] getExecutionTypes() {
        return new ProcessNodeExecutionType[]{
                ProcessNodeExecutionType.Automatic,
                ProcessNodeExecutionType.SemiAutomatic
        };
    }

    @Nonnull
    @Override
    public String getName() {
        return "Daten anfordern";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Fordert Daten von einer dritten Person mittels eines Formulars an.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                
                """;
    }

    @Nonnull
    @Override
    public Class<NodeConfig> getNodeConfigurationClass() {
        return NodeConfig.class;
    }

    @Nonnull
    @Override
    public AuthoredElementValues getInitialConfiguration() {
        return new AuthoredElementValues().putLiteral(NodeConfig.RECIPIENT_MODE_FIELD_ID, RECIPIENT_MODE_EXISTING);
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper
                    .createFromPOJO(NodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Fehler bei der Erstellung des Konfigurationslayouts: %s", e.getMessage());
        }

        layout.findChild(NodeConfig.RECIPIENT_MODE_FIELD_ID, RadioInputElement.class)
                .ifPresent(field -> {
                    field.setOptions(List.of(
                            RadioInputElementOption.of(RECIPIENT_MODE_EXISTING, "Existierende Identität"),
                            RadioInputElementOption.of(RECIPIENT_MODE_NEW, "Neue Identität")
                    ));
                    field.setValue(new ElementValueFunctions()
                            .setType(ValueFunctionType.NoCode)
                            .setNoCode(NoCodeStaticValue.of(RECIPIENT_MODE_EXISTING)));
                });
        layout.findChild(NodeConfig.RECIPIENT_IDENTITY_ID_FIELD_ID, de.aivot.prosuna.backend.elements.models.elements.form.input.ProcessIdentityIdInputElement.class)
                .ifPresent(field -> field.setVisibility(recipientModeVisibility(RECIPIENT_MODE_EXISTING)));
        layout.findChild(NodeConfig.NEW_IDENTITIES_FIELD_ID, de.aivot.prosuna.backend.elements.models.elements.form.input.IdentityConfigElement.class)
                .ifPresent(field -> field.setVisibility(recipientModeVisibility(RECIPIENT_MODE_NEW)));
        layout.findChild(NodeConfig.RECIPIENT_EMAIL_ADDRESS_FIELD_ID, TextInputElement.class)
                .ifPresent(field -> {
                    field.setVisibility(recipientModeVisibility(RECIPIENT_MODE_NEW));
                    field.setMaxCharacters(254);
                    field.setValidation(ElementValidationFunctions.of(
                            NoCodeExpression.of(
                                    NoCodeRegexMatchOperator.OPERATOR_ID,
                                    NoCodeReference.of(NodeConfig.RECIPIENT_EMAIL_ADDRESS_FIELD_ID),
                                    NoCodeStaticValue.of(EmailAddressUtils.EMAIL_PATTERN_VALUE)
                            ),
                            "Bitte geben Sie eine gültige E-Mail-Adresse ein."
                    ));
                });

        layout.findChild(SemiAutomaticMessageConfig.GROUP_ID, GroupLayoutElement.class)
                .ifPresent(group -> {
                    SemiAutomaticMessageConfig.initConfigurationLayout(
                            group,
                            context.thisNode().getProcessId(),
                            context.thisNode().getProcessVersion()
                    );
                    group.findChild(SemiAutomaticMessageConfig.AutomaticContent.CONTENT_FIELD_ID, RichTextInputElement.class)
                            .ifPresent(field -> field.setHint("Der Text der Nachricht. Der Link, unter welchem die Identität das Formular aufrufen kann, wird automatisch an das Ende angefügt."));
                    group.findChild(SemiAutomaticMessageConfig.ManualContent.CONTENT_FIELD_ID, RichTextInputElement.class)
                            .ifPresent(field -> field.setHint("Der Text der Nachricht. Der Link, unter welchem die Identität das Formular aufrufen kann, wird automatisch an das Ende angefügt."));
                });

        return layout;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_SUBMITTED,
                        "Daten eingereicht",
                        "Der Prozess wird fortgesetzt, nachdem die Daten erfolgreich eingereicht wurden."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        OUTPUT_RECIPIENT_IDENTITY_ID,
                        "Identität",
                        "Die ID der bestehenden oder beim Einreichen neu angegebenen Prozessidentität.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PAYLOAD,
                        "Zugeordnete Formulardaten",
                        "Enthält alle Formulardaten welche über einen Datenschlüssel zugeordnet wurden.",
                        "Record<string, unknown>"
                ),
                new ProcessNodeOutput(
                        OUTPUT_UNMAPPED,
                        "Formular-Rohdaten",
                        "Enthält alle Formulardaten unter der jeweiligen Element-ID des Feldes, unabhängig davon, ob ein Element über einen Datenschlüssel zugewiesen wurde oder nicht.",
                        "Record<string, unknown>"
                ),
                new ProcessNodeOutput(
                        OUTPUT_ATTACHMENTS,
                        "Anlagen",
                        "Eine Liste aller Anlagen, die über dieses Formular hochgeladen wurden.",
                        OUTPUT_ATTACHMENTS_TYPE_DEFINITION
                ),
                new ProcessNodeOutput(
                        OUTPUT_STARTED,
                        "Eingangszeitstempel",
                        "Der Zeitstempel des Dateneingangs an den Auslöser",
                        "string"
                )
        );
    }

    @Nullable
    @Override
    public Map<String, List<String>> validateConfiguration(
            @Nonnull ProcessNodeConfigurationValidationContext<NodeConfig> context
    ) {
        var configuration = context.configuration();
        var errors = new LinkedHashMap<String, List<String>>();
        var mode = StringUtils.toNullableTrimmedString(configuration.recipientMode);
        if (mode != null && !RECIPIENT_MODE_EXISTING.equals(mode) && !RECIPIENT_MODE_NEW.equals(mode)) {
            errors.put(NodeConfig.RECIPIENT_MODE_FIELD_ID, List.of("Wählen Sie eine gültige Empfängerart aus."));
        }
        if (!RECIPIENT_MODE_NEW.equals(mode)) {
            return errors.isEmpty() ? null : errors;
        }

        var identities = configuration.newIdentities;
        if (identities == null || identities.size() != 1 || identities.getFirst() == null) {
            errors.put(NodeConfig.NEW_IDENTITIES_FIELD_ID, List.of("Konfigurieren Sie genau eine verpflichtende Identität."));
        } else {
            var slot = identities.getFirst();
            if (!isValidNewIdentityId(slot.getId()) || StringUtils.isNullOrEmpty(slot.getTitle())
                    || Boolean.TRUE.equals(slot.getIsOptional())) {
                errors.put(NodeConfig.NEW_IDENTITIES_FIELD_ID, List.of("Die neue Identität benötigt einen Schlüssel aus 1 bis 32 Buchstaben, Zahlen oder Unterstrichen, einen Titel und muss verpflichtend sein."));
            }
        }

        if (!context.isDeferred(NodeConfig.RECIPIENT_EMAIL_ADDRESS_FIELD_ID)
                && !EmailAddressUtils.isValidSingleAddress(configuration.recipientEmailAddress)) {
            errors.put(NodeConfig.RECIPIENT_EMAIL_ADDRESS_FIELD_ID, List.of("Bitte geben Sie eine gültige E-Mail-Adresse ein."));
        }
        return errors.isEmpty() ? null : errors;
    }

    @Nonnull
    @Override
    public ProcessNodeDefinitionMetadata getMetadata(@Nonnull ProcessNodeEntity processNodeEntity,
                                                     @Nonnull NodeConfig configuration,
                                                     @Nonnull ProcessNodeDefinitionMetadata previousMetadata) {
        var metadata = ProcessNodeDefinitionMetadata
                .reuse(previousMetadata)
                .withLayout(configuration.uiDefinition, processNodeEntity);

        if (!RECIPIENT_MODE_NEW.equals(configuration.recipientMode)
                || configuration.newIdentities == null
                || configuration.newIdentities.size() != 1) {
            return metadata;
        }
        var identity = configuration.newIdentities.getFirst();
        if (identity == null || !isValidNewIdentityId(identity.getId()) || StringUtils.isNullOrEmpty(identity.getTitle())) {
            return metadata;
        }
        return metadata.addForwardedIdentity(
                identity.getId(),
                identity.getTitle().trim(),
                identity.getDescription(),
                Optional.ofNullable(identity.getOptions()).orElse(List.of()).stream()
                        .filter(Objects::nonNull)
                        .map(IdentityConfigElementOption::getIdentityProviderKey)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList(),
                processNodeEntity
        );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<NodeConfig> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();
        if (RECIPIENT_MODE_NEW.equals(requireRecipientMode(configuration.recipientMode))) {
            requireNewRecipientConfiguration(configuration, context.getThisProcessInstance());
        }

        if (SemiAutomaticMessageConfig.isAutomatic(configuration.messageConfig)) {
            return initAutomatic(context, configuration);
        }
        if (SemiAutomaticMessageConfig.isManual(configuration.messageConfig)) {
            return initManual(context, configuration);
        }

        var executionType = configuration.messageConfig == null
                ? null
                : StringUtils.toNullableTrimmedString(configuration.messageConfig.executionType);
        throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                "Ungültige Ausführungsart für die Formularanforderung. Erwartet werden entweder %s oder %s. Übergeben wurde: %s",
                StringUtils.quote(SemiAutomaticMessageConfig.LayoutConfig.EXECUTION_TYPE_AUTOMATIC),
                StringUtils.quote(SemiAutomaticMessageConfig.LayoutConfig.EXECUTION_TYPE_MANUAL),
                StringUtils.quote(executionType)
        );
    }

    @Nonnull
    private ProcessNodeExecutionResult initAutomatic(
            @Nonnull ProcessNodeExecutionInitContext<NodeConfig> context,
            @Nonnull NodeConfig configuration
    ) throws ProcessNodeExecutionException {
        var automaticContent = requireAutomaticContent(configuration);
        var subject = automaticContent.subject.trim();
        var content = automaticContent.content.trim();

        return createCustomerAssignmentResult(
                context.getThisProcessInstance(),
                context.getThisTask(),
                configuration,
                subject,
                content
        );
    }

    @Nonnull
    private ProcessNodeExecutionResult initManual(
            @Nonnull ProcessNodeExecutionInitContext<NodeConfig> context,
            @Nonnull NodeConfig configuration
    ) throws ProcessNodeExecutionException {
        var manualContent = requireManualContent(configuration);

        var assigneeUserId = assignmentContextAssigneeResolverService
                .resolveAssignee(
                        context.getThisNode().getProcessId(),
                        context.getThisNode().getProcessVersion(),
                        context.getThisProcessInstance().getId(),
                        context.getThisNode().getId(),
                        context.getThisTask().getId(),
                        context.getThisTask().getPreviousProcessNodeId(),
                        context.getThisProcessInstance().getAssignedUserId(),
                        manualContent.assignmentContext,
                        List.of(ProcessPermissionProvider.PROCESS_INSTANCE_EDIT_TASK)
                )
                .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidAssignment(
                        "Für das Prozesselement %s konnte keine geeignete Bearbeiter:in im konfigurierten Personenkreis ermittelt werden.",
                        StringUtils.quote(context.getThisNode().resolveName(this))
                ));

        var runtimeData = new LinkedHashMap<>(context.getThisTask().getRuntimeData());

        return ProcessNodeExecutionResultTaskAssigned
                .of(assigneeUserId)
                .setRuntimeData(runtimeData)
                .setProcessData(context.getCurrentProcessExecutionData().getProcessData());
    }

    @Nonnull
    @Override
    public ProcessNodeStaffView getStaffTaskView(
            @Nonnull ProcessNodeExecutionContextUIStaff<NodeConfig> context
    ) throws ResponseException {
        var configuration = context.getConfigurationOfExecutingNode();
        var subjectField = new TextInputElement();
        subjectField.setId(STAFF_TASK_SUBJECT_FIELD_ID);
        subjectField.setLabel("Betreff der Aufforderung");
        subjectField.setRequired(true);

        var contentField = new RichTextInputElement();
        contentField.setId(STAFF_TASK_CONTENT_FIELD_ID);
        contentField.setLabel("Nachricht der Aufforderung");
        contentField.setHint("Der Text der Nachricht. Der Link, unter welchem die Identität das Formular aufrufen kann, wird automatisch an das Ende angefügt.");
        contentField.setRequired(true);

        var root = new GroupLayoutElement();
        root.setId(STAFF_TASK_ROOT_ID);
        root.setChildren(new LinkedList<>(List.of(subjectField, contentField)));

        var manualContent = requireManualContentForStaffView(configuration);
        var taskViewData = new AuthoredElementValues();

        taskViewData.putLiteral(STAFF_TASK_SUBJECT_FIELD_ID, manualContent.subject.trim());
        taskViewData.putLiteral(STAFF_TASK_CONTENT_FIELD_ID, manualContent.content.trim());

        if (RECIPIENT_MODE_NEW.equals(configuration.recipientMode)) {
            try {
                var recipientId = requireNewIdentitySlot(configuration).getId();
                if (!hasIdentity(context.getThisProcessInstance(), recipientId)) {
                    var recipientField = new TextInputElement();
                    recipientField.setId(STAFF_TASK_RECIPIENT_EMAIL_FIELD_ID);
                    recipientField.setLabel("E-Mail-Adresse der Einladung");
                    recipientField.setDisabled(true);
                    root.getChildren().addFirst(recipientField);
                    taskViewData.putLiteral(STAFF_TASK_RECIPIENT_EMAIL_FIELD_ID, requireInvitationEmailForStaffView(configuration));
                }
            } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
                throw ResponseException.internalServerError(e, e.getMessage());
            }
        }

        return ProcessNodeStaffView.of(
                context,
                root,
                List.of(new TaskViewEvent("Aufforderung versenden", STAFF_TASK_SEND_EVENT)),
                taskViewData
        );
    }

    @Nonnull
    @Override
    public Optional<ProcessNodeExecutionResult> onEventFromStaffTaskView(
            @Nonnull ProcessNodeExecutionContextUIStaff<NodeConfig> context,
            @Nonnull AuthoredElementValues update,
            @Nonnull String event
    ) throws ResponseException, ProcessNodeExecutionException {
        if (!STAFF_TASK_SEND_EVENT.equals(event)) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    "Das Event %s wird von diesem Prozesselement nicht unterstützt.",
                    StringUtils.quote(event)
            );
        }

        var configuration = context.getConfigurationOfExecutingNode();
        if (!SemiAutomaticMessageConfig.isManual(configuration.messageConfig)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Aufforderung kann nur im manuellen Ausführungsmodus über eine Aufgabe versendet werden."
            );
        }

        var subject = StringUtils.toNullableTrimmedString(update.getLiteral(STAFF_TASK_SUBJECT_FIELD_ID));
        var content = StringUtils.toNullableTrimmedString(update.getLiteral(STAFF_TASK_CONTENT_FIELD_ID));
        validateStaffMessage(subject, content);

        var result = createCustomerAssignmentResult(
                context.getThisProcessInstance(),
                context.getThisTask(),
                configuration,
                subject,
                content
        );

        return Optional.of(result);
    }

    private ProcessNodeExecutionResult createCustomerAssignmentResult(ProcessInstanceEntity processInstance,
                                                                      ProcessInstanceTaskEntity task,
                                                                      NodeConfig configuration,
                                                                      String subject,
                                                                      String content) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var newRecipientMode = RECIPIENT_MODE_NEW.equals(requireRecipientMode(configuration.recipientMode));
        var recipientId = newRecipientMode
                ? requireNewRecipientConfiguration(configuration, processInstance).getId()
                : requireRecipientIdentity(configuration.recipientIdentityId);
        var newRecipient = newRecipientMode && !hasIdentity(processInstance, recipientId);

        var customerLink = prosunaConfig
                .createUrl("/process/", processInstance.getAccessKey(), "tasks", task.getAccessKey());

        var message = CommunicationMessage.of(
                subject,
                content,
                content,
                List.of(new CommunicationMessageCallToAction("Daten einreichen", customerLink)),
                List.of()
        ).withSignatureDepartment(SemiAutomaticMessageConfig.resolveSignatureDepartment(
                configuration.messageConfig,
                vDepartmentShadowedService
        ));

        var communicationRequest = newRecipient
                ? ProcessNodeExecutionResultCommunicationRequest.toEmail(requireInvitationEmail(configuration.recipientEmailAddress), message)
                : new ProcessNodeExecutionResultCommunicationRequest(recipientId, message);

        var assignmentResult = newRecipient
                ? ProcessNodeExecutionResultTaskAssignedCustomer.withoutIdentity()
                : ProcessNodeExecutionResultTaskAssignedCustomer.of(recipientId);
        return assignmentResult.setCommunicationRequest(communicationRequest);
    }

    @Nonnull
    @Override
    public ProcessNodeCustomerView getCustomerTaskView(@Nonnull ProcessNodeExecutionContextUICustomer<NodeConfig> context) throws ResponseException {
        var configuration = context.getConfigurationOfExecutingNode();
        final String existingIdentityId;
        final IdentityConfigElementSlot newIdentitySlot;
        if (RECIPIENT_MODE_NEW.equals(configuration.recipientMode)) {
            try {
                var slot = requireNewIdentitySlot(configuration);
                if (hasIdentity(context.getThisProcessInstance(), slot.getId())) {
                    existingIdentityId = slot.getId();
                    newIdentitySlot = null;
                } else {
                    existingIdentityId = null;
                    newIdentitySlot = slot;
                }
            } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
                throw ResponseException.internalServerError(e, e.getMessage());
            }
        } else {
            existingIdentityId = configuration.recipientIdentityId;
            newIdentitySlot = null;
        }
        var effectiveValues = elementDataTransformService.buildEffectiveValues(
                configuration.uiDefinition,
                context.getThisTask().getProcessData()
        );
        var initialData = authoredInputValueService.toLiteralAuthoredElementValues(
                configuration.uiDefinition,
                effectiveValues
        );
        return ProcessNodeCustomerView.of(
                context,
                configuration.uiDefinition,
                List.of(new TaskViewEvent("Daten einreichen", CUSTOMER_TASK_SUBMIT_EVENT)),
                initialData,
                existingIdentityId,
                newIdentitySlot
        );
    }

    @Nonnull
    @Override
    public Optional<ProcessNodeExecutionResult> onEventFromCustomerTaskView(@Nonnull ProcessNodeExecutionContextUICustomer<NodeConfig> context,
                                                                            @Nonnull AuthoredElementValues update,
                                                                            @Nonnull DerivedRuntimeElementData derived,
                                                                            @Nonnull String event) throws ResponseException, ProcessNodeExecutionException {
        if (!CUSTOMER_TASK_SUBMIT_EVENT.equals(event)) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    "Das Event %s wird von diesem Prozesselement nicht unterstützt.",
                    StringUtils.quote(event)
            );
        }

        var effectiveValues = derived.getEffectiveValues();
        var configuration = context.getConfigurationOfExecutingNode();
        var payload = elementDataTransformService.buildPayload(
                configuration.uiDefinition,
                effectiveValues,
                derived.getElementStates()
        );
        var updatedProcessData = elementDataTransformService.buildUpdatedProcessData(
                configuration.uiDefinition,
                derived,
                context.getThisTask().getProcessData()
        );
        var nodeData = new LinkedHashMap<String, Object>();
        nodeData.put(
                OUTPUT_RECIPIENT_IDENTITY_ID,
                RECIPIENT_MODE_NEW.equals(requireRecipientMode(configuration.recipientMode))
                        ? requireNewIdentitySlot(configuration).getId()
                        : requireRecipientIdentity(configuration.recipientIdentityId)
        );
        nodeData.put(OUTPUT_PAYLOAD, payload);
        nodeData.put(OUTPUT_UNMAPPED, effectiveValues);
        nodeData.put(OUTPUT_ATTACHMENTS, resolveSubmittedAttachments(context, effectiveValues));
        nodeData.put(OUTPUT_STARTED, Instant.now());

        var result = new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_SUBMITTED)
                .setNodeData(nodeData)
                .setProcessData(updatedProcessData);

        return Optional.of(result);
    }

    @Nonnull
    private List<Map<String, Object>> resolveSubmittedAttachments(
            @Nonnull ProcessNodeExecutionContextUICustomer<NodeConfig> context,
            @Nonnull Map<String, Object> effectiveValues
    ) throws ResponseException {
        var referencedAttachmentKeys = new LinkedHashSet<UUID>();
        collectReferencedAttachmentKeys(effectiveValues, referencedAttachmentKeys);
        if (referencedAttachmentKeys.isEmpty()) {
            return List.of();
        }

        var processInstanceId = context.getThisProcessInstance().getId();
        var processInstanceTaskId = context.getThisTask().getId();
        var attachmentsByKey = new HashMap<UUID, ProcessInstanceAttachmentEntity>();
        processInstanceAttachmentService
                .list(ProcessInstanceAttachmentFilter
                        .create()
                        .setProcessInstanceTaskId(processInstanceTaskId))
                .forEach(attachment -> {
                    if (Objects.equals(attachment.getProcessInstanceId(), processInstanceId)
                            && Objects.equals(attachment.getProcessInstanceTaskId(), processInstanceTaskId)) {
                        attachmentsByKey.put(attachment.getKey(), attachment);
                    }
                });

        return referencedAttachmentKeys
                .stream()
                .map(attachmentsByKey::get)
                .filter(Objects::nonNull)
                .map(FormRequestActionNodeV1::createAttachmentOutput)
                .toList();
    }

    private static void collectReferencedAttachmentKeys(
            @Nullable Object value,
            @Nonnull Set<UUID> attachmentKeys
    ) {
        if (value instanceof FileUploadInputElementItem fileItem) {
            addReferencedAttachmentKey(fileItem.getUri(), attachmentKeys);
            return;
        }

        if (value instanceof ReplicatingContainerLayoutElementValue replicatedValue) {
            collectReferencedAttachmentKeys(replicatedValue.getValues(), attachmentKeys);
            return;
        }

        if (value instanceof Map<?, ?> map) {
            var uri = map.get("uri");
            if (uri instanceof String stringUri) {
                addReferencedAttachmentKey(stringUri, attachmentKeys);
            }
            map.values().forEach(child -> collectReferencedAttachmentKeys(child, attachmentKeys));
            return;
        }

        if (value instanceof Iterable<?> iterable) {
            iterable.forEach(child -> collectReferencedAttachmentKeys(child, attachmentKeys));
        }
    }

    private static void addReferencedAttachmentKey(
            @Nullable String uri,
            @Nonnull Set<UUID> attachmentKeys
    ) {
        if (uri == null || !uri.startsWith(FileUploadMultipartInputService.PROCESS_INSTANCE_ATTACHMENT_URI_PREFIX)) {
            return;
        }

        var rawAttachmentKey = uri
                .substring(FileUploadMultipartInputService.PROCESS_INSTANCE_ATTACHMENT_URI_PREFIX.length())
                .trim();
        try {
            attachmentKeys.add(UUID.fromString(rawAttachmentKey));
        } catch (IllegalArgumentException ignored) {
            // Invalid or stale attachment references are not exposed as task outputs.
        }
    }

    @Nonnull
    private static Map<String, Object> createAttachmentOutput(@Nonnull ProcessInstanceAttachmentEntity attachment) {
        var attachmentOutput = new LinkedHashMap<String, Object>();
        attachmentOutput.put("key", attachment.getKey());
        attachmentOutput.put("fileName", attachment.getFileName());
        attachmentOutput.put("originalFileName", attachment.getOriginalFileName());
        attachmentOutput.put("group", attachment.getGroup());
        attachmentOutput.put("storageProviderId", attachment.getStorageProviderId());
        attachmentOutput.put("storagePathFromRoot", attachment.getStoragePathFromRoot());
        return attachmentOutput;
    }

    @Nonnull
    @Override
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        configuration.remove(NodeConfig.RECIPIENT_IDENTITY_ID_FIELD_ID);
        configuration.remove(NodeConfig.RECIPIENT_EMAIL_ADDRESS_FIELD_ID);
        configuration.remove(NodeConfig.NEW_IDENTITIES_FIELD_ID);
        configuration.remove(SemiAutomaticMessageConfig.ManualContent.ASSIGNMENT_FIELD_ID);
        configuration.remove(SemiAutomaticMessageConfig.LayoutConfig.SIGNATURE_DEPARTMENT_FIELD_ID_1);
        configuration.remove(SemiAutomaticMessageConfig.LayoutConfig.SIGNATURE_DEPARTMENT_FIELD_ID_2);
        return configuration;
    }

    @Nonnull
    private String requireRecipientIdentity(@Nullable String recipientIdentity) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var normalizedIdentity = StringUtils.toNullableTrimmedString(recipientIdentity);
        if (normalizedIdentity == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Für die Formularanforderung muss eine Empfängeridentität konfiguriert sein.");
        }
        return normalizedIdentity;
    }

    @Nonnull
    private static String requireRecipientMode(@Nullable String configuredMode)
            throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var mode = StringUtils.toNullableTrimmedString(configuredMode);
        if (mode == null || RECIPIENT_MODE_EXISTING.equals(mode)) {
            return RECIPIENT_MODE_EXISTING;
        }
        if (RECIPIENT_MODE_NEW.equals(mode)) {
            return RECIPIENT_MODE_NEW;
        }
        throw new ProcessNodeExecutionExceptionInvalidConfiguration("Die konfigurierte Empfängerart ist ungültig.");
    }

    @Nonnull
    private static IdentityConfigElementSlot requireNewIdentitySlot(@Nonnull NodeConfig configuration)
            throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (configuration.newIdentities == null || configuration.newIdentities.size() != 1
                || configuration.newIdentities.getFirst() == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Für neue Identitäten muss genau eine Identität konfiguriert sein."
            );
        }
        var slot = configuration.newIdentities.getFirst();
        if (!isValidNewIdentityId(slot.getId()) || StringUtils.isNullOrEmpty(slot.getTitle())
                || Boolean.TRUE.equals(slot.getIsOptional())) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die neue Identität benötigt einen Schlüssel aus 1 bis 32 Buchstaben, Zahlen oder Unterstrichen, einen Titel und muss verpflichtend sein."
            );
        }
        var hasProvider = Optional.ofNullable(slot.getOptions()).orElse(List.of()).stream()
                .anyMatch(option -> option != null && option.getIdentityProviderKey() != null);
        if (!Boolean.TRUE.equals(slot.getAllowsMail()) && !hasProvider) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Für die neue Identität muss ein Identitätsanbieter oder die direkte E-Mail-Eingabe aktiviert sein."
            );
        }
        return slot;
    }

    @Nonnull
    private static String requireInvitationEmail(@Nullable String emailAddress)
            throws ProcessNodeExecutionExceptionInvalidConfiguration {
        try {
            return EmailAddressUtils.normalizeSingleAddress(emailAddress);
        } catch (IllegalArgumentException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die E-Mail-Adresse der Einladung ist ungültig."
            );
        }
    }

    @Nonnull
    private static String requireInvitationEmailForStaffView(@Nonnull NodeConfig configuration) throws ResponseException {
        try {
            return requireInvitationEmail(configuration.recipientEmailAddress);
        } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
            throw ResponseException.internalServerError(e, e.getMessage());
        }
    }

    @Nonnull
    private static IdentityConfigElementSlot requireNewRecipientConfiguration(
            @Nonnull NodeConfig configuration,
            @Nonnull ProcessInstanceEntity processInstance
    ) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var slot = requireNewIdentitySlot(configuration);
        if (!hasIdentity(processInstance, slot.getId())) {
            requireInvitationEmail(configuration.recipientEmailAddress);
        }
        return slot;
    }

    private static boolean hasIdentity(@Nonnull ProcessInstanceEntity processInstance, @Nonnull String identityId) {
        return processInstance.getIdentities() != null && processInstance.getIdentities().containsKey(identityId);
    }

    private static boolean isValidNewIdentityId(@Nullable String identityId) {
        return identityId != null && NEW_IDENTITY_ID_PATTERN.matcher(identityId).matches();
    }

    @Nonnull
    private static ElementVisibilityFunctions recipientModeVisibility(@Nonnull String expectedMode) {
        return ElementVisibilityFunctions.of(NoCodeExpression.of(
                NoCodeEqualsOperator.OPERATOR_ID,
                NoCodeReference.of(NodeConfig.RECIPIENT_MODE_FIELD_ID),
                NoCodeStaticValue.of(expectedMode)
        )).recalculateReferencedIds();
    }

    @Nonnull
    private SemiAutomaticMessageConfig.AutomaticContent requireAutomaticContent(
            @Nonnull NodeConfig configuration
    ) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var content = configuration.messageConfig == null ? null : configuration.messageConfig.automaticContent;
        if (content == null || StringUtils.isNullOrEmpty(content.subject) || StringUtils.isNullOrEmpty(content.content)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Für den automatischen Versand müssen Betreff und Nachrichtentext konfiguriert sein."
            );
        }
        return content;
    }

    @Nonnull
    private SemiAutomaticMessageConfig.ManualContent requireManualContent(
            @Nonnull NodeConfig configuration
    ) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var content = configuration.messageConfig == null ? null : configuration.messageConfig.manualContent;
        if (content == null || StringUtils.isNullOrEmpty(content.subject) || StringUtils.isNullOrEmpty(content.content)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Für den manuellen Versand müssen Vorlagen für Betreff und Nachrichtentext konfiguriert sein."
            );
        }
        return content;
    }

    @Nonnull
    private SemiAutomaticMessageConfig.ManualContent requireManualContentForStaffView(
            @Nonnull NodeConfig configuration
    ) throws ResponseException {
        try {
            return requireManualContent(configuration);
        } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
            throw ResponseException.internalServerError(e, e.getMessage());
        }
    }

    private static void validateStaffMessage(
            @Nullable String subject,
            @Nullable String content
    ) throws ResponseException {
        var derivedRuntimeData = new DerivedRuntimeElementData();
        if (subject == null) {
            derivedRuntimeData.getElementStates().put(
                    STAFF_TASK_SUBJECT_FIELD_ID,
                    new ComputedElementState().setError("Der Betreff der Formularanforderung darf nicht leer sein.")
            );
        }
        if (content == null) {
            derivedRuntimeData.getElementStates().put(
                    STAFF_TASK_CONTENT_FIELD_ID,
                    new ComputedElementState().setError("Die Nachricht der Formularanforderung darf nicht leer sein.")
            );
        }
        if (derivedRuntimeData.hasAnyError()) {
            throw ResponseException.badRequest(derivedRuntimeData);
        }
    }

    /**
     * Configuration for sending a form request to an existing identity or inviting a new one.
     */
    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class NodeConfig {
        public static final String RECIPIENT_MODE_FIELD_ID = "recipientMode";
        public static final String RECIPIENT_IDENTITY_ID_FIELD_ID = "recipientIdentityId";
        public static final String RECIPIENT_EMAIL_ADDRESS_FIELD_ID = "recipientEmailAddress";
        public static final String NEW_IDENTITIES_FIELD_ID = "newIdentities";
        public static final String UI_DEFINITION_FIELD_ID = "uiDefinition";

        /**
         * Missing mode in older process definitions retains the existing-identity behavior.
         */
        @InputElementPOJOBinding(id = RECIPIENT_MODE_FIELD_ID, type = ElementType.Radio, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Empfänger:in"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie, ob die Datenanforderung an eine existierende Identität oder per E-Mail an eine neue Identität gesendet wird."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String recipientMode;

        /**
         * Logical process identity receiving the form request in the existing-identity mode. A missing identity or
         * one that is not present in the process instance prevents dispatch.
         */
        @InputElementPOJOBinding(id = RECIPIENT_IDENTITY_ID_FIELD_ID, type = ElementType.ProcessIdentityIdInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Identität"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie die Identität aus, an welche die Nachricht gesendet wird."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "requiresCommunication", boolValue = true)
        })
        public String recipientIdentityId;

        /**
         * Delivery address used only until the configured identity exists; it is never stored as a process identity.
         */
        @InputElementPOJOBinding(id = RECIPIENT_EMAIL_ADDRESS_FIELD_ID, type = ElementType.Text,
                dynamicText = true,
                allowedInputModes = {InputMode.Literal, InputMode.Variable, InputMode.NoCode, InputMode.LowCode},
                properties = {
                        @ElementPOJOBindingProperty(key = "label", strValue = "E-Mail-Adresse"),
                        @ElementPOJOBindingProperty(key = "hint", strValue = "Die E-Mail-Adresse der Empfänger:in, welche Ihre Identität nachweisen und anschließend die Aufgabe bearbeiten soll."),
                        @ElementPOJOBindingProperty(key = "required", boolValue = true)
                })
        public String recipientEmailAddress;

        /**
         * Exactly one required identity is collected before the first submission and reused on later executions.
         */
        @InputElementPOJOBinding(id = NEW_IDENTITIES_FIELD_ID, type = ElementType.IdentityConfig, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Neue Identität"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Konfigurieren Sie die Identität, die vor der Bearbeitung der Aufgabe angegeben werden muss."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "maxSlots", intValue = 1),
                @ElementPOJOBindingProperty(key = "optionalSlotsAllowed", falseValue = true)
        })
        public List<IdentityConfigElementSlot> newIdentities;

        /**
         * UI-Definition for the form request. This is a required field and must be a valid UI definition element.
         */
        @InputElementPOJOBinding(id = UI_DEFINITION_FIELD_ID, type = ElementType.UiDefinitionInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Formular"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Konfiguration des Formulars."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public GroupLayoutElement uiDefinition;

        public SemiAutomaticMessageConfig.LayoutConfig messageConfig;
    }
}
