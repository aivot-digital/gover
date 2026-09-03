package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.zxing.WriterException;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.ComputedElementState;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.prosuna.backend.elements.models.elements.LayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.AssignmentContextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.AssignmentContextInputElementValue;
import de.aivot.prosuna.backend.elements.models.elements.form.input.DomainAndUserSelectProcessAccessConstraint;
import de.aivot.prosuna.backend.elements.models.elements.form.input.PaymentConfigElementValue;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RadioInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RadioInputElementOption;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.uiPresets.PaymentGroupPreset;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.enums.XBezahldienstStatus;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.payment.entities.PaymentProviderEntity;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.exceptions.PaymentException;
import de.aivot.prosuna.backend.payment.models.PaymentPayload;
import de.aivot.prosuna.backend.payment.models.PaymentTaskRuntimeDataKeys;
import de.aivot.prosuna.backend.payment.repositories.PaymentProviderRepository;
import de.aivot.prosuna.backend.payment.services.PaymentPayloadCreationService;
import de.aivot.prosuna.backend.payment.services.PaymentProviderDefinitionsService;
import de.aivot.prosuna.backend.payment.services.PaymentTransactionService;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.NoCodeEqualsOperator;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.*;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeOutput;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.TaskViewEvent;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultCommunicationRequest;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultNoop;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultPaymentRequested;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskAssigned;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUICustomer;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUIStaff;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.services.AssignmentContextAssigneeResolverService;
import de.aivot.prosuna.backend.process.services.TemplateRenderService;
import de.aivot.prosuna.backend.utils.NumberUtils;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PaymentRequestActionNodeV1 implements ProcessNodeDefinition<PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig> {
    public static final String NODE_KEY = "payment_request";

    private static final String PORT_PAID = "paid";

    private static final String OUTPUT_RECIPIENT_IDENTITY_ID = "recipientIdentityId";
    private static final String OUTPUT_PAYMENT_URL = "paymentUrl";
    private static final String OUTPUT_PAYMENT_PROVIDER_NAME = "paymentProviderName";
    private static final String OUTPUT_PAYMENT_TRANSACTION_KEY = "paymentTransactionKey";
    private static final String OUTPUT_PAYMENT_PURPOSE = "paymentPurpose";
    private static final String OUTPUT_PAYMENT_DESCRIPTION = "paymentDescription";
    private static final String OUTPUT_PAYMENT_TOTAL = "paymentTotal";
    private static final String OUTPUT_PAYMENT_STATUS = "paymentStatus";
    private static final String OUTPUT_PAYMENT_DETAILS = "paymentDetails";
    private static final String OUTPUT_PAYMENT_STATUS_TYPE_DEFINITION =
            "\"INITIAL\" | \"PAYED\" | \"FAILED\" | \"CANCELED\"";
    private static final String OUTPUT_PAYMENT_DETAILS_TYPE_DEFINITION =
            "{ transactionUrl: string | null; transactionRedirectUrl: string | null; " +
                    "transactionId: string | null; transactionReference: string | null; " +
                    "transactionTimestamp: string | null; " +
                    "paymentMethod: \"GIROPAY\" | \"PAYDIRECT\" | \"CREDITCARD\" | \"PAYPAL\" | \"OTHER\" | null; " +
                    "paymentMethodDetail: string | null; " +
                    "status: " + OUTPUT_PAYMENT_STATUS_TYPE_DEFINITION + " | null; " +
                    "statusDetail: string | null; } | null";

    private static final String STAFF_TASK_ROOT_ID = "root";
    private static final String STAFF_TASK_PAYMENT_INFORMATION_ID = "payment-information";
    private static final String STAFF_TASK_SUBJECT_FIELD_ID = "subject";
    private static final String STAFF_TASK_CONTENT_FIELD_ID = "body";
    private static final String STAFF_TASK_SEND_EVENT = "send";

    private final PaymentPayloadCreationService paymentPayloadCreationService;
    private final PaymentTransactionService paymentTransactionService;
    private final PaymentProviderRepository paymentProviderRepository;
    private final PaymentProviderDefinitionsService paymentProviderDefinitionsService;
    private final TemplateRenderService templateRenderService;
    private final ProsunaConfig prosunaConfig;
    private final JsonMapper jsonMapper;
    private final AssignmentContextAssigneeResolverService assignmentContextAssigneeResolverService;

    public PaymentRequestActionNodeV1(PaymentPayloadCreationService paymentPayloadCreationService,
                                      PaymentTransactionService paymentTransactionService,
                                      PaymentProviderRepository paymentProviderRepository,
                                      PaymentProviderDefinitionsService paymentProviderDefinitionsService,
                                      TemplateRenderService templateRenderService,
                                      ProsunaConfig prosunaConfig,
                                      JsonMapper jsonMapper,
                                      AssignmentContextAssigneeResolverService assignmentContextAssigneeResolverService) {
        this.paymentPayloadCreationService = paymentPayloadCreationService;
        this.paymentTransactionService = paymentTransactionService;
        this.paymentProviderRepository = paymentProviderRepository;
        this.paymentProviderDefinitionsService = paymentProviderDefinitionsService;
        this.templateRenderService = templateRenderService;
        this.prosunaConfig = prosunaConfig;
        this.jsonMapper = jsonMapper;
        this.assignmentContextAssigneeResolverService = assignmentContextAssigneeResolverService;
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
        return "Zahlung anfordern";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Versendet eine Zahlungsaufforderung automatisch oder nach manueller Bearbeitung und wartet auf den Zahlungseingang.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                Erstellt eine Online-Zahlung und sendet den Zahlungslink über den für eine Prozessidentität ausgewählten Kommunikationsweg.

                Betreff und Nachricht werden entweder automatisch aus Vorlagen erzeugt oder vor dem Versand durch eine Mitarbeiter:in bearbeitet. Anschließend wartet das Prozesselement auf den erfolgreichen Abschluss der Zahlung.
                """;
    }

    @Nonnull
    @Override
    public Class<PaymentRequestActionNodeConfig> getNodeConfigurationClass() {
        return PaymentRequestActionNodeConfig.class;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper
                    .createFromPOJO(PaymentRequestActionNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Fehler bei der Erstellung des Konfigurationslayouts: %s", e.getMessage());
        }

        layout
                .findChild(PaymentRequestActionNodeConfig.EXECUTION_TYPE_FIELD_ID, RadioInputElement.class)
                .ifPresent(executionType -> executionType.setOptions(List.of(
                        RadioInputElementOption.of(PaymentRequestActionNodeConfig.EXECUTION_TYPE_AUTOMATIC, "Automatisch versenden"),
                        RadioInputElementOption.of(PaymentRequestActionNodeConfig.EXECUTION_TYPE_MANUAL, "Vor dem Versand bearbeiten")
                )));

        layout
                .findChild(AutomaticContent.GROUP_ID, GroupLayoutElement.class)
                .ifPresent(group -> group.setVisibility(createExecutionTypeVisibility(
                        PaymentRequestActionNodeConfig.EXECUTION_TYPE_AUTOMATIC
                )));

        layout
                .findChild(ManualContent.GROUP_ID, GroupLayoutElement.class)
                .ifPresent(group -> group.setVisibility(createExecutionTypeVisibility(
                        PaymentRequestActionNodeConfig.EXECUTION_TYPE_MANUAL
                )));

        layout
                .findChild(ManualContent.ASSIGNMENT_FIELD_ID, AssignmentContextInputElement.class)
                .ifPresent(assignment -> {
                    assignment.setAllowedTypes(List.of(
                            AssignmentContextInputElement.ALLOWED_TYPE_ORG_UNIT,
                            AssignmentContextInputElement.ALLOWED_TYPE_TEAM,
                            AssignmentContextInputElement.ALLOWED_TYPE_USER
                    ));
                    assignment.setProcessAccessConstraint(new DomainAndUserSelectProcessAccessConstraint()
                            .setProcessId(context.processDefinition().getId())
                            .setProcessVersion(context.processDefinitionVersion().getProcessVersion())
                            .setRequiredPermissions(List.of(ProcessPermissionProvider.PROCESS_INSTANCE_EDIT_TASK)));
                });

        return layout;
    }

    @Nonnull
    private static ElementVisibilityFunctions createExecutionTypeVisibility(@Nonnull String executionType) {
        return ElementVisibilityFunctions
                .of(NoCodeExpression.of(
                        NoCodeEqualsOperator.OPERATOR_ID,
                        new NoCodeReference(PaymentRequestActionNodeConfig.EXECUTION_TYPE_FIELD_ID),
                        new NoCodeStaticValue(executionType)
                ))
                .recalculateReferencedIds();
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_PAID,
                        "Zahlung erfolgreich",
                        "Der Prozess wird fortgesetzt, nachdem die Zahlung erfolgreich abgeschlossen wurde."
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
                        "Die ID der Prozessidentität, an die die Zahlungsaufforderung gesendet wurde.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PAYMENT_URL,
                        "Zahlungslink",
                        "Der öffentliche Link zur Zahlungsaufgabe.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PAYMENT_PROVIDER_NAME,
                        "Zahlungsanbieter",
                        "Der Name des verwendeten Zahlungsanbieters.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PAYMENT_TRANSACTION_KEY,
                        "Transaktionsschlüssel",
                        "Der interne Schlüssel der Zahlungstransaktion.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PAYMENT_PURPOSE,
                        "Buchungszweck",
                        "Der gerenderte Buchungszweck der Zahlungsanforderung.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PAYMENT_DESCRIPTION,
                        "Beschreibung",
                        "Die gerenderte Beschreibung der Zahlungsanforderung.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PAYMENT_TOTAL,
                        "Gesamtbetrag",
                        "Der Gesamtbetrag der Zahlungsanforderung.",
                        "number"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PAYMENT_STATUS,
                        "Zahlungsstatus",
                        "Der Status der Zahlungstransaktion.",
                        OUTPUT_PAYMENT_STATUS_TYPE_DEFINITION
                ),
                new ProcessNodeOutput(
                        OUTPUT_PAYMENT_DETAILS,
                        "Zahlungsdetails",
                        "Die Zahlungsinformationen des Zahlungsanbieters nach Abschluss der Zahlung.",
                        OUTPUT_PAYMENT_DETAILS_TYPE_DEFINITION
                )
        );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<PaymentRequestActionNodeConfig> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();
        var executionType = StringUtils.toNullableTrimmedString(configuration.executionType);

        if (PaymentRequestActionNodeConfig.EXECUTION_TYPE_AUTOMATIC.equals(executionType)) {
            return initAutomatic(context, configuration);
        }
        if (PaymentRequestActionNodeConfig.EXECUTION_TYPE_MANUAL.equals(executionType)) {
            return initManual(context, configuration);
        }

        throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                "Ungültige Ausführungsart für die Zahlungsaufforderung. Erwartet werden entweder %s oder %s. Übergeben wurde: %s",
                StringUtils.quote(PaymentRequestActionNodeConfig.EXECUTION_TYPE_AUTOMATIC),
                StringUtils.quote(PaymentRequestActionNodeConfig.EXECUTION_TYPE_MANUAL),
                StringUtils.quote(executionType)
        );
    }

    @Nonnull
    private ProcessNodeExecutionResult initAutomatic(
            @Nonnull ProcessNodeExecutionInitContext<PaymentRequestActionNodeConfig> context,
            @Nonnull PaymentRequestActionNodeConfig configuration
    ) throws ProcessNodeExecutionException {
        var resolvedConfiguration = resolveRequestConfiguration(
                configuration,
                context.getThisProcessInstance()
        );
        var automaticContent = requireAutomaticContent(configuration);
        var subject = renderRequiredTemplate(
                context.getCurrentProcessExecutionData(),
                automaticContent.subject,
                "Betreff"
        );
        var content = renderRequiredTemplate(
                context.getCurrentProcessExecutionData(),
                automaticContent.content,
                "Nachrichtentext"
        );
        var paymentPayload = createPaymentPayload(
                resolvedConfiguration.paymentConfig(),
                context.getCurrentProcessExecutionData()
        );

        return createPaymentRequest(
                context.getCurrentProcessExecutionData(),
                context.getThisProcessInstance(),
                context.getThisTask(),
                resolvedConfiguration,
                paymentPayload,
                subject,
                content,
                null
        );
    }

    @Nonnull
    private ProcessNodeExecutionResult initManual(
            @Nonnull ProcessNodeExecutionInitContext<PaymentRequestActionNodeConfig> context,
            @Nonnull PaymentRequestActionNodeConfig configuration
    ) throws ProcessNodeExecutionException {
        var resolvedConfiguration = resolveRequestConfiguration(configuration, context.getThisProcessInstance());
        var manualContent = requireManualContent(configuration);
        var paymentPayload = createPaymentPayload(
                resolvedConfiguration.paymentConfig(),
                context.getCurrentProcessExecutionData()
        );

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
        runtimeData.put(PaymentTaskRuntimeDataKeys.PAYMENT_PAYLOAD, paymentPayload);

        return ProcessNodeExecutionResultTaskAssigned
                .of(assigneeUserId)
                .setRuntimeData(runtimeData)
                .setProcessData(context.getCurrentProcessExecutionData().getProcessData());
    }

    @Nonnull
    @Override
    public LayoutElement<?> getStaffTaskView(
            @Nonnull ProcessNodeExecutionContextUIStaff<PaymentRequestActionNodeConfig> context
    ) throws ResponseException {
        var paymentPayload = resolveRuntimePaymentPayloadForStaffView(context);

        var paymentInformation = new RichTextContentElement();
        paymentInformation.setId(STAFF_TASK_PAYMENT_INFORMATION_ID);
        paymentInformation.setContent(createPaymentInformationMarkdown(paymentPayload));

        var subjectField = new TextInputElement();
        subjectField.setId(STAFF_TASK_SUBJECT_FIELD_ID);
        subjectField.setLabel("Betreff der Zahlungsaufforderung");
        subjectField.setRequired(true);

        var contentField = new RichTextInputElement();
        contentField.setId(STAFF_TASK_CONTENT_FIELD_ID);
        contentField.setLabel("Nachricht der Zahlungsaufforderung");
        contentField.setRequired(true);

        var root = new GroupLayoutElement();
        root.setId(STAFF_TASK_ROOT_ID);
        root.setChildren(new LinkedList<>(List.of(paymentInformation, subjectField, contentField)));
        return root;
    }

    @Nonnull
    @Override
    public AuthoredElementValues createDefaultStaffTaskViewData(
            @Nonnull ProcessNodeExecutionContextUIStaff<PaymentRequestActionNodeConfig> context
    ) throws ResponseException {
        var manualContent = requireManualContentForStaffView(context.getConfigurationOfExecutingNode());
        var taskViewData = new AuthoredElementValues();

        try {
            taskViewData.put(
                    STAFF_TASK_SUBJECT_FIELD_ID,
                    templateRenderService.interpolate(context.getCurrentProcessExecutionData(), manualContent.subject)
            );
            taskViewData.put(
                    STAFF_TASK_CONTENT_FIELD_ID,
                    templateRenderService.interpolate(context.getCurrentProcessExecutionData(), manualContent.content)
            );
        } catch (RuntimeException e) {
            throw ResponseException.internalServerError(
                    e,
                    "Die Nachrichtenvorlage der Zahlungsaufforderung konnte nicht gerendert werden: %s",
                    e.getMessage()
            );
        }

        return taskViewData;
    }

    @Nonnull
    @Override
    public List<TaskViewEvent> getStaffTaskViewEvents(
            @Nonnull ProcessNodeExecutionContextUIStaff<PaymentRequestActionNodeConfig> context
    ) {
        return List.of(new TaskViewEvent(
                "Zahlungsaufforderung versenden",
                STAFF_TASK_SEND_EVENT
        ));
    }

    @Nonnull
    @Override
    public Optional<ProcessNodeExecutionResult> onEventFromStaffTaskView(
            @Nonnull ProcessNodeExecutionContextUIStaff<PaymentRequestActionNodeConfig> context,
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
        if (!PaymentRequestActionNodeConfig.EXECUTION_TYPE_MANUAL.equals(
                StringUtils.toNullableTrimmedString(configuration.executionType)
        )) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Zahlungsaufforderung kann nur im manuellen Ausführungsmodus über eine Aufgabe versendet werden."
            );
        }

        var subject = StringUtils.toNullableTrimmedString(update.get(STAFF_TASK_SUBJECT_FIELD_ID));
        var content = StringUtils.toNullableTrimmedString(update.get(STAFF_TASK_CONTENT_FIELD_ID));
        validateStaffMessage(subject, content);

        var resolvedConfiguration = resolveRequestConfiguration(
                configuration,
                context.getThisProcessInstance()
        );
        var paymentPayload = resolveRuntimePaymentPayload(context.getThisTask());
        var result = createPaymentRequest(
                context.getCurrentProcessExecutionData(),
                context.getThisProcessInstance(),
                context.getThisTask(),
                resolvedConfiguration,
                paymentPayload,
                subject,
                content,
                update
        );
        return Optional.of(result);
    }

    @Nullable
    @Override
    public ProcessNodeExecutionResult resume(@Nonnull ProcessNodeExecutionInitContext<PaymentRequestActionNodeConfig> context) throws ProcessNodeExecutionException {
        var transaction = resolveRuntimePaymentTransaction(context);
        var paymentStatus = resolveStatus(transaction);

        if (paymentStatus == XBezahldienstStatus.INITIAL) {
            return new ProcessNodeExecutionResultNoop();
        }

        if (paymentStatus != XBezahldienstStatus.PAYED) {
            throw new ProcessNodeExecutionExceptionIO(
                    "Die Zahlungstransaktion %s wurde nicht erfolgreich abgeschlossen. Status: %s%s",
                    StringUtils.quote(transaction.getKey()),
                    StringUtils.quote(paymentStatus.getKey()),
                    StringUtils.isNotNullOrEmpty(transaction.getPaymentError()) ? " Fehler: " + transaction.getPaymentError() : ""
            );
        }

        var nodeData = new LinkedHashMap<>(context.getThisTask().getNodeData());
        nodeData.put(OUTPUT_PAYMENT_STATUS, paymentStatus.getKey());
        nodeData.put(OUTPUT_PAYMENT_DETAILS, transaction.getPaymentInformation());

        return ProcessNodeExecutionResultTaskCompleted
                .of(PORT_PAID)
                .setRuntimeData(new LinkedHashMap<>(context.getThisTask().getRuntimeData()))
                .setNodeData(nodeData)
                .setProcessData(context.getThisTask().getProcessData());
    }

    @Nonnull
    @Override
    public GroupLayoutElement getCustomerTaskView(@Nonnull ProcessNodeExecutionContextUICustomer<PaymentRequestActionNodeConfig> context) throws ResponseException {
        var paymentTransactionKey = context
                .getThisTask()
                .getRuntimeData()
                .get(PaymentTaskRuntimeDataKeys.PAYMENT_TRANSACTION_KEY);

        if (paymentTransactionKey == null) {
            return ProcessNodeDefinition.super.getCustomerTaskView(context);
        }

        var transaction = paymentTransactionService
                .retrieve(String.valueOf(paymentTransactionKey));

        if (transaction.isEmpty()) {
            return ProcessNodeDefinition.super.getCustomerTaskView(context);
        }

        var paymentPayloadRawData = context
                .getThisTask()
                .getRuntimeData()
                .get(PaymentTaskRuntimeDataKeys.PAYMENT_PAYLOAD);
        if (paymentPayloadRawData == null) {
            return ProcessNodeDefinition.super.getCustomerTaskView(context);
        }

        var paymentPayload = jsonMapper
                .convertValue(paymentPayloadRawData, PaymentPayload.class);

        var paymentProvider = paymentProviderRepository
                .findById(transaction.get().getPaymentProviderKey())
                .orElseThrow(() -> ResponseException.internalServerError(
                        "Der Zahlungsanbieter mit dem Schlüssel %s konnte nicht gefunden werden.".formatted(
                                StringUtils.quote(transaction.get().getPaymentProviderKey().toString())
                        )
                ));

        var paymentProviderDefinition = paymentProviderDefinitionsService
                .getProviderDefinition(
                        paymentProvider.getPaymentProviderDefinitionKey(),
                        paymentProvider.getPaymentProviderDefinitionVersion()
                )
                .orElseThrow(() -> ResponseException.internalServerError(
                        "Die Definition des Zahlungsanbieters %s in Version %s konnte nicht gefunden werden.".formatted(
                                StringUtils.quote(paymentProvider.getPaymentProviderDefinitionKey()),
                                paymentProvider.getPaymentProviderDefinitionVersion()
                        )
                ));

        var paymentConfig = context.getConfigurationOfExecutingNode().payment;
        var successMessage = paymentConfig == null ? null : renderOptionalPaymentMessage(context, paymentConfig.successMessage());
        var failureMessage = paymentConfig == null ? null : renderOptionalPaymentMessage(context, paymentConfig.failureMessage());
        var downloadUrl = createPaymentConfirmationUrl(context);

        try {
            return new PaymentGroupPreset(
                    paymentProvider,
                    paymentProviderDefinition,
                    paymentPayload,
                    transaction.get(),
                    successMessage,
                    failureMessage,
                    downloadUrl
            );
        } catch (IOException | WriterException e) {
            throw ResponseException.internalServerError(e);
        }
    }

    @Nonnull
    @Override
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        configuration.remove(PaymentRequestActionNodeConfig.PAYMENT_FIELD_ID);
        configuration.remove(ManualContent.ASSIGNMENT_FIELD_ID);
        return configuration;
    }

    @Nonnull
    private ResolvedRequestConfiguration resolveRequestConfiguration(
            @Nonnull PaymentRequestActionNodeConfig configuration,
            @Nonnull ProcessInstanceEntity processInstance
    ) throws ProcessNodeExecutionException {
        var paymentConfig = requirePaymentConfig(configuration);
        var recipientIdentityId = requireRecipientIdentity(configuration.recipientIdentityId);
        var recipientIdentity = processInstance.getIdentities().get(recipientIdentityId);
        if (recipientIdentity == null) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Die konfigurierte Empfängeridentität %s ist in der Prozessinstanz nicht vorhanden.",
                    StringUtils.quote(recipientIdentityId)
            );
        }

        return new ResolvedRequestConfiguration(
                recipientIdentityId,
                paymentConfig,
                resolvePaymentProvider(paymentConfig)
        );
    }

    @Nonnull
    private PaymentConfigElementValue requirePaymentConfig(@Nonnull PaymentRequestActionNodeConfig configuration) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (configuration.payment == null || configuration.payment.paymentProviderKey() == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Für die Zahlungsanforderung muss ein Zahlungsanbieter konfiguriert sein.");
        }
        return configuration.payment;
    }

    @Nonnull
    private String requireRecipientIdentity(@Nullable String recipientIdentity) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var normalizedIdentity = StringUtils.toNullableTrimmedString(recipientIdentity);
        if (normalizedIdentity == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Für die Zahlungsanforderung muss eine Empfängeridentität konfiguriert sein.");
        }
        return normalizedIdentity;
    }

    @Nonnull
    private AutomaticContent requireAutomaticContent(
            @Nonnull PaymentRequestActionNodeConfig configuration
    ) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var content = configuration.automaticContent;
        if (content == null || StringUtils.isNullOrEmpty(content.subject) || StringUtils.isNullOrEmpty(content.content)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Für den automatischen Versand müssen Betreff und Nachrichtentext konfiguriert sein."
            );
        }
        return content;
    }

    @Nonnull
    private ManualContent requireManualContent(
            @Nonnull PaymentRequestActionNodeConfig configuration
    ) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var content = configuration.manualContent;
        if (content == null || StringUtils.isNullOrEmpty(content.subject) || StringUtils.isNullOrEmpty(content.content)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Für den manuellen Versand müssen Vorlagen für Betreff und Nachrichtentext konfiguriert sein."
            );
        }
        return content;
    }

    @Nonnull
    private ManualContent requireManualContentForStaffView(
            @Nonnull PaymentRequestActionNodeConfig configuration
    ) throws ResponseException {
        try {
            return requireManualContent(configuration);
        } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
            throw ResponseException.internalServerError(e, e.getMessage());
        }
    }

    @Nonnull
    private String renderRequiredTemplate(
            @Nonnull ProcessExecutionData processExecutionData,
            @Nonnull String template,
            @Nonnull String fieldName
    ) throws ProcessNodeExecutionException {
        final String rendered;
        try {
            rendered = StringUtils.toNullableTrimmedString(
                    templateRenderService.interpolate(processExecutionData, template)
            );
        } catch (RuntimeException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die Vorlage für %s konnte nicht gerendert werden: %s",
                    fieldName,
                    e.getMessage()
            );
        }

        if (rendered == null) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der gerenderte Wert für %s ist leer.",
                    fieldName
            );
        }
        return rendered;
    }

    private static void validateStaffMessage(
            @Nullable String subject,
            @Nullable String content
    ) throws ResponseException {
        var derivedRuntimeData = new DerivedRuntimeElementData();
        if (subject == null) {
            derivedRuntimeData.getElementStates().put(
                    STAFF_TASK_SUBJECT_FIELD_ID,
                    new ComputedElementState().setError("Der Betreff der Zahlungsaufforderung darf nicht leer sein.")
            );
        }
        if (content == null) {
            derivedRuntimeData.getElementStates().put(
                    STAFF_TASK_CONTENT_FIELD_ID,
                    new ComputedElementState().setError("Die Nachricht der Zahlungsaufforderung darf nicht leer sein.")
            );
        }
        if (derivedRuntimeData.hasAnyError()) {
            throw ResponseException.badRequest(derivedRuntimeData);
        }
    }

    @Nonnull
    private PaymentProviderEntity resolvePaymentProvider(@Nonnull PaymentConfigElementValue paymentConfig) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        return paymentProviderRepository
                .findById(paymentConfig.paymentProviderKey())
                .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Der Zahlungsanbieter mit dem Schlüssel %s wurde nicht gefunden.",
                        StringUtils.quote(paymentConfig.paymentProviderKey().toString())
                ));
    }

    @Nonnull
    private PaymentPayload createPaymentPayload(@Nonnull PaymentConfigElementValue paymentConfig,
                                                @Nonnull ProcessExecutionData processExecutionData) throws ProcessNodeExecutionException {
        Optional<PaymentPayload> paymentPayload;
        try {
            paymentPayload = paymentPayloadCreationService
                    .createRequest(paymentConfig, DerivedRuntimeElementData.empty(), processExecutionData);
        } catch (PaymentException e) {
            throw new ProcessNodeExecutionExceptionUnknown(e, "Die Zahlungsanforderung konnte nicht erstellt werden: %s", e.getMessage());
        }

        return paymentPayload
                .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Die Zahlungsanforderung enthält keine zahlbaren Positionen oder ergibt einen Gesamtbetrag von 0,00 Euro."
                ));
    }

    @Nonnull
    private PaymentTransactionEntity createPaymentTransaction(@Nonnull PaymentProviderEntity paymentProvider,
                                                              @Nonnull PaymentPayload paymentPayload,
                                                              @Nonnull String paymentUrl) throws ProcessNodeExecutionExceptionUnknown {
        try {
            return paymentTransactionService
                    .create(paymentProvider, paymentPayload, paymentUrl);
        } catch (PaymentException e) {
            throw new ProcessNodeExecutionExceptionUnknown(e, "Die Zahlungstransaktion konnte nicht erstellt werden: %s", e.getMessage());
        }
    }

    @Nonnull
    private ProcessNodeExecutionResult createPaymentRequest(
            @Nonnull ProcessExecutionData processExecutionData,
            @Nonnull ProcessInstanceEntity processInstance,
            @Nonnull ProcessInstanceTaskEntity task,
            @Nonnull ResolvedRequestConfiguration resolvedConfiguration,
            @Nonnull PaymentPayload paymentPayload,
            @Nonnull String subject,
            @Nonnull String content,
            @Nullable AuthoredElementValues staffTaskViewData
    ) throws ProcessNodeExecutionException {
        var paymentUrl = createPaymentUrl(processInstance, task);
        var transaction = createPaymentTransaction(
                resolvedConfiguration.paymentProvider(),
                paymentPayload,
                paymentUrl
        );

        var runtimeData = new LinkedHashMap<>(task.getRuntimeData());
        runtimeData.put(PaymentTaskRuntimeDataKeys.PAYMENT_PAYLOAD, paymentPayload);
        runtimeData.put(PaymentTaskRuntimeDataKeys.PAYMENT_TRANSACTION_KEY, transaction.getKey());
        if (staffTaskViewData != null) {
            runtimeData.put(STAFF_TASK_VIEW_DATA_RUNTIME_KEY, staffTaskViewData.clone());
        }

        return new ProcessNodeExecutionResultPaymentRequested(
                transaction.getKey(),
                resolvedConfiguration.paymentProvider().getName()
        )
                .setRuntimeData(runtimeData)
                .setNodeData(createNodeData(
                        resolvedConfiguration.recipientIdentityId(),
                        paymentUrl,
                        resolvedConfiguration.paymentProvider(),
                        paymentPayload,
                        transaction
                ))
                .setProcessData(processExecutionData.getProcessData())
                .setCommunicationRequest(new ProcessNodeExecutionResultCommunicationRequest(
                        resolvedConfiguration.recipientIdentityId(),
                        CommunicationMessage.of(subject, content, content),
                        null
                ));
    }

    @Nonnull
    private PaymentPayload resolveRuntimePaymentPayload(
            @Nonnull ProcessInstanceTaskEntity task
    ) throws ProcessNodeExecutionException {
        var paymentPayloadData = task
                .getRuntimeData()
                .get(PaymentTaskRuntimeDataKeys.PAYMENT_PAYLOAD);
        if (paymentPayloadData == null) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Für die manuelle Zahlungsaufforderung wurde keine vorberechnete Zahlung in den Laufzeitdaten gefunden."
            );
        }
        if (paymentPayloadData instanceof PaymentPayload paymentPayload) {
            return paymentPayload;
        }

        try {
            return jsonMapper.convertValue(paymentPayloadData, PaymentPayload.class);
        } catch (RuntimeException e) {
            throw new ProcessNodeExecutionExceptionInvalidDataType(
                    e,
                    "Die vorberechnete Zahlung der manuellen Zahlungsaufforderung konnte nicht gelesen werden."
            );
        }
    }

    @Nonnull
    private PaymentPayload resolveRuntimePaymentPayloadForStaffView(
            @Nonnull ProcessNodeExecutionContextUIStaff<PaymentRequestActionNodeConfig> context
    ) throws ResponseException {
        try {
            return resolveRuntimePaymentPayload(context.getThisTask());
        } catch (ProcessNodeExecutionException e) {
            throw ResponseException.internalServerError(e, e.getMessage());
        }
    }

    @Nonnull
    private static String createPaymentInformationMarkdown(@Nonnull PaymentPayload paymentPayload) {
        var itemMarkdown = paymentPayload
                .getPaymentItems()
                .stream()
                .map(item -> "- %s: %s Euro%s".formatted(
                        item.getDescription(),
                        NumberUtils.formatGermanNumber(item.getTotalPrice(), 2),
                        item.getTaxRate().signum() > 0
                                ? " inkl. %s %% Steuern".formatted(NumberUtils.formatGermanNumber(item.getTaxRate(), 2))
                                : ""
                ))
                .collect(Collectors.joining("\n"));
        var includesTaxes = paymentPayload
                .getPaymentItems()
                .stream()
                .anyMatch(item -> item.getTaxRate().signum() > 0);

        return """
                # Zahlungsinformationen

                **Buchungstext:** %s

                **Beschreibung:** %s

                **Zahlungspositionen:**

                %s

                **Gesamtbetrag:** %s Euro%s
                """.formatted(
                paymentPayload.getPurpose(),
                paymentPayload.getDescription(),
                itemMarkdown,
                NumberUtils.formatGermanNumber(paymentPayload.getTotal(), 2),
                includesTaxes ? " inkl. Steuern" : ""
        );
    }

    @Nonnull
    private PaymentTransactionEntity resolveRuntimePaymentTransaction(@Nonnull ProcessNodeExecutionInitContext<PaymentRequestActionNodeConfig> context) throws ProcessNodeExecutionExceptionMissingValue {
        var transactionKey = context
                .getThisTask()
                .getRuntimeData()
                .get(PaymentTaskRuntimeDataKeys.PAYMENT_TRANSACTION_KEY);

        if (transactionKey == null) {
            throw new ProcessNodeExecutionExceptionMissingValue("Für die Zahlungsanforderung wurde keine Zahlungstransaktion in den Laufzeitdaten gefunden.");
        }

        return paymentTransactionService
                .retrieve(String.valueOf(transactionKey))
                .orElseThrow(() -> new ProcessNodeExecutionExceptionMissingValue(
                        "Die Zahlungstransaktion %s wurde nicht gefunden.",
                        StringUtils.quote(String.valueOf(transactionKey))
                ));
    }

    @Nonnull
    private LinkedHashMap<String, Object> createNodeData(@Nonnull String recipientIdentityId,
                                                         @Nonnull String paymentUrl,
                                                         @Nonnull PaymentProviderEntity paymentProvider,
                                                         @Nonnull PaymentPayload paymentPayload,
                                                         @Nonnull PaymentTransactionEntity transaction) {
        var nodeData = new LinkedHashMap<String, Object>();
        nodeData.put(OUTPUT_RECIPIENT_IDENTITY_ID, recipientIdentityId);
        nodeData.put(OUTPUT_PAYMENT_URL, paymentUrl);
        nodeData.put(OUTPUT_PAYMENT_PROVIDER_NAME, paymentProvider.getName());
        nodeData.put(OUTPUT_PAYMENT_TRANSACTION_KEY, transaction.getKey());
        nodeData.put(OUTPUT_PAYMENT_PURPOSE, paymentPayload.getPurpose());
        nodeData.put(OUTPUT_PAYMENT_DESCRIPTION, paymentPayload.getDescription());
        nodeData.put(OUTPUT_PAYMENT_TOTAL, paymentPayload.getTotal());
        nodeData.put(OUTPUT_PAYMENT_STATUS, resolveStatus(transaction).getKey());
        nodeData.put(OUTPUT_PAYMENT_DETAILS, transaction.getPaymentInformation());
        return nodeData;
    }

    @Nonnull
    private String createPaymentUrl(@Nonnull ProcessInstanceEntity processInstance,
                                    @Nonnull ProcessInstanceTaskEntity task) {
        return prosunaConfig.createUrl(
                "/process/",
                processInstance.getAccessKey(),
                "tasks",
                task.getAccessKey()
        );
    }

    @Nonnull
    private String createPaymentConfirmationUrl(@Nonnull ProcessNodeExecutionContextUICustomer<PaymentRequestActionNodeConfig> context) {
        return prosunaConfig.createUrlWithTrailingSlash(
                "/api/public/processes/",
                context.getThisProcessInstance().getAccessKey(),
                "tasks",
                context.getThisTask().getAccessKey(),
                "payment-confirmation"
        );
    }

    @Nullable
    private String renderOptionalPaymentMessage(@Nonnull ProcessNodeExecutionContextUICustomer<PaymentRequestActionNodeConfig> context,
                                                @Nullable String template) throws ResponseException {
        if (StringUtils.isNullOrEmpty(template)) {
            return null;
        }

        var processExecutionData = new ProcessExecutionData()
                .addProcessData(context.getThisTask().getProcessData());

        try {
            return templateRenderService.interpolate(processExecutionData, template);
        } catch (RuntimeException e) {
            throw ResponseException.internalServerError(
                    e,
                    "Die Vorlage %s konnte nicht gerendert werden.",
                    StringUtils.quote(template)
            );
        }
    }

    @Nonnull
    private static XBezahldienstStatus resolveStatus(@Nonnull PaymentTransactionEntity transaction) {
        var status = transaction.getStatus();
        return status == null ? XBezahldienstStatus.INITIAL : status;
    }

    private record ResolvedRequestConfiguration(
            @Nonnull String recipientIdentityId,
            @Nonnull PaymentConfigElementValue paymentConfig,
            @Nonnull PaymentProviderEntity paymentProvider
    ) {
    }

    /**
     * Configuration for creating a payable customer task and sending its payment request to one process identity.
     */
    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class PaymentRequestActionNodeConfig {
        public static final String RECIPIENT_IDENTITY_ID_FIELD_ID = "recipientIdentityId";
        public static final String PAYMENT_FIELD_ID = "payment";
        public static final String EXECUTION_TYPE_FIELD_ID = "execution_type";
        public static final String EXECUTION_TYPE_AUTOMATIC = "automatic";
        public static final String EXECUTION_TYPE_MANUAL = "manual";

        /**
         * Logical process identity receiving the payment request. A missing identity or an identity that is not
         * present in the process instance prevents initialization or dispatch.
         */
        @InputElementPOJOBinding(id = RECIPIENT_IDENTITY_ID_FIELD_ID, type = ElementType.ProcessIdentityIdInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Empfängeridentität"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Identität, an die die Zahlungsaufforderung über den ausgewählten Kommunikationsweg gesendet wird."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String recipientIdentityId;

        /**
         * Payment provider, purpose, description, requestor mapping and payable items used to create the transaction.
         */
        @InputElementPOJOBinding(id = PAYMENT_FIELD_ID, type = ElementType.PaymentConfig, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Zahlung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Konfiguration der Zahlungsanforderung."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public PaymentConfigElementValue payment;

        /**
         * Dispatch mode. Only {@link #EXECUTION_TYPE_AUTOMATIC} and {@link #EXECUTION_TYPE_MANUAL} are accepted;
         * missing or unknown values fail execution.
         */
        @InputElementPOJOBinding(id = EXECUTION_TYPE_FIELD_ID, type = ElementType.Radio, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Ausführungsart"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Auswahl, ob die Zahlungsaufforderung automatisch versendet oder vorher durch eine Mitarbeiter:in bearbeitet wird."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String executionType;

        /** Configuration used only for automatic dispatch. */
        public AutomaticContent automaticContent;

        /** Configuration used only when a staff member edits and dispatches the message. */
        public ManualContent manualContent;
    }

    /** Message templates used for automatic dispatch. */
    @LayoutElementPOJOBinding(id = AutomaticContent.GROUP_ID, type = ElementType.GroupLayout)
    public static class AutomaticContent {
        public static final String GROUP_ID = "automatic_group";
        public static final String SUBJECT_FIELD_ID = "automatic_subject";
        public static final String CONTENT_FIELD_ID = "automatic_content";

        /** Subject template rendered against the process data immediately before dispatch. */
        @InputElementPOJOBinding(id = SUBJECT_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Betreff der Zahlungsaufforderung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Vorlage für den Betreff. Unterstützt Template-Tags mit Vorgangsdaten."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String subject;

        /** Rich-text template rendered against the process data immediately before dispatch. */
        @InputElementPOJOBinding(id = CONTENT_FIELD_ID, type = ElementType.RichTextInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Nachricht der Zahlungsaufforderung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Vorlage für die Nachricht. Unterstützt Template-Tags mit Vorgangsdaten."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String content;
    }

    /** Message templates and assignment used for staff-assisted dispatch. */
    @LayoutElementPOJOBinding(id = ManualContent.GROUP_ID, type = ElementType.GroupLayout)
    public static class ManualContent {
        public static final String GROUP_ID = "manual_group";
        public static final String SUBJECT_FIELD_ID = "manual_subject";
        public static final String CONTENT_FIELD_ID = "manual_content";
        public static final String ASSIGNMENT_FIELD_ID = "manual_assignment";

        /** Required subject template rendered once to initialize the editable staff task. */
        @InputElementPOJOBinding(id = SUBJECT_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Vorlage für den Betreff"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Vorbelegung des bearbeitbaren Betreffs. Unterstützt Template-Tags mit Vorgangsdaten."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String subject;

        /** Required rich-text template rendered once to initialize the editable staff task. */
        @InputElementPOJOBinding(id = CONTENT_FIELD_ID, type = ElementType.RichTextInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Vorlage für die Nachricht"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Vorbelegung der bearbeitbaren Nachricht. Unterstützt Template-Tags mit Vorgangsdaten."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String content;

        /** Staff assignment context used when the node enters manual mode; null or unresolved values fail assignment. */
        @InputElementPOJOBinding(id = ASSIGNMENT_FIELD_ID, type = ElementType.AssignmentContext, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Verantwortlicher Personenkreis"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Definieren Sie den Personenkreis, der die Zahlungsaufforderung bearbeiten und versenden darf."),
                @ElementPOJOBindingProperty(key = "placeholder", strValue = "Organisationseinheit, Team oder Mitarbeiter:in suchen"),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public AssignmentContextInputElementValue assignmentContext;
    }
}
