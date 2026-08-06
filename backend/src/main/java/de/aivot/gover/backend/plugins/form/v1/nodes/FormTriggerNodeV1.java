package de.aivot.gover.backend.plugins.form.v1.nodes;

import de.aivot.gover.backend.core.services.ObjectMapperFactory;
import de.aivot.gover.backend.elements.enums.ElementDisplayContext;
import de.aivot.gover.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.gover.backend.elements.models.AuthoredElementValues;
import de.aivot.gover.backend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.gover.backend.elements.models.elements.form.input.FileUploadInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.TextInputElementPattern;
import de.aivot.gover.backend.elements.models.elements.form.input.UiDefinitionInputElement;
import de.aivot.gover.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.gover.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.gover.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.gover.backend.elements.services.ElementDerivationService;
import de.aivot.gover.backend.elements.utils.ElementPOJOMapper;
import de.aivot.gover.backend.elements.utils.ElementStreamUtils;
import de.aivot.gover.backend.enums.ElementType;
import de.aivot.gover.backend.enums.XBezahldienstStatus;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.models.config.GoverConfig;
import de.aivot.gover.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.gover.backend.payment.exceptions.PaymentException;
import de.aivot.gover.backend.payment.models.PaymentPayload;
import de.aivot.gover.backend.payment.repositories.PaymentProviderRepository;
import de.aivot.gover.backend.payment.services.PaymentPayloadCreationService;
import de.aivot.gover.backend.payment.services.PaymentTransactionService;
import de.aivot.gover.backend.pdf.enums.FormPdfScope;
import de.aivot.gover.backend.plugin.models.PluginComponent;
import de.aivot.gover.backend.plugins.form.FormPlugin;
import de.aivot.gover.backend.plugins.form.v1.services.FormLayoutCleanerService;
import de.aivot.gover.backend.process.entities.ProcessEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.gover.backend.process.enums.ProcessNodeType;
import de.aivot.gover.backend.process.exceptions.*;
import de.aivot.gover.backend.process.filters.ProcessNodeFilter;
import de.aivot.gover.backend.process.models.ProcessNodeDefinition;
import de.aivot.gover.backend.process.models.ProcessNodeDefinitionMetadata;
import de.aivot.gover.backend.process.models.ProcessNodeOutput;
import de.aivot.gover.backend.process.models.ProcessNodePort;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResultPaymentRequested;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeDefinitionTestingLayoutContext;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeExecutionContextUICustomer;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.gover.backend.process.repositories.ProcessNodeRepository;
import de.aivot.gover.backend.process.services.FileUploadMultipartInputService;
import de.aivot.gover.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.gover.backend.process.services.ProcessInstanceAttachmentSetService;
import de.aivot.gover.backend.process.services.PublicUrlService;
import de.aivot.gover.backend.services.PdfService;
import de.aivot.gover.backend.utils.NumberUtils;
import de.aivot.gover.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class FormTriggerNodeV1 implements ProcessNodeDefinition<FormTriggerConfigV1>, PluginComponent {
    public static final String NODE_KEY = "form";
    private static final String PORT_NAME = "input";
    private static final String COPY_VALUE_TEMPLATE_PATH_SEGMENT = "__copy_value__";
    private static final String CUSTOMER_SUMMARY_FILE_NAME = "Formularausdruck.pdf";

    public static final String DATA_KEY_PAYMENT_PAYLOAD = "paymentPayload";
    public static final String DATA_KEY_PAYMENT_TRANSACTION_KEY = "paymentTransaction";

    public static final String DATA_KEY_PAYLOAD = "payload";
    public static final String DATA_KEY_UNMAPPED = "unmapped";
    public static final String DATA_KEY_AUTHORED = "authored";
    public static final String DATA_KEY_ATTACHMENTS = "attachments";
    public static final String DATA_KEY_STARTED = "started";
    public static final String DATA_KEY_CUSTOMER_SUMMARY_FILES = "customerSummaryFiles";

    private final PublicUrlService publicUrlService;
    private final ProcessNodeRepository processNodeRepository;
    private final PaymentPayloadCreationService paymentRequestCreationService;
    private final ElementDerivationService elementDerivationService;
    private final PaymentTransactionService paymentTransactionService;
    private final PaymentProviderRepository paymentProviderRepository;
    private final GoverConfig goverConfig;
    private final PdfService pdfService;
    private final ProcessInstanceAttachmentService processInstanceAttachmentService;
    private final ProcessInstanceAttachmentSetService processInstanceAttachmentSetService;

    public FormTriggerNodeV1(PublicUrlService publicUrlService,
                             ProcessNodeRepository processNodeRepository,
                             PaymentPayloadCreationService paymentRequestCreationService,
                             ElementDerivationService elementDerivationService,
                             PaymentTransactionService paymentTransactionService,
                             PaymentProviderRepository paymentProviderRepository,
                             GoverConfig goverConfig,
                             PdfService pdfService,
                             ProcessInstanceAttachmentService processInstanceAttachmentService,
                             ProcessInstanceAttachmentSetService processInstanceAttachmentSetService) {
        this.publicUrlService = publicUrlService;
        this.processNodeRepository = processNodeRepository;
        this.pdfService = pdfService;
        this.processInstanceAttachmentService = processInstanceAttachmentService;
        this.processInstanceAttachmentSetService = processInstanceAttachmentSetService;
        this.paymentRequestCreationService = paymentRequestCreationService;
        this.elementDerivationService = elementDerivationService;
        this.paymentTransactionService = paymentTransactionService;
        this.paymentProviderRepository = paymentProviderRepository;
        this.goverConfig = goverConfig;
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
    public String getDescription() {
        return "Wird durch einen Formulareingang ausgelöst";
    }

    @Nonnull
    @Override
    public ProcessNodeType getType() {
        return ProcessNodeType.Trigger;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME,
                        "Formular eingereicht",
                        "Der Prozess wird mit den eingereichten Formulardaten gestartet."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        DATA_KEY_PAYLOAD,
                        "Zugeordnete Formulardaten",
                        "Enthält alle Formulardaten welche über einen Datenschlüssel zugeordnet wurden."
                ),
                new ProcessNodeOutput(
                        DATA_KEY_UNMAPPED,
                        "Formular-Rohdaten",
                        "Enthält alle Formulardaten unter der jeweiligen Element-ID des Feldes, unabhängig davon, ob ein Element über einen Datenschlüssel zugewiesen wurde oder nicht."
                ),
                new ProcessNodeOutput(
                        DATA_KEY_ATTACHMENTS,
                        "Anlagen",
                        "Eine Liste aller Anlagen, die über dieses Formular hochgeladen wurden."
                ),
                new ProcessNodeOutput(
                        DATA_KEY_STARTED,
                        "Eingangszeitstempel",
                        "Der Zeitstempel des Dateneingangs an den Auslöser"
                ),
                new ProcessNodeOutput(
                        DATA_KEY_CUSTOMER_SUMMARY_FILES,
                        "PDF-Zusammenfassung",
                        "Die erzeugte PDF-Zusammenfassung der eingereichten Formulardaten im Format des Datei-Anlagen-Feldes."
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
                        processNodeEntity
                );
            }
        }

        pdm.addForwardedAttachmentSet(
                processNodeEntity.getDataKey(),
                "PDF-Zusammenfassung",
                "Zusammenfassung der eingereichten Formulardaten.",
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
            throw new RuntimeException(e);
        }

        config
                .findChild(FormTriggerConfigV1.FORM_SLUG, TextInputElement.class)
                .ifPresent(field -> {
                    var pattern = new TextInputElementPattern()
                            .setRegex("^[a-z0-9-]+$")
                            .setMessage("Das URL-Segment des Formulars darf nur aus Kleinbuchstaben, Zahlen und Bindestrichen bestehen.");
                    field.setPattern(pattern);

                    field.setPrefix(publicUrlService.createProcessNamespaceDisplayPrefix());
                    field.setCopyable(true);
                    field.setCopyValueTemplate(createPublicFormCopyValueTemplate(context.processDefinition()));
                });

        config
                .findChild(FormTriggerConfigV1.FORM_LAYOUT, UiDefinitionInputElement.class)
                .ifPresent(uid -> {
                    uid.setElementType(ElementType.FormLayout);
                    uid.setDisplayContext(ElementDisplayContext.CitizenFacing);
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
                Sie können das Formular abrufen unter [%s](%s).
                """, link, link));

        layout.addChild(rtx);

        return layout;
    }

    @Nullable
    @Override
    public Map<String, List<String>> validateConfiguration(@Nonnull ProcessNodeEntity processNodeEntity,
                                                           @Nonnull FormTriggerConfigV1 configuration) throws ResponseException {
        var errors = new LinkedHashMap<String, List<String>>();
        var formSlug = configuration.formSlug;

        if (StringUtils.isNotNullOrEmpty(formSlug)) {
            if (!formSlug.matches("^[a-z0-9-]+$")) {
                addValidationError(
                        errors,
                        FormTriggerConfigV1.FORM_SLUG,
                        "Das URL-Segment des Formulars darf nur aus Kleinbuchstaben, Zahlen und Bindestrichen bestehen."
                );
            }

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
                        "Das URL-Segment des Formulars wird in dieser Prozessversion bereits von einem anderen Formulareingang verwendet."
                );
            }
        }

        var layoutErrors = validateLegacyPublishChecklistFields(configuration.formLayout);
        if (!layoutErrors.isEmpty()) {
            errors.put(FormTriggerConfigV1.FORM_LAYOUT, layoutErrors);
        }

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
    private List<String> validateLegacyPublishChecklistFields(@Nullable FormLayoutElement formLayout) {
        if (formLayout == null) {
            return List.of();
        }

        var errors = new LinkedList<String>();

        if (StringUtils.isNullOrEmpty(formLayout.getPublicTitle())) {
            errors.add("Der öffentliche Titel muss hinterlegt sein.");
        }
        if (formLayout.getLegalSupportDepartmentId() == null) {
            errors.add("Der fachliche Support muss eingerichtet sein.");
        }
        if (formLayout.getTechnicalSupportDepartmentId() == null) {
            errors.add("Der technische Support muss eingerichtet sein.");
        }
        if (formLayout.getImprintDepartmentId() == null) {
            errors.add("Das Impressum muss eingerichtet sein.");
        }
        if (formLayout.getPrivacyDepartmentId() == null) {
            errors.add("Die Datenschutzerklärung muss eingerichtet sein.");
        }
        if (formLayout.getAccessibilityDepartmentId() == null) {
            errors.add("Die Barrierefreiheitserklärung muss eingerichtet sein.");
        }
        ElementStreamUtils.applyAction(formLayout, element -> {
            if (element instanceof FileUploadInputElement uploadElement && StringUtils.isNullOrEmpty(uploadElement.getSubmittedFileName())) {
                var uploadElementLabel = FileUploadMultipartInputService.describeUploadElement(uploadElement);
                var quotedUploadElementLabel = StringUtils.quote(uploadElementLabel);
                var message = String.format(
                        "Für das Anlagen-Feld %s muss ein Dateiname bei Einreichung hinterlegt sein.",
                        quotedUploadElementLabel
                );
                errors.add(message);
            }
        });

        return errors;
    }

    @Nonnull
    @Override
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        // Clean the form layout because it has references to system specific resources like department ids.
        var rawLayout = configuration.get(FormTriggerConfigV1.FORM_LAYOUT);
        var layout = ObjectMapperFactory
                .getInstance()
                .convertValue(rawLayout, FormLayoutElement.class);
        var cleanedLayout = FormLayoutCleanerService.clean(layout);
        configuration.put(FormTriggerConfigV1.FORM_LAYOUT, cleanedLayout);

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
                    "Die Konfiguration des Formulareingangs enthält kein Formular."
            );
        }

        var formLayout = configuration.formLayout;

        var authoredValues = ObjectMapperFactory
                .getInstance()
                .convertValue(
                        context
                                .getThisProcessInstance()
                                .getInitialPayload()
                                .get(DATA_KEY_AUTHORED),
                        AuthoredElementValues.class
                );

        var derived = elementDerivationService
                .derive(formLayout, authoredValues);

        var paymentConfig = context
                .getConfigurationOfExecutingNode()
                .payment;

        if (paymentConfig != null && paymentConfig.paymentProviderKey() != null) {
            var paymentProvider = paymentProviderRepository
                    .findById(paymentConfig.paymentProviderKey())
                    .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidConfiguration(
                            "Der Zahlungsanbieter mit dem Schlüssel %s konnte nicht gefunden werden",
                            StringUtils.quote(paymentConfig.paymentProviderKey().toString())
                    ));

            Optional<PaymentPayload> paymentRequest;
            try {
                paymentRequest = paymentRequestCreationService
                        .createRequest(paymentConfig,
                                derived,
                                context.getCurrentProcessExecutionData());
            } catch (PaymentException e) {
                throw new ProcessNodeExecutionExceptionUnknown(
                        e,
                        "Fehler beim Erstellen der Zahlungsanforderung: %s",
                        e.getMessage()
                );
            }

            if (paymentRequest.isEmpty()) {
                context
                        .getLogger()
                        .logf(
                                ProcessNodeExecutionLogLevel.Info,
                                false,
                                true,
                                "Keine Zahlungsanforderung erstellt",
                                "Es wurde keine Zahlungsanforderung erstellt, da keine Zahlungspositionen mit einem Gesamtwert größer als 0 gefunden wurden."
                        );
            } else {
                PaymentTransactionEntity transaction;
                try {
                    transaction = paymentTransactionService.create(
                            paymentProvider,
                            paymentRequest.get(),
                            goverConfig.createUrl("/process/", context.getThisProcessInstance().getAccessKey(), "tasks", context.getThisTask().getAccessKey())
                    );
                } catch (PaymentException e) {
                    throw new ProcessNodeExecutionExceptionUnknown(
                            e,
                            "Fehler beim Absenden der Zahlungsanforderung: %s",
                            e.getMessage()
                    );
                }

                return new ProcessNodeExecutionResultPaymentRequested(
                        transaction.getKey(),
                        paymentProvider.getName()
                )
                        .setRuntimeData(Map.of(
                                DATA_KEY_PAYMENT_PAYLOAD, paymentRequest.get(),
                                DATA_KEY_PAYMENT_TRANSACTION_KEY, transaction.getKey()
                        ));
            }
        }

        var processInstanceInitialPayload = context
                .getThisProcessInstance()
                .getInitialPayload();

        var nodeData = new LinkedHashMap<>(processInstanceInitialPayload);
        nodeData.put(
                DATA_KEY_CUSTOMER_SUMMARY_FILES,
                createCustomerSummaryFiles(context, configuration, processInstanceInitialPayload)
        );

        var nodeInitialPayloadRaw = processInstanceInitialPayload
                .get(DATA_KEY_PAYLOAD);

        var nodeInitialPayload = new HashMap<String, Object>();
        if (nodeInitialPayloadRaw instanceof Map<?, ?> mInitialPayload) {
            for (var key : mInitialPayload.keySet()) {
                nodeInitialPayload.put(String.valueOf(key), mInitialPayload.get(key));
            }
        }

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME)
                .setNodeData(nodeData)
                .setProcessData(nodeInitialPayload);
    }

    @Nonnull
    private List<?> createCustomerSummaryFiles(@Nonnull ProcessNodeExecutionInitContext<FormTriggerConfigV1> context,
                                               @Nonnull FormTriggerConfigV1 configuration,
                                               @Nonnull Map<String, Object> initialPayload) throws ProcessNodeExecutionException {
        var submission = readSubmission(initialPayload);

        byte[] pdfBytes;
        try {
            pdfBytes = pdfService.generateCustomerSummary(
                    configuration.formLayout,
                    submission,
                    FormPdfScope.Citizen,
                    context.getThisProcessInstance(),
                    context.getConfigurationOfExecutingNode(),
                    context.getThisNode()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Die PDF-Erstellung der Formularzusammenfassung wurde unterbrochen."
            );
        } catch (IOException | URISyntaxException | ResponseException e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Fehler beim Erzeugen der Formularzusammenfassung: %s",
                    e.getMessage()
            );
        }

        ProcessInstanceAttachmentEntity attachment;
        try {
            var attachmentSet = processInstanceAttachmentSetService.create(
                    new ProcessInstanceAttachmentSetEntity()
                            .setName(CUSTOMER_SUMMARY_FILE_NAME)
                            .setDataKey(context.getThisNode().getDataKey())
                            .setProcessInstanceId(context.getThisProcessInstance().getId())
                            .setProcessInstanceTaskId(context.getThisTask().getId())
            );

            attachment = processInstanceAttachmentService.create(
                    ProcessInstanceAttachmentEntity.of(
                            CUSTOMER_SUMMARY_FILE_NAME,
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
                    "Fehler beim Speichern der Formularzusammenfassung als Prozess-Anhang: %s",
                    e.getMessage()
            );
        }
    }

    @Nonnull
    private AuthoredElementValues readSubmission(@Nonnull Map<String, Object> initialPayload) throws ProcessNodeExecutionException {
        var rawSubmission = initialPayload.get(DATA_KEY_UNMAPPED);
        if (rawSubmission == null) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Die Formular-Rohdaten für die PDF-Zusammenfassung fehlen."
            );
        }

        try {
            return ObjectMapperFactory
                    .getNullPreservingInstance()
                    .convertValue(rawSubmission, AuthoredElementValues.class);
        } catch (IllegalArgumentException e) {
            throw new ProcessNodeExecutionExceptionInvalidDataType(
                    e,
                    "Die Formular-Rohdaten konnten nicht für die PDF-Zusammenfassung verarbeitet werden."
            );
        }
    }

    @Nonnull
    @Override
    public GroupLayoutElement getCustomerTaskView(@Nonnull ProcessNodeExecutionContextUICustomer context) throws ResponseException {
        var paymentTransactionKey = context
                .getThisTask()
                .getRuntimeData()
                .get(DATA_KEY_PAYMENT_TRANSACTION_KEY);

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
                .get(DATA_KEY_PAYMENT_PAYLOAD);
        var paymentPayload = ObjectMapperFactory
                .getInstance()
                .convertValue(paymentPayloadRawData, PaymentPayload.class);

        var paymentProvider = paymentProviderRepository
                .findById(transaction.get().getPaymentProviderKey())
                .orElseThrow(() -> ResponseException.internalServerError(
                        "Der Zahlungsanbieter mit dem Schlüssel %s konnte nicht gefunden werden".formatted(
                                StringUtils.quote(transaction.get().getPaymentProviderKey().toString())
                        )
                ));

        String content = switch (transaction.get().getStatus()) {
            case XBezahldienstStatus.INITIAL -> {
                yield """
                        # Zahlung ausstehend
                        Um Ihre Einreichung bearbeiten zu können, ist eine Zahlung von Gebühren erforderlich.
                        Die Zahlung wird durch den **%s** abgewickelt.
                        Bitte achten Sie darauf, dass Sie die Zahlungsinformationen korrekt eingeben und den Vorgang abschließen.

                        Für Ihre Einreichung sind folgende Gebühren zu zahlen:
                        %s

                        Insgesamt zu entrichtende Gebühr: %s Euro inkl. Steuern.

                        Sie können den Betrag über den folgenden Link zahlen: [%s](%s)
                        """
                        .formatted(
                                StringUtils.quote(paymentProvider.getName()),
                                paymentPayload
                                        .getPaymentItems()
                                        .stream()
                                        .map(item -> "- %s: %s Euro%s\n".formatted(
                                                item.getDescription(),
                                                NumberUtils.formatGermanNumber(item.getTotalPrice(), 2),
                                                item.getTaxRate().compareTo(BigDecimal.ZERO) > 0
                                                        ? " inkl. %s Steuern".formatted(NumberUtils.formatGermanNumber(item.getTaxRate(), 2))
                                                        : ""
                                        ))
                                        .collect(Collectors.joining()),
                                NumberUtils.formatGermanNumber(paymentPayload.getTotal(), 2),
                                transaction.get().getPaymentInformation().getTransactionRedirectUrl(),
                                transaction.get().getPaymentInformation().getTransactionRedirectUrl()
                        );
            }
            case XBezahldienstStatus.FAILED -> {
                yield """
                        # Zahlung fehlgeschlagen
                        Die Zahlung konnte nicht erfolgreich abgeschlossen werden.
                        Bitte wenden Sie sich an den Support, um weitere Informationen zu erhalten und die Zahlung erneut zu versuchen.
                        """;
            }
            case XBezahldienstStatus.CANCELED -> {
                yield """
                        # Zahlung abgebrochen
                        Die Zahlung wurde abgebrochen.
                        Bitte wenden Sie sich an den Support, um weitere Informationen zu erhalten und die Zahlung erneut zu versuchen.
                        """;
            }
            case XBezahldienstStatus.PAYED -> {
                yield """
                        # Zahlung erfolgreich
                        Die Zahlung wurde erfolgreich abgeschlossen.
                        Vielen Dank für Ihre Einreichung.
                        """;
            }
        };

        var richtext = new RichTextContentElement();
        richtext.setId("rtx");
        richtext.setContent(content);

        var layout = new GroupLayoutElement();
        layout.setId("grp");
        layout.addChild(richtext);

        return layout;
    }

    @Nonnull
    @Override
    public Class<FormTriggerConfigV1> getNodeConfigurationClass() {
        return FormTriggerConfigV1.class;
    }
}
