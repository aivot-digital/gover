package de.aivot.prosuna.backend.plugins.form.v1.nodes;

import com.google.zxing.WriterException;
import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.enums.ElementDisplayContext;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.ElementValidationFunctions;
import de.aivot.prosuna.backend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.IdentityConfigElementOption;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.UiDefinitionInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.services.AuthoredInputValueService;
import de.aivot.prosuna.backend.elements.uiPresets.PaymentGroupPreset;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.enums.XBezahldienstStatus;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.exceptions.PaymentException;
import de.aivot.prosuna.backend.payment.models.PaymentPayload;
import de.aivot.prosuna.backend.payment.models.PaymentTaskRuntimeDataKeys;
import de.aivot.prosuna.backend.payment.repositories.PaymentProviderRepository;
import de.aivot.prosuna.backend.payment.services.PaymentPayloadCreationService;
import de.aivot.prosuna.backend.payment.services.PaymentProviderDefinitionsService;
import de.aivot.prosuna.backend.payment.services.PaymentTaskAttemptService;
import de.aivot.prosuna.backend.payment.services.PaymentTransactionService;
import de.aivot.prosuna.backend.pdf.enums.FormPdfScope;
import de.aivot.prosuna.backend.plugin.models.PluginComponent;
import de.aivot.prosuna.backend.plugins.core.v1.operators.text.NoCodeRegexMatchOperator;
import de.aivot.prosuna.backend.plugins.form.FormPlugin;
import de.aivot.prosuna.backend.plugins.form.v1.services.FormLayoutCleanerService;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.enums.TemplateOutputFormat;
import de.aivot.prosuna.backend.process.exceptions.*;
import de.aivot.prosuna.backend.process.filters.ProcessNodeFilter;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionSummaryContext;
import de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown;
import de.aivot.prosuna.backend.process.models.*;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultNoop;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultPaymentRequested;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeConfigurationValidationContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionTestingLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUICustomer;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.repositories.ProcessNodeRepository;
import de.aivot.prosuna.backend.process.services.FileUploadMultipartInputService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentSetService;
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.process.services.PublicUrlService;
import de.aivot.prosuna.backend.process.services.TemplateRenderService;
import de.aivot.prosuna.backend.services.PdfService;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.detail;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.text;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.timestamp;

@Component
public class FormTriggerNodeV1 implements ProcessNodeDefinition<FormTriggerConfigV1>, PluginComponent {
    private static final ProcessNodeDeadlineDefinition PAYMENT_DEADLINE = new ProcessNodeDeadlineDefinition(
            "payment", "Zahlung",
            "Zeit für die Zahlung ab Erstellung der Zahlungsanforderung. Bei einem Gesamtbetrag von 0,00 Euro beginnt keine Zahlungsfrist.",
            new ProcessNodePort("payment_overdue", "Fristüberschreitung: Zahlung",
                    "Der Vorgang wird hier fortgesetzt, wenn die Aufgabe bei Ablauf der Zahlungsfrist automatisch beendet wird."));

    public static final String NODE_KEY = "form";
    private static final String PORT_NAME = "input";
    private static final String COPY_VALUE_TEMPLATE_PATH_SEGMENT = "__copy_value__";
    private static final String CUSTOMER_SUMMARY_FILE_NAME = "Kopie der Angaben";
    private static final String CUSTOMER_SUMMARY_FILE_NAME_EXT = CUSTOMER_SUMMARY_FILE_NAME + ".pdf";

    public static final String DATA_KEY_PAYMENT_PAYLOAD = PaymentTaskRuntimeDataKeys.PAYMENT_PAYLOAD;
    public static final String DATA_KEY_PAYMENT_TRANSACTION_KEY = PaymentTaskRuntimeDataKeys.PAYMENT_TRANSACTION_KEY;

    public static final String DATA_KEY_PAYLOAD = "payload";
    public static final String DATA_KEY_UNMAPPED = "unmapped";
    public static final String DATA_KEY_ATTACHMENTS = "attachments";
    public static final String DATA_KEY_STARTED = "started";
    public static final String DATA_KEY_CUSTOMER_SUMMARY_FILES = "customerSummaryFiles";
    public static final String DATA_KEY_PAYMENT_DETAILS = "paymentDetails";
    private static final String OUTPUT_ATTACHMENTS_TYPE_DEFINITION =
            "Array<{ key: string; fileName: string; originalFileName: string; group: string | null; " +
                    "storageProviderId: number; storagePathFromRoot: string; }>";
    private static final String OUTPUT_FILES_TYPE_DEFINITION =
            "Array<{ name: string; originalFileName: string; uri: string; size: number; }>";
    private static final String OUTPUT_PAYMENT_DETAILS_TYPE_DEFINITION =
            "{ transactionUrl: string | null; transactionRedirectUrl: string | null; " +
                    "transactionId: string | null; transactionReference: string | null; " +
                    "transactionTimestamp: string | null; " +
                    "paymentMethod: \"GIROPAY\" | \"PAYDIRECT\" | \"CREDITCARD\" | \"PAYPAL\" | \"OTHER\" | null; " +
                    "paymentMethodDetail: string | null; " +
                    "status: \"INITIAL\" | \"PAYED\" | \"FAILED\" | \"CANCELED\" | null; " +
                    "statusDetail: string | null; }";

    private final PublicUrlService publicUrlService;
    private final ProcessNodeRepository processNodeRepository;
    private final PaymentPayloadCreationService paymentRequestCreationService;
    private final TemplateRenderService templateRenderService;
    private final PaymentTransactionService paymentTransactionService;
    private final PaymentProviderRepository paymentProviderRepository;
    private final PaymentProviderDefinitionsService paymentProviderDefinitionsService;
    private final ProcessService processService;
    private final ProsunaConfig prosunaConfig;
    private final PdfService pdfService;
    private final ProcessInstanceAttachmentService processInstanceAttachmentService;
    private final ProcessInstanceAttachmentSetService processInstanceAttachmentSetService;
    private final JsonMapper jsonMapper;
    private final AuthoredInputValueService authoredInputValueService;
    private final PaymentTaskAttemptService paymentTaskAttemptService;

    public FormTriggerNodeV1(PublicUrlService publicUrlService,
                             ProcessNodeRepository processNodeRepository,
                             PaymentPayloadCreationService paymentRequestCreationService,
                             TemplateRenderService templateRenderService,
                             PaymentTransactionService paymentTransactionService,
                             PaymentProviderRepository paymentProviderRepository,
                             PaymentProviderDefinitionsService paymentProviderDefinitionsService,
                             ProcessService processService,
                             ProsunaConfig prosunaConfig,
                             PdfService pdfService,
                             ProcessInstanceAttachmentService processInstanceAttachmentService,
                             ProcessInstanceAttachmentSetService processInstanceAttachmentSetService,
                             JsonMapper jsonMapper,
                             AuthoredInputValueService authoredInputValueService,
                             PaymentTaskAttemptService paymentTaskAttemptService) {
        this.publicUrlService = publicUrlService;
        this.processNodeRepository = processNodeRepository;
        this.pdfService = pdfService;
        this.processInstanceAttachmentService = processInstanceAttachmentService;
        this.processInstanceAttachmentSetService = processInstanceAttachmentSetService;
        this.paymentRequestCreationService = paymentRequestCreationService;
        this.templateRenderService = templateRenderService;
        this.paymentTransactionService = paymentTransactionService;
        this.paymentProviderRepository = paymentProviderRepository;
        this.paymentProviderDefinitionsService = paymentProviderDefinitionsService;
        this.processService = processService;
        this.prosunaConfig = prosunaConfig;
        this.jsonMapper = jsonMapper;
        this.authoredInputValueService = authoredInputValueService;
        this.paymentTaskAttemptService = paymentTaskAttemptService;
    }

    @Nonnull
    @Override
    public String generateExecutionSummary(@Nonnull ProcessNodeExecutionSummaryContext<FormTriggerConfigV1> context) {
        var summary = new ExecutionSummaryMarkdown(context);
        var configuration = context.configurationOfExecutingNode();
        var title = configuration.formLayout == null ? null : configuration.formLayout.getPublicTitle();
        if (title == null || title.isBlank()) {
            title = context.thisNode().getName();
        }
        var submittedAt = summary.data(DATA_KEY_STARTED);
        if (timestamp(submittedAt).isEmpty()) {
            submittedAt = context.thisProcessInstance().getStarted();
        }
        return "Der Vorgang wurde" + summary.at(submittedAt) + " durch die Übermittlung des Formulars"
                + (title == null || title.isBlank() ? "" : " „" + text(title) + "“") + " ausgelöst."
                + (summary.data(DATA_KEY_PAYMENT_DETAILS) == null ? "" : " Die Zahlung wurde bestätigt.")
                + detail("Kurzbeschreibung", context.thisNode().getDescription());
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return FormPlugin.PLUGIN_KEY;
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
    public String getName() {
        return "Formulareingang";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Startet einen neuen Vorgang mit den Angaben aus einem übermittelten Formular.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                ### Wofür verwenden Sie dieses Prozesselement?

                Starten Sie einen neuen Vorgang, indem eine Person Angaben über ein Formular übermittelt, beispielsweise eine Anmeldung oder Anfrage. Das Formular gestalten Sie direkt in diesem Prozesselement. Für zusätzliche Angaben zu einem bestehenden Vorgang verwenden Sie „Angaben anfordern“.

                ### Was richten Sie ein?

                Legen Sie das URL-Segment fest und gestalten Sie unter „Formular“ die benötigten Felder, Hinweise und Eingaberegeln. Das URL-Segment bildet zusammen mit der Adresse der Prosuna-Installation und dem URL-Namespace des Prozesses die öffentliche Formularadresse. Die vollständige Adresse können Sie am Feld kopieren. Mehrere Formulareingänge derselben Prozessversion benötigen unterschiedliche URL-Segmente.

                Über den Datenschlüssel eines Eingabeelements legen Sie fest, wo sein Wert später in den Vorgangsdaten steht. Angaben ohne Datenschlüssel bleiben über die Elementdaten nach Element-ID verfügbar.

                Optional richten Sie Identitäten ein, um Angaben über Beteiligte für die weitere Mitwirkung im Vorgang zu erfassen. Dabei bestimmen Sie, welche Angaben verpflichtend sind und welche Anmeldedienste oder E-Mail-Eingaben zugelassen werden. Bei Bedarf konfigurieren Sie außerdem eine Zahlung und eine Zahlungsfrist.

                ### Was passiert bei der Ausführung?

                Die ausfüllende Person übermittelt das Formular. Prosuna prüft die erforderlichen Identitäten und Eingaben, legt den Vorgang an und verarbeitet die vorgesehenen Angaben und Anlagen. Zusätzlich wird eine „Kopie der Angaben“ als PDF erstellt und beim Vorgang gespeichert.

                Ist eine Zahlung erforderlich, wartet die weitere Bearbeitung auf ihre Bestätigung. Danach wird der Vorgang über „Angaben übermittelt“ fortgesetzt. Ohne Zahlung oder bei einem Gesamtbetrag von 0,00 Euro wird dieser Ausgang direkt verwendet. Schlägt die Zahlung fehl oder wird sie abgebrochen, schlägt die Ausführung des Prozesselements fehl.

                ### Welches Ergebnis steht danach bereit?

                Beim regulären Abschluss stehen die Angaben aus Eingabeelementen mit Datenschlüssel in den Vorgangsdaten bereit. In den Elementdaten finden Sie außerdem die zugeordneten Angaben, die Werte nach Element-ID, die übernommenen Anlagen, den Übermittlungszeitpunkt und die PDF-Kopie. Nach einer bestätigten Zahlung stehen auch deren Zahlungsdetails bereit.

                Im Vorgangsverlauf ist erkennbar, durch welches Formular und wann der Vorgang ausgelöst wurde. Während einer erforderlichen Zahlung wird der Wartezustand angezeigt; nach erfolgreichem Abschluss ist auch die Zahlungsbestätigung erkennbar.

                ### Wie sieht ein Beispiel aus?

                Ein Formular nimmt Anmeldungen zu einer Veranstaltung entgegen. Das Feld „Name“ erhält den Datenschlüssel `anmeldung.name`. Nach der Übermittlung steht der Name beim regulären Abschluss unter diesem Pfad in den Vorgangsdaten. Ist eine Teilnahmegebühr von 20,00 Euro eingerichtet, beginnt der nächste Schritt erst nach bestätigter Zahlung. Die Kopie der Angaben bleibt als Anlage beim Vorgang verfügbar.

                ### Was ist zu beachten?

                Der öffentliche Formularlink verwendet die veröffentlichte Prozessversion. Verwenden Sie zum Erproben eines Entwurfs den Testlink aus dem Testmodus; dessen Übermittlung erzeugt einen Testvorgang.

                Die erfassten Identitäten beschreiben Beteiligte. Diese müssen nicht mit der Person übereinstimmen, die das Formular ausfüllt. Eine direkt angegebene E-Mail-Adresse wird dabei nicht durch eine Bestätigungs-E-Mail überprüft.

                Ist „Aufgabe bei Fristüberschreitung beenden“ aktiviert, wird nach Ablauf der Zahlungsfrist über „Fristüberschreitung: Zahlung“ fortgesetzt. Ohne diese Option bleibt die Aufgabe nach Fristablauf wartend. Die Formularübermittlung liegt zu diesem Zeitpunkt bereits vor.
                """;
    }

    @Nonnull
    @Override
    public ProcessNodeType getType() {
        return ProcessNodeType.Trigger;
    }

    @Nonnull
    @Override
    public ProcessNodeExecutionType[] getExecutionTypes() {
        return new ProcessNodeExecutionType[]{ProcessNodeExecutionType.SemiAutomatic};
    }

    @Nonnull
    @Override
    public List<ProcessNodeDeadlineDefinition> getDeadlines(@Nonnull FormTriggerConfigV1 configuration) {
        return configuration.payment != null && configuration.payment.paymentProviderKey() != null
                ? List.of(PAYMENT_DEADLINE) : List.of();
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts(@Nonnull FormTriggerConfigV1 configuration) {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME,
                        "Angaben übermittelt",
                        "Die Formularangaben wurden übernommen. Eine erforderliche Zahlung wurde bestätigt. Der Vorgang wird hier fortgesetzt."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        DATA_KEY_PAYLOAD,
                        "Angaben mit Datenschlüssel",
                        "Die über Datenschlüssel zugeordneten Formularwerte. Beim regulären Abschluss werden sie in die Vorgangsdaten übernommen.",
                        "Record<string, unknown>"
                ),
                new ProcessNodeOutput(
                        DATA_KEY_UNMAPPED,
                        "Werte nach Element-ID",
                        "Die bei der Übermittlung berücksichtigten Werte unter den Kennungen der Formularelemente, unabhängig von einem Datenschlüssel. Enthält auch berechnete Werte.",
                        "Record<string, unknown>"
                ),
                new ProcessNodeOutput(
                        DATA_KEY_ATTACHMENTS,
                        "Anlagen",
                        "Die über dieses Formular übernommenen hochgeladenen Dateien. Die PDF-Kopie der Angaben steht gesondert bereit.",
                        OUTPUT_ATTACHMENTS_TYPE_DEFINITION
                ),
                new ProcessNodeOutput(
                        DATA_KEY_STARTED,
                        "Übermittelt am",
                        "Der beim Beginn der Übernahme festgehaltene Übermittlungszeitpunkt im ISO-Format.",
                        "string"
                ),
                new ProcessNodeOutput(
                        DATA_KEY_CUSTOMER_SUMMARY_FILES,
                        CUSTOMER_SUMMARY_FILE_NAME,
                        "Die erzeugte PDF-Kopie der übermittelten Angaben als Dateiverweise.",
                        OUTPUT_FILES_TYPE_DEFINITION
                ),
                new ProcessNodeOutput(
                        DATA_KEY_PAYMENT_DETAILS,
                        "Zahlungsdetails",
                        "Die gespeicherten Informationen zur bestätigten Zahlung. Nur vorhanden, wenn eine Zahlung bestätigt wurde.",
                        OUTPUT_PAYMENT_DETAILS_TYPE_DEFINITION
                )
        );
    }

    @Nonnull
    @Override
    public ProcessNodeDefinitionMetadata getMetadata(@Nonnull ProcessNodeEntity processNodeEntity,
                                                     @Nonnull FormTriggerConfigV1 configuration,
                                                     @Nonnull ProcessNodeDefinitionMetadata previousMetadata) {
        var pdm = ProcessNodeDefinitionMetadata
                .reuse(previousMetadata)
                .withLayout(configuration.formLayout, processNodeEntity);

        if (configuration.identities != null) {
            for (var identity : configuration.identities) {
                if (identity.getId() == null || identity.getTitle() == null) {
                    continue;
                }

                pdm.addForwardedIdentity(
                        identity.getId(),
                        identity.getTitle(),
                        identity.getDescription(),
                        identity.getOptions() == null
                                ? List.of()
                                : identity.getOptions()
                                        .stream()
                                        .filter(Objects::nonNull)
                                        .map(IdentityConfigElementOption::getIdentityProviderKey)
                                        .filter(Objects::nonNull)
                                        .distinct()
                                        .toList(),
                        processNodeEntity
                );
            }
        }

        pdm.addForwardedAttachmentSet(
                processNodeEntity.getDataKey(),
                CUSTOMER_SUMMARY_FILE_NAME,
                "Zusammenfassung der übermittelten Formulardaten.",
                false,
                processNodeEntity
        );

        return pdm;
    }

    @Nonnull
    @Override
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement config;
        try {
            config = ElementPOJOMapper
                    .createFromPOJO(FormTriggerConfigV1.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerErrorWithDetails(e, "Die Einstellungen für den Formulareingang konnten nicht geladen werden.", null);
        }

        config
                .findChild(FormTriggerConfigV1.FORM_SLUG, TextInputElement.class)
                .ifPresent(field -> {
                    field.setValidation(ElementValidationFunctions.of(
                            NoCodeExpression.of(
                                    NoCodeRegexMatchOperator.OPERATOR_ID,
                                    NoCodeReference.of(FormTriggerConfigV1.FORM_SLUG),
                                    NoCodeStaticValue.of("^[a-z0-9-]+$")
                            ),
                            "Das URL-Segment darf nur Kleinbuchstaben von a bis z, Zahlen und Bindestriche enthalten."
                    ));

                    field.setPrefix(publicUrlService.createProcessNamespaceDisplayPrefix());
                    field.setCopyable(true);
                    field.setCopyValueTemplate(createPublicFormCopyValueTemplate(context.processDefinition()));
                });

        config
                .findChild(FormTriggerConfigV1.FORM_LAYOUT, UiDefinitionInputElement.class)
                .ifPresent(uid -> {
                    uid.setElementType(ElementType.FormLayout);
                    uid.setDisplayContext(ElementDisplayContext.CustomerFacing);
                });


        return config;
    }

    @Nonnull
    private String createPublicFormCopyValueTemplate(@Nonnull ProcessEntity process) {
        return publicUrlService
                .createPublicFormUrl(process, COPY_VALUE_TEMPLATE_PATH_SEGMENT)
                .replace(COPY_VALUE_TEMPLATE_PATH_SEGMENT, TextInputElement.COPY_VALUE_TEMPLATE_PLACEHOLDER);
    }

    @Nullable
    @Override
    public GroupLayoutElement getTestingLayout(@Nonnull ProcessNodeDefinitionTestingLayoutContext<FormTriggerConfigV1> context) throws ResponseException {
        var link = publicUrlService.createPublicFormUrl(
                context.processDefinition(),
                context.configuration().formSlug
        ) + "?" + FormTriggerControllerV1.TEST_CLAIM_QUERY_PARAM + "=" + context.testClaim().getAccessKey();

        var layout = new GroupLayoutElement();
        layout.setId("testing");

        var rtx = new RichTextContentElement();
        rtx.setId("rtx");
        rtx.setContent(String.format("""
                Über diesen Testlink können Sie das Formular ausfüllen und einen Testvorgang starten: [%s](%s).
                """, link, link));

        layout.addChild(rtx);

        return layout;
    }

    @Nullable
    @Override
    public Map<String, List<String>> validateConfiguration(
            @Nonnull ProcessNodeConfigurationValidationContext<FormTriggerConfigV1> context
    ) throws ResponseException {
        // Definition-level checks belong to authoring, not to each execution of a published process.
        if (!context.isAuthoring()) {
            return null;
        }
        var processNodeEntity = context.thisNode();
        var configuration = context.configuration();
        var errors = new LinkedHashMap<String, List<String>>();
        var formSlug = configuration.formSlug;

        if (StringUtils.isNotNullOrEmpty(formSlug)) {
            // The configuration field already reports the format error. Only the cross-node uniqueness check belongs here.
            var duplicateNodeFilter = ProcessNodeFilter
                    .create()
                    .setNotId(processNodeEntity.getId())
                    .setProcessId(processNodeEntity.getProcessId())
                    .setProcessVersion(processNodeEntity.getProcessVersion())
                    .setProcessNodeDefinitionKey(processNodeEntity.getProcessNodeDefinitionKey())
                    .addConfigEquals(FormTriggerConfigV1.FORM_SLUG, formSlug);

            if (processNodeRepository.exists(duplicateNodeFilter.build())) {
                addValidationError(
                        errors,
                        FormTriggerConfigV1.FORM_SLUG,
                        "Das URL-Segment wird in dieser Prozessversion bereits von einem anderen Formulareingang verwendet. Wählen Sie ein anderes Segment."
                );
            }
        }

        // The form layout field reports configuration errors of its elements itself, see UiDefinitionInputElement.

        return errors.isEmpty() ? null : errors;
    }

    private static void addValidationError(@Nonnull Map<String, List<String>> errors,
                                           @Nonnull String fieldId,
                                           @Nonnull String message) {
        errors
                .computeIfAbsent(fieldId, ignored -> new LinkedList<>())
                .add(message);
    }

    @Nonnull
    @Override
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        // Clean the form layout because it has references to system specific resources like department ids.
        var rawLayout = configuration.getLiteral(FormTriggerConfigV1.FORM_LAYOUT);
        var layout = JsonMapperFactory
                .getInstance()
                .convertValue(rawLayout, FormLayoutElement.class);
        var cleanedLayout = FormLayoutCleanerService.clean(layout);
        configuration.putLiteral(FormTriggerConfigV1.FORM_LAYOUT, cleanedLayout);

        // Clean the identities for they are not the same on every system.
        configuration.remove(FormTriggerConfigV1.IDENTITIES);

        // Clean the payment configuration because payment providers are system specific.
        configuration.remove(FormTriggerConfigV1.PAYMENT);

        // Return the cleaned config.
        return configuration;
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<FormTriggerConfigV1> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();
        if (configuration.formLayout == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Hinterlegen Sie ein Formular für den Formulareingang."
            );
        }

        var processInstanceInitialPayload = context
                .getThisProcessInstance()
                .getInitialPayload();

        var nodeData = new LinkedHashMap<>(processInstanceInitialPayload);

        nodeData.put(
                DATA_KEY_CUSTOMER_SUMMARY_FILES,
                createCustomerSummaryFiles(context, configuration, processInstanceInitialPayload)
        );

        var paymentConfig = context
                .getConfigurationOfExecutingNode()
                .payment;

        if (paymentConfig != null && paymentConfig.paymentProviderKey() != null) {
            var paidAttempt = paymentTaskAttemptService.findPaidEarlierAttempt(context.getThisTask());
            if (paidAttempt.isPresent()) {
                paymentTaskAttemptService.logReuse(context.getLogger(), paidAttempt.get());
                return complete(context, nodeData, paidAttempt.get().attempt().getRuntimeData());
            }

            var paymentProvider = paymentProviderRepository
                    .findById(paymentConfig.paymentProviderKey())
                    .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidConfiguration(
                            "Der ausgewählte Zahlungsanbieter wurde nicht gefunden. Zahlungsanbieter: %s.",
                            StringUtils.quote(paymentConfig.paymentProviderKey().toString())
                    ));

            var execData = context
                    .getCurrentProcessExecutionData()
                    .clone()
                    .addProcessData(context.getCurrentProcessExecutionData().getProcessData().get(DATA_KEY_PAYLOAD));

            Optional<PaymentPayload> paymentRequest;
            try {
                paymentRequest = paymentRequestCreationService
                        .createRequest(
                                paymentConfig,
                                DerivedRuntimeElementData.empty(),
                                execData
                        );
            } catch (PaymentException e) {
                throw new ProcessNodeExecutionExceptionUnknown(
                        e,
                        "Die Zahlungsanforderung für das Formular konnte nicht erstellt werden."
                );
            }

            if (paymentRequest.isEmpty()) {
                context
                        .getLogger()
                        .logf(
                                ProcessNodeExecutionLogLevel.Info,
                                false,
                                true,
                                "Keine Zahlung erforderlich",
                                "Es wurde keine Zahlungsanforderung erstellt, weil der Gesamtbetrag 0,00 Euro beträgt."
                        );
            } else {
                PaymentTransactionEntity transaction;
                try {
                    transaction = paymentTransactionService.create(
                            paymentProvider,
                            paymentRequest.get(),
                            prosunaConfig.createUrl("/process/", context.getThisProcessInstance().getAccessKey(), "tasks", context.getThisTask().getAccessKey())
                    );
                } catch (PaymentException e) {
                    throw new ProcessNodeExecutionExceptionUnknown(
                            e,
                            "Die Zahlungsanforderung konnte beim Zahlungsanbieter nicht angelegt werden."
                    );
                }

                return new ProcessNodeExecutionResultPaymentRequested(
                        transaction.getKey(),
                        paymentProvider.getName()
                )
                        .setNewDeadlineKey(PAYMENT_DEADLINE.key())
                        .setNodeData(nodeData)
                        .setRuntimeData(Map.of(
                                DATA_KEY_PAYMENT_PAYLOAD, paymentRequest.get(),
                                DATA_KEY_PAYMENT_TRANSACTION_KEY, transaction.getKey()
                        ));
            }
        }

        return complete(context, nodeData);
    }

    @Nullable
    @Override
    public ProcessNodeExecutionResult resume(@Nonnull ProcessNodeExecutionInitContext<FormTriggerConfigV1> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();
        if (configuration.formLayout == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Hinterlegen Sie ein Formular für den Formulareingang."
            );
        }

        var processInstanceInitialPayload = context
                .getThisProcessInstance()
                .getInitialPayload();

        var currentNodeData = context.getThisTask().getNodeData();
        var nodeData = currentNodeData.isEmpty()
                ? new LinkedHashMap<>(processInstanceInitialPayload)
                : new LinkedHashMap<>(currentNodeData);

        return complete(context, nodeData);
    }

    @Nonnull
    private ProcessNodeExecutionResult complete(@Nonnull ProcessNodeExecutionInitContext<FormTriggerConfigV1> context,
                                                @Nonnull Map<String, Object> nodeData) throws ProcessNodeExecutionException {
        return complete(context, nodeData, context.getThisTask().getRuntimeData());
    }

    @Nonnull
    private ProcessNodeExecutionResult complete(@Nonnull ProcessNodeExecutionInitContext<FormTriggerConfigV1> context,
                                                @Nonnull Map<String, Object> nodeData,
                                                @Nonnull Map<String, Object> runtimeData) throws ProcessNodeExecutionException {
        var processInstanceInitialPayload = context
                .getThisProcessInstance()
                .getInitialPayload();

        var nodeInitialPayloadRaw = processInstanceInitialPayload
                .get(DATA_KEY_PAYLOAD);

        var nodeInitialPayload = new HashMap<String, Object>();
        if (nodeInitialPayloadRaw instanceof Map<?, ?> mInitialPayload) {
            for (var key : mInitialPayload.keySet()) {
                nodeInitialPayload.put(String.valueOf(key), mInitialPayload.get(key));
            }
        }

        if (runtimeData.containsKey(DATA_KEY_PAYMENT_TRANSACTION_KEY)) {
            var txKey = runtimeData
                    .get(DATA_KEY_PAYMENT_TRANSACTION_KEY)
                    .toString();

            var tx = paymentTransactionService
                    .retrieve(txKey)
                    .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidConfiguration(
                            "Die zum Formulareingang gehörende Zahlungsanforderung wurde nicht gefunden. Zahlungsreferenz: %s.",
                            StringUtils.quote(txKey)
                    ));

            // If the transaction is still in the initial state, exit this method and wait for the next resume.
            if (tx.getStatus() == XBezahldienstStatus.INITIAL) {
                return new ProcessNodeExecutionResultNoop();
            }

            if (tx.getStatus() != XBezahldienstStatus.PAYED) {
                var status = tx.getStatus() == XBezahldienstStatus.FAILED ? "Fehlgeschlagen" : "Abgebrochen";
                if (StringUtils.isNotNullOrEmpty(tx.getPaymentError())) {
                    throw new ProcessNodeExecutionExceptionIO(
                            new PaymentException("%s", tx.getPaymentError()),
                            "Die Zahlung wurde nicht erfolgreich abgeschlossen. Status: %s. Zahlungsreferenz: %s.",
                            status,
                            StringUtils.quote(txKey)
                    );
                } else {
                    throw new ProcessNodeExecutionExceptionIO(
                            "Die Zahlung wurde nicht erfolgreich abgeschlossen. Status: %s. Zahlungsreferenz: %s.",
                            status,
                            StringUtils.quote(txKey)
                    );
                }
            } else {
                context
                        .getLogger()
                        .logf(
                                ProcessNodeExecutionLogLevel.Info,
                                false,
                                true,
                                "Zahlung bestätigt",
                                "Die Zahlung wurde bestätigt. Zahlungsreferenz: %s.",
                                StringUtils.quote(txKey)
                        );

                nodeData.put(DATA_KEY_PAYMENT_DETAILS, tx.getPaymentInformation());
            }
        }

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME)
                .setRuntimeData(runtimeData)
                .setNodeData(nodeData)
                .setProcessData(nodeInitialPayload);
    }

    @Nonnull
    private List<?> createCustomerSummaryFiles(@Nonnull ProcessNodeExecutionInitContext<FormTriggerConfigV1> context,
                                               @Nonnull FormTriggerConfigV1 configuration,
                                               @Nonnull Map<String, Object> initialPayload) throws ProcessNodeExecutionException {
        var submission = readSubmission(initialPayload, configuration.formLayout);

        byte[] pdfBytes;
        try {
            pdfBytes = pdfService.generateCustomerSummary(
                    configuration.formLayout,
                    submission,
                    FormPdfScope.Customer,
                    context.getThisProcessInstance(),
                    context.getConfigurationOfExecutingNode(),
                    context.getThisNode()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Die Erstellung der Kopie der Angaben als PDF wurde unterbrochen."
            );
        } catch (IOException | URISyntaxException | ResponseException e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Die Kopie der Angaben konnte nicht als PDF erstellt werden."
            );
        }

        ProcessInstanceAttachmentEntity attachment;
        try {
            var attachmentSet = processInstanceAttachmentSetService.create(
                    new ProcessInstanceAttachmentSetEntity()
                            .setName(CUSTOMER_SUMMARY_FILE_NAME_EXT)
                            .setDataKey(context.getThisNode().getDataKey())
                            .setProcessInstanceId(context.getThisProcessInstance().getId())
                            .setProcessInstanceTaskId(context.getThisTask().getId())
            );

            attachment = processInstanceAttachmentService.create(
                    ProcessInstanceAttachmentEntity.of(
                            CUSTOMER_SUMMARY_FILE_NAME_EXT,
                            1,
                            context.getThisProcessInstance().getId(),
                            context.getThisTask().getId(),
                            pdfBytes
                    ).setAttachmentSetId(attachmentSet.getId())
            );

            return List.of(FileUploadMultipartInputService.buildAttachmentItem(attachment, pdfBytes.length));
        } catch (ResponseException e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Die Kopie der Angaben konnte nicht als Anlage zum Vorgang gespeichert werden."
            );
        }
    }

    @Nonnull
    private AuthoredElementValues readSubmission(@Nonnull Map<String, Object> initialPayload,
                                                 @Nonnull FormLayoutElement formLayout) throws ProcessNodeExecutionException {
        var rawSubmission = initialPayload.get(DATA_KEY_UNMAPPED);
        if (rawSubmission == null) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Die übermittelten Formulardaten für die Kopie der Angaben fehlen."
            );
        }

        try {
            return authoredInputValueService.toLiteralAuthoredElementValues(formLayout, rawSubmission);
        } catch (IllegalArgumentException e) {
            throw new ProcessNodeExecutionExceptionInvalidDataType(
                    e,
                    "Die übermittelten Formulardaten konnten nicht für die Kopie der Angaben verarbeitet werden."
            );
        }
    }

    @Nonnull
    @Override
    public ProcessNodeCustomerView getCustomerTaskView(@Nonnull ProcessNodeExecutionContextUICustomer<FormTriggerConfigV1> context) throws ResponseException {
        var paymentView = createPaymentView(context);
        return paymentView == null
                ? ProcessNodeDefinition.super.getCustomerTaskView(context)
                : paymentView;
    }

    @Nonnull
    @Override
    public ProcessNodeCustomerView getCompletedCustomerTaskView(@Nonnull ProcessNodeExecutionContextUICustomer<FormTriggerConfigV1> context) throws ResponseException {
        var paymentView = createPaymentView(context);
        return paymentView == null
                ? ProcessNodeDefinition.super.getCompletedCustomerTaskView(context)
                : paymentView;
    }

    @Nullable
    private ProcessNodeCustomerView createPaymentView(@Nonnull ProcessNodeExecutionContextUICustomer<FormTriggerConfigV1> context) throws ResponseException {
        var paymentTransactionKey = context
                .getThisTask()
                .getRuntimeData()
                .get(DATA_KEY_PAYMENT_TRANSACTION_KEY);

        if (paymentTransactionKey == null) {
            return null;
        }

        var transaction = paymentTransactionService
                .retrieve(String.valueOf(paymentTransactionKey));

        if (transaction.isEmpty()) {
            return null;
        }

        var paymentPayloadRawData = context
                .getThisTask()
                .getRuntimeData()
                .get(DATA_KEY_PAYMENT_PAYLOAD);
        if (paymentPayloadRawData == null) {
            return null;
        }
        var paymentPayload = jsonMapper
                .convertValue(paymentPayloadRawData, PaymentPayload.class);

        var paymentProvider = paymentProviderRepository
                .findById(transaction.get().getPaymentProviderKey())
                .orElseThrow(() -> ResponseException.internalServerError(
                        "Der ausgewählte Zahlungsanbieter wurde nicht gefunden. Zahlungsanbieter: %s.".formatted(
                                StringUtils.quote(transaction.get().getPaymentProviderKey().toString())
                        )
                ));

        var paymentProviderDefinition = paymentProviderDefinitionsService
                .getProviderDefinition(
                        paymentProvider.getPaymentProviderDefinitionKey(),
                        paymentProvider.getPaymentProviderDefinitionVersion()
                )
                .orElseThrow(() -> ResponseException.internalServerError(
                        "Die Einrichtung des Zahlungsanbieters wurde nicht gefunden. Kennung: %s, Version: %s.".formatted(
                                StringUtils.quote(paymentProvider.getPaymentProviderDefinitionKey()),
                                paymentProvider.getPaymentProviderDefinitionVersion()
                        )
                ));

        var process = processService
                .retrieve(context.getThisProcessInstance().getProcessId())
                .orElseThrow(() -> ResponseException.internalServerError(
                        "Der Prozess mit der ID %s konnte nicht gefunden werden.".formatted(
                                context.getThisProcessInstance().getProcessId()
                        )
                ));

        var config = context.getConfigurationOfExecutingNode();
        var paymentConfig = config.payment;
        var successMessage = paymentConfig == null
                ? null
                : renderOptionalPaymentMessage(context, paymentConfig.successMessage());
        var failureMessage = paymentConfig == null
                ? null
                : renderOptionalPaymentMessage(context, paymentConfig.failureMessage());
        var downloadUrl = prosunaConfig.createUrlWithTrailingSlash(
                "/api/public/form/",
                process.getSlug(),
                config.formSlug,
                "submit",
                context.getThisProcessInstance().getAccessKey(),
                context.getThisTask().getAccessKey(),
                "payment-confirmation"
        );

        try {
            var layout = new PaymentGroupPreset(
                    paymentProvider,
                    paymentProviderDefinition,
                    paymentPayload,
                    transaction.get(),
                    successMessage,
                    failureMessage,
                    downloadUrl
            );
            return ProcessNodeCustomerView.of(context, layout, List.of(), new AuthoredElementValues());
        } catch (IOException | WriterException e) {
            throw ResponseException.internalServerErrorWithDetails(e, "Die Zahlungsansicht konnte nicht erstellt werden.", null);
        }
    }

    @Nullable
    private String renderOptionalPaymentMessage(@Nonnull ProcessNodeExecutionContextUICustomer<FormTriggerConfigV1> context,
                                                @Nullable String template) throws ResponseException {
        if (StringUtils.isNullOrEmpty(template)) {
            return null;
        }

        var processExecutionData = new ProcessExecutionData()
                .addProcessData(context.getThisTask().getProcessData());

        try {
            // The message is shown as Markdown on the payment status page.
            return templateRenderService.interpolate(processExecutionData, template, TemplateOutputFormat.Markdown);
        } catch (RuntimeException e) {
            throw ResponseException.internalServerErrorWithDetails(
                    e,
                    "Die hinterlegte Nachricht zum Zahlungsstatus konnte nicht erstellt werden.",
                    null
            );
        }
    }

    @Nonnull
    @Override
    public Class<FormTriggerConfigV1> getNodeConfigurationClass() {
        return FormTriggerConfigV1.class;
    }
}
