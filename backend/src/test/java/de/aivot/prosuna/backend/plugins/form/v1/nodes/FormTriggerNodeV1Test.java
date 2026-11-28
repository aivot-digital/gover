package de.aivot.prosuna.backend.plugins.form.v1.nodes;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.elements.enums.ValidationFunctionType;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.form.content.AlertContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.content.LinkButtonContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.FileUploadInputElementItem;
import de.aivot.prosuna.backend.elements.models.elements.form.input.IdentityConfigElementOption;
import de.aivot.prosuna.backend.elements.models.elements.form.input.IdentityConfigElementSlot;
import de.aivot.prosuna.backend.elements.models.elements.form.input.PaymentConfigElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.PaymentConfigElementValue;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.prosuna.backend.elements.services.AuthoredInputValueService;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.enums.XBezahldienstStatus;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.pdf.enums.FormPdfScope;
import de.aivot.prosuna.backend.payment.entities.PaymentProviderEntity;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.exceptions.PaymentException;
import de.aivot.prosuna.backend.payment.models.PaymentProviderDefinition;
import de.aivot.prosuna.backend.payment.models.PaymentPayload;
import de.aivot.prosuna.backend.payment.services.PaymentTaskAttemptService;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentInformation;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.payment.repositories.PaymentProviderRepository;
import de.aivot.prosuna.backend.payment.services.PaymentPayloadCreationService;
import de.aivot.prosuna.backend.payment.services.PaymentProviderDefinitionsService;
import de.aivot.prosuna.backend.payment.services.PaymentTransactionService;
import de.aivot.prosuna.backend.plugins.core.v1.operators.text.NoCodeRegexMatchOperator;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionIO;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinitionMetadata;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultNoop;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeConfigurationValidationContext;
import de.aivot.prosuna.backend.process.enums.ProcessNodeConfigurationValidationPhase;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultPaymentRequested;
import org.mockito.ArgumentCaptor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FormTriggerNodeV1Test {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final Integer NODE_ID = 123;
    private static final Long PROCESS_INSTANCE_ID = 99L;
    private static final Long TASK_ID = 456L;

    private ProcessNodeRepository processNodeRepository;
    private PdfService pdfService;
    private ProcessInstanceAttachmentService processInstanceAttachmentService;
    private ProcessInstanceAttachmentSetService processInstanceAttachmentSetService;
    private PaymentPayloadCreationService paymentPayloadCreationService;
    private PaymentTransactionService paymentTransactionService;
    private PaymentTaskAttemptService paymentTaskAttemptService;
    private PaymentProviderRepository paymentProviderRepository;
    private PaymentProviderDefinitionsService paymentProviderDefinitionsService;
    private ProcessService processService;
    private TemplateRenderService templateRenderService;
    private FormTriggerNodeV1 node;

    @BeforeEach
    void setUp() {
        processNodeRepository = mock(ProcessNodeRepository.class);
        pdfService = mock(PdfService.class);
        processInstanceAttachmentService = mock(ProcessInstanceAttachmentService.class);
        processInstanceAttachmentSetService = mock(ProcessInstanceAttachmentSetService.class);
        paymentTransactionService = mock(PaymentTransactionService.class);
        paymentTaskAttemptService = mock(PaymentTaskAttemptService.class);
        paymentPayloadCreationService = mock(PaymentPayloadCreationService.class);
        paymentProviderRepository = mock(PaymentProviderRepository.class);
        paymentProviderDefinitionsService = mock(PaymentProviderDefinitionsService.class);
        processService = mock(ProcessService.class);
        templateRenderService = new TemplateRenderService(new JavascriptEngineFactoryService(List.of()));
        node = createNode(mock(PublicUrlService.class));
    }

    @Test
    void offersPaymentDeadlineOnlyWhenPaymentIsConfigured() {
        var configuration = new FormTriggerConfigV1();
        assertTrue(node.getDeadlines(configuration).isEmpty());
        configuration.payment = new PaymentConfigElementValue(null, null, null, null, null, null, null, null);
        assertTrue(node.getDeadlines(configuration).isEmpty());
        configuration.payment = new PaymentConfigElementValue(UUID.randomUUID(), null, null, null, null, null, null, null);
        assertEquals(List.of("payment"), node.getDeadlines(configuration).stream().map(definition -> definition.key()).toList());
        assertEquals("payment_overdue", node.getDeadlines(configuration).getFirst().port().key());
    }

    @Test
    void validateConfiguration_ShouldAllowValidLayoutAndUniqueSlug() throws Exception {
        when(processNodeRepository.exists(anySpecification())).thenReturn(false);

        var errors = node.validateConfiguration(new ProcessNodeConfigurationValidationContext<>(
                processNode(),
                configuration("antrag-online", validFormLayout()),
                DerivedRuntimeElementData.empty(), ProcessNodeConfigurationValidationPhase.Authoring
        ));

        assertNull(errors);
    }

    @Test
    void validateConfiguration_ShouldSkipDefinitionChecksAtRuntime() throws Exception {
        var errors = node.validateConfiguration(new ProcessNodeConfigurationValidationContext<>(
                processNode(), configuration("Invalid Slug", new FormLayoutElement()),
                DerivedRuntimeElementData.empty(), ProcessNodeConfigurationValidationPhase.Runtime
        ));

        assertNull(errors);
        verifyNoInteractions(processNodeRepository);
    }

    @Test
    void getOutputs_ShouldExposeStartedTimestamp() {
        var output = node
                .getOutputs()
                .stream()
                .filter(candidate -> FormTriggerNodeV1.DATA_KEY_STARTED.equals(candidate.key()))
                .findFirst()
                .orElse(null);

        assertNotNull(output);
        assertEquals("Übermittelt am", output.label());
        assertEquals("Der beim Beginn der Übernahme festgehaltene Übermittlungszeitpunkt im ISO-Format.", output.description());
        assertEquals("string", output.typeDefinition());
    }

    @Test
    void getOutputs_ShouldExposeCustomerSummaryFiles() {
        var output = node
                .getOutputs()
                .stream()
                .filter(candidate -> FormTriggerNodeV1.DATA_KEY_CUSTOMER_SUMMARY_FILES.equals(candidate.key()))
                .findFirst()
                .orElse(null);

        assertNotNull(output);
        assertEquals("Kopie der Angaben", output.label());
        assertEquals(
                "Array<{ name: string; originalFileName: string; uri: string; size: number; }>",
                output.typeDefinition()
        );
    }

    @Test
    void getOutputs_ShouldExposePaymentDetailsType() {
        var output = node
                .getOutputs()
                .stream()
                .filter(candidate -> FormTriggerNodeV1.DATA_KEY_PAYMENT_DETAILS.equals(candidate.key()))
                .findFirst()
                .orElse(null);

        assertNotNull(output);
        assertEquals(
                "{ transactionUrl: string | null; transactionRedirectUrl: string | null; " +
                        "transactionId: string | null; transactionReference: string | null; " +
                        "transactionTimestamp: string | null; " +
                        "paymentMethod: \"GIROPAY\" | \"PAYDIRECT\" | \"CREDITCARD\" | \"PAYPAL\" | \"OTHER\" | null; " +
                        "paymentMethodDetail: string | null; " +
                        "status: \"INITIAL\" | \"PAYED\" | \"FAILED\" | \"CANCELED\" | null; " +
                        "statusDetail: string | null; }",
                output.typeDefinition()
        );
    }

    @Test
    void getMetadata_ShouldForwardCustomerSummaryAttachmentSet() {
        var metadata = node.getMetadata(
                processNode(),
                configuration("antrag-online", validFormLayout()),
                ProcessNodeDefinitionMetadata.empty()
        );

        assertEquals(1, metadata.forwardedAttachmentSets().size());

        var attachmentSet = metadata.forwardedAttachmentSets().getFirst();
        assertEquals("formNode", attachmentSet.dataKey());
        assertEquals("Kopie der Angaben", attachmentSet.label());
        assertFalse(attachmentSet.isMultifile());
    }

    @Test
    void getMetadata_ShouldForwardIdentityProviderKeys() {
        var firstProviderKey = UUID.randomUUID();
        var secondProviderKey = UUID.randomUUID();
        var configuration = configuration("antrag-online", validFormLayout());
        configuration.identities = List.of(new IdentityConfigElementSlot(
                "applicant",
                "Antragsteller:in",
                null,
                true,
                false,
                List.of(
                        new IdentityConfigElementOption(firstProviderKey, List.of()),
                        new IdentityConfigElementOption(firstProviderKey, List.of()),
                        new IdentityConfigElementOption(secondProviderKey, List.of())
                )
        ));

        var metadata = node.getMetadata(
                processNode(),
                configuration,
                ProcessNodeDefinitionMetadata.empty()
        );

        assertEquals(1, metadata.forwardedIdentities().size());
        assertEquals(
                List.of(firstProviderKey, secondProviderKey),
                metadata.forwardedIdentities().getFirst().identityProviderKeys()
        );
    }

    @Test
    void getConfigurationLayout_ShouldExposeCopyableSlugUrlTemplate() throws Exception {
        var publicUrlService = new PublicUrlService(prosunaConfig());
        var node = createNode(publicUrlService);

        var layout = node.getConfigurationLayout(configurationLayoutContext());
        var slugField = layout
                .findChild(FormTriggerConfigV1.FORM_SLUG, TextInputElement.class)
                .orElseThrow();

        assertEquals(true, slugField.getCopyable());
        assertEquals("https://example.test/form/antrag-prozess/{value}/", slugField.getCopyValueTemplate());
        var slugValidation = slugField.getValidation();
        assertNotNull(slugValidation);
        assertEquals(ValidationFunctionType.NoCode, slugValidation.getType());
        assertEquals(List.of(FormTriggerConfigV1.FORM_SLUG), List.copyOf(slugValidation.getReferencedIds()));
        assertEquals("Das URL-Segment darf nur Kleinbuchstaben von a bis z, Zahlen und Bindestriche enthalten.",
                slugValidation.getNoCodeList().getFirst().getMessage());
        var slugExpression = assertInstanceOf(
                NoCodeExpression.class,
                slugValidation.getNoCodeList().getFirst().getNoCode()
        );
        assertEquals(NoCodeRegexMatchOperator.OPERATOR_ID, slugExpression.getOperatorIdentifier());
        assertEquals(
                List.of(
                        NoCodeReference.of(FormTriggerConfigV1.FORM_SLUG),
                        NoCodeStaticValue.of("^[a-z0-9-]+$")
                ),
                List.copyOf(slugExpression.getOperands())
        );
    }

    @Test
    void getConfigurationLayout_ShouldExposePaymentConfigBelowIdentityConfig() throws Exception {
        var publicUrlService = new PublicUrlService(prosunaConfig());
        var node = createNode(publicUrlService);

        var layout = node.getConfigurationLayout(configurationLayoutContext());
        var children = layout.getChildren();

        assertEquals(FormTriggerConfigV1.IDENTITIES, children.get(2).getId());
        assertEquals(FormTriggerConfigV1.PAYMENT, children.get(3).getId());
        assertTrue(layout.findChild(FormTriggerConfigV1.PAYMENT, PaymentConfigElement.class).isPresent());
    }

    @Test
    void validateConfiguration_ShouldLeaveFormLayoutChecksToTheElements() throws Exception {
        when(processNodeRepository.exists(anySpecification())).thenReturn(false);

        // The form layout field reports a missing public title through the element configuration validation.
        var errors = node.validateConfiguration(new ProcessNodeConfigurationValidationContext<>(
                processNode(),
                configuration("antrag-online", new FormLayoutElement()),
                DerivedRuntimeElementData.empty(), ProcessNodeConfigurationValidationPhase.Authoring
        ));

        assertNull(errors);
    }

    @Test
    void validateConfiguration_ShouldRejectDuplicateSlug() throws Exception {
        when(processNodeRepository.exists(anySpecification())).thenReturn(true);

        var errors = node.validateConfiguration(new ProcessNodeConfigurationValidationContext<>(
                processNode(),
                configuration("antrag-online", validFormLayout()),
                DerivedRuntimeElementData.empty(), ProcessNodeConfigurationValidationPhase.Authoring
        ));

        assertNotNull(errors);
        assertEquals(
                List.of("Das URL-Segment wird in dieser Prozessversion bereits von einem anderen Formulareingang verwendet. Wählen Sie ein anderes Segment."),
                errors.get(FormTriggerConfigV1.FORM_SLUG)
        );
    }

    @Test
    void validateConfiguration_ShouldReturnMultipleSlugErrors() throws Exception {
        when(processNodeRepository.exists(anySpecification())).thenReturn(true);

        var errors = node.validateConfiguration(new ProcessNodeConfigurationValidationContext<>(
                processNode(),
                configuration("Antrag Online", validFormLayout()),
                DerivedRuntimeElementData.empty(), ProcessNodeConfigurationValidationPhase.Authoring
        ));

        assertNotNull(errors);
        assertEquals(
                List.of(
                        "Das URL-Segment wird in dieser Prozessversion bereits von einem anderen Formulareingang verwendet. Wählen Sie ein anderes Segment."
                ),
                errors.get(FormTriggerConfigV1.FORM_SLUG)
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void missingFormHasTheSameReadableErrorOnInitAndResume(boolean resume) {
        var context = context(new FormTriggerConfigV1(), Map.of());

        var error = assertThrows(ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> {
                    if (resume) node.resume(context);
                    else node.init(context);
                });

        assertEquals("Hinterlegen Sie ein Formular für den Formulareingang.", error.getMessage());
        verifyNoInteractions(pdfService, paymentPayloadCreationService, paymentTransactionService);
    }

    @Test
    void missingSubmissionExplainsWhyThePdfCopyCannotBeCreated() {
        var error = assertThrows(ProcessNodeExecutionExceptionMissingValue.class,
                () -> node.init(context(configuration("antrag-online", validFormLayout()), Map.of())));

        assertEquals("Die übermittelten Formulardaten für die Kopie der Angaben fehlen.", error.getMessage());
        verifyNoInteractions(pdfService, processInstanceAttachmentSetService, processInstanceAttachmentService);
    }

    @Test
    void pdfCreationFailureKeepsItsCauseOutOfTheVisibleMessage() throws Exception {
        var cause = new IOException("Interner PDF-Dienstfehler mit vertraulichen Details");
        when(pdfService.generateCustomerSummary(any(), any(), any(), any(), any(), any())).thenThrow(cause);

        var error = assertThrows(ProcessNodeExecutionExceptionUnknown.class, () -> node.init(context(
                configuration("antrag-online", validFormLayout()),
                Map.of(FormTriggerNodeV1.DATA_KEY_UNMAPPED, Map.of()))));

        assertEquals("Die Kopie der Angaben konnte nicht als PDF erstellt werden.", error.getMessage());
        assertSame(cause, error.getCause());
        verifyNoInteractions(processInstanceAttachmentSetService, processInstanceAttachmentService);
    }

    @Test
    void interruptedPdfCreationKeepsItsCauseAndRestoresTheInterruptFlag() throws Exception {
        var cause = new InterruptedException("PDF-Erstellung unterbrochen");
        when(pdfService.generateCustomerSummary(any(), any(), any(), any(), any(), any())).thenThrow(cause);

        try {
            var error = assertThrows(ProcessNodeExecutionExceptionUnknown.class, () -> node.init(context(
                    configuration("antrag-online", validFormLayout()),
                    Map.of(FormTriggerNodeV1.DATA_KEY_UNMAPPED, Map.of()))));

            assertEquals("Die Erstellung der Kopie der Angaben als PDF wurde unterbrochen.", error.getMessage());
            assertSame(cause, error.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void pdfStorageFailureKeepsTheCauseWithoutExposingServiceDetails(boolean attachmentSetFails) throws Exception {
        prepareSummaryStorage();
        var cause = ResponseException.internalServerError("Interner Speicherfehler mit vertraulichen Details");
        if (attachmentSetFails) {
            when(processInstanceAttachmentSetService.create(any(ProcessInstanceAttachmentSetEntity.class))).thenThrow(cause);
        } else {
            when(processInstanceAttachmentService.create(any(ProcessInstanceAttachmentEntity.class))).thenThrow(cause);
        }

        var error = assertThrows(ProcessNodeExecutionExceptionUnknown.class, () -> node.init(context(
                configuration("antrag-online", validFormLayout()),
                Map.of(FormTriggerNodeV1.DATA_KEY_UNMAPPED, Map.of()))));

        assertEquals("Die Kopie der Angaben konnte nicht als Anlage zum Vorgang gespeichert werden.", error.getMessage());
        assertSame(cause, error.getCause());
    }

    @Test
    void missingPaymentProviderIdentifiesTheConfiguredProvider() throws Exception {
        prepareSummaryStorage();
        var configuration = configuration("antrag-online", validFormLayout());
        var providerKey = UUID.randomUUID();
        configuration.payment = new PaymentConfigElementValue(providerKey, null, null, null, null, null, null, null);
        when(paymentProviderRepository.findById(providerKey)).thenReturn(Optional.empty());

        var error = assertThrows(ProcessNodeExecutionExceptionInvalidConfiguration.class, () -> node.init(context(
                configuration, Map.of(FormTriggerNodeV1.DATA_KEY_UNMAPPED, Map.of()))));

        assertEquals("Der ausgewählte Zahlungsanbieter wurde nicht gefunden. Zahlungsanbieter: „" + providerKey + "“.",
                error.getMessage());
        verifyNoInteractions(paymentPayloadCreationService, paymentTransactionService);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void paymentServiceFailureRetainsItsCauseWithoutExposingServiceDetails(boolean creatingAtProvider) throws Exception {
        prepareSummaryStorage();
        var configuration = configuration("antrag-online", validFormLayout());
        var providerKey = UUID.randomUUID();
        configuration.payment = new PaymentConfigElementValue(providerKey, null, null, null, null, null, null, null);
        when(paymentProviderRepository.findById(providerKey)).thenReturn(Optional.of(paymentProvider(providerKey)));
        var cause = new PaymentException("%s", "Interner Anbieterfehler: 50% mit vertraulichen Details");
        if (creatingAtProvider) {
            when(paymentPayloadCreationService.createRequest(any(), any(), any())).thenReturn(Optional.of(new PaymentPayload()));
            when(paymentTransactionService.create(any(), any(), any())).thenThrow(cause);
        } else {
            when(paymentPayloadCreationService.createRequest(any(), any(), any())).thenThrow(cause);
        }

        var error = assertThrows(ProcessNodeExecutionExceptionUnknown.class, () -> node.init(context(
                configuration, Map.of(FormTriggerNodeV1.DATA_KEY_UNMAPPED, Map.of()))));

        assertEquals(creatingAtProvider
                        ? "Die Zahlungsanforderung konnte beim Zahlungsanbieter nicht angelegt werden."
                        : "Die Zahlungsanforderung für das Formular konnte nicht erstellt werden.",
                error.getMessage());
        assertSame(cause, error.getCause());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void resumeWaitsForPaymentOrRecordsItsConfirmation(boolean confirmed) throws Exception {
        var context = context(configuration("antrag-online", validFormLayout()),
                Map.of(FormTriggerNodeV1.DATA_KEY_PAYLOAD, Map.of("name", "Ada")));
        context.getThisTask().setRuntimeData(Map.of(FormTriggerNodeV1.DATA_KEY_PAYMENT_TRANSACTION_KEY, "tx-1"));
        var transaction = paymentTransaction(UUID.randomUUID(), confirmed ? XBezahldienstStatus.PAYED : XBezahldienstStatus.INITIAL);
        when(paymentTransactionService.retrieve("tx-1")).thenReturn(Optional.of(transaction));

        var result = node.resume(context);

        if (confirmed) {
            var completed = assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, result);
            assertEquals(Map.of("name", "Ada"), completed.getProcessData());
            assertSame(transaction.getPaymentInformation(), completed.getNodeData().get(FormTriggerNodeV1.DATA_KEY_PAYMENT_DETAILS));
            verify(context.getLogger()).logf(ProcessNodeExecutionLogLevel.Info, false, true,
                    "Zahlung bestätigt", "Die Zahlung wurde bestätigt. Zahlungsreferenz: %s.", "„tx-1“");
        } else {
            assertInstanceOf(ProcessNodeExecutionResultNoop.class, result);
            verifyNoInteractions(context.getLogger());
        }
        verifyNoInteractions(pdfService, processInstanceAttachmentSetService, processInstanceAttachmentService);
    }

    @ParameterizedTest
    @CsvSource({"FAILED, Fehlgeschlagen, true", "CANCELED, Abgebrochen, false"})
    void failedPaymentUsesGermanStatusAndKeepsProviderDetailsInItsCause(
            XBezahldienstStatus status, String label, boolean hasProviderDetails) {
        var context = context(configuration("antrag-online", validFormLayout()), Map.of());
        context.getThisTask().setRuntimeData(Map.of(FormTriggerNodeV1.DATA_KEY_PAYMENT_TRANSACTION_KEY, "tx-1"));
        var transaction = paymentTransaction(UUID.randomUUID(), status);
        var providerDetails = "Interner Anbieterhinweis: 50% mit vertraulichen Details";
        if (hasProviderDetails) transaction.setPaymentError(providerDetails);
        when(paymentTransactionService.retrieve("tx-1")).thenReturn(Optional.of(transaction));

        var error = assertThrows(ProcessNodeExecutionExceptionIO.class, () -> node.resume(context));

        assertEquals("Die Zahlung wurde nicht erfolgreich abgeschlossen. Status: " + label + ". Zahlungsreferenz: „tx-1“.",
                error.getMessage());
        if (hasProviderDetails) {
            assertInstanceOf(PaymentException.class, error.getCause());
            assertEquals(providerDetails, error.getCause().getMessage());
        } else {
            assertNull(error.getCause());
        }
    }

    @Test
    void missingPaymentTransactionIdentifiesThePaymentReference() {
        var context = context(configuration("antrag-online", validFormLayout()), Map.of());
        context.getThisTask().setRuntimeData(Map.of(FormTriggerNodeV1.DATA_KEY_PAYMENT_TRANSACTION_KEY, "tx-1"));
        when(paymentTransactionService.retrieve("tx-1")).thenReturn(Optional.empty());

        var error = assertThrows(ProcessNodeExecutionExceptionInvalidConfiguration.class, () -> node.resume(context));

        assertEquals("Die zum Formulareingang gehörende Zahlungsanforderung wurde nicht gefunden. Zahlungsreferenz: „tx-1“.",
                error.getMessage());
    }

    @Test
    void init_ShouldGenerateCustomerSummaryAndKeepMappedPayloadAsProcessData() throws Exception {
        var pdfBytes = "pdf-bytes".getBytes(StandardCharsets.UTF_8);
        var formLayout = validFormLayout();
        var configuration = configuration("antrag-online", formLayout);
        when(pdfService.generateCustomerSummary(
                same(formLayout),
                any(AuthoredElementValues.class),
                eq(FormPdfScope.Customer),
                any(ProcessInstanceEntity.class),
                same(configuration),
                any(ProcessNodeEntity.class)
        )).thenReturn(pdfBytes);
        when(processInstanceAttachmentSetService.create(any(ProcessInstanceAttachmentSetEntity.class)))
                .thenAnswer(invocation -> invocation
                        .getArgument(0, ProcessInstanceAttachmentSetEntity.class)
                        .setId(321));
        when(processInstanceAttachmentService.create(any(ProcessInstanceAttachmentEntity.class)))
                .thenAnswer(invocation -> invocation
                        .getArgument(0, ProcessInstanceAttachmentEntity.class)
                        .setKey(UUID.randomUUID())
                        .setStorageProviderId(7)
                        .setStoragePathFromRoot("attachments/Formularzusammenfassung.pdf"));

        var started = Instant.now();
        var mappedPayload = Map.<String, Object>of("person", Map.of("name", "Ada"));
        var initialPayload = Map.<String, Object>of(
                FormTriggerNodeV1.DATA_KEY_PAYLOAD, mappedPayload,
                FormTriggerNodeV1.DATA_KEY_UNMAPPED, Map.of("nameField", "Ada"),
                FormTriggerNodeV1.DATA_KEY_ATTACHMENTS, List.of(),
                FormTriggerNodeV1.DATA_KEY_STARTED, started
        );

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(context(configuration, initialPayload))
        );

        assertEquals(mappedPayload, result.getProcessData());
        assertEquals(mappedPayload, result.getNodeData().get(FormTriggerNodeV1.DATA_KEY_PAYLOAD));
        assertEquals(started, result.getNodeData().get(FormTriggerNodeV1.DATA_KEY_STARTED));

        var submissionCaptor = ArgumentCaptor.forClass(AuthoredElementValues.class);
        var processInstanceCaptor = ArgumentCaptor.forClass(ProcessInstanceEntity.class);
        var processNodeCaptor = ArgumentCaptor.forClass(ProcessNodeEntity.class);
        verify(pdfService).generateCustomerSummary(
                same(formLayout),
                submissionCaptor.capture(),
                eq(FormPdfScope.Customer),
                processInstanceCaptor.capture(),
                same(configuration),
                processNodeCaptor.capture()
        );
        assertEquals("Ada", submissionCaptor.getValue().getLiteral("nameField"));
        assertEquals(PROCESS_INSTANCE_ID, processInstanceCaptor.getValue().getId());
        assertEquals(PROCESS_ID, processInstanceCaptor.getValue().getProcessId());
        assertEquals("formNode", processNodeCaptor.getValue().getDataKey());

        var attachmentSetCaptor = ArgumentCaptor.forClass(ProcessInstanceAttachmentSetEntity.class);
        verify(processInstanceAttachmentSetService).create(attachmentSetCaptor.capture());
        assertEquals("Kopie der Angaben.pdf", attachmentSetCaptor.getValue().getName());
        assertEquals("formNode", attachmentSetCaptor.getValue().getDataKey());
        assertEquals(PROCESS_INSTANCE_ID, attachmentSetCaptor.getValue().getProcessInstanceId());
        assertEquals(TASK_ID, attachmentSetCaptor.getValue().getProcessInstanceTaskId());

        var attachmentCaptor = ArgumentCaptor.forClass(ProcessInstanceAttachmentEntity.class);
        verify(processInstanceAttachmentService).create(attachmentCaptor.capture());
        assertEquals("Kopie der Angaben.pdf", attachmentCaptor.getValue().getFileName());
        assertEquals(321, attachmentCaptor.getValue().getAttachmentSetId());
        Assertions.assertArrayEquals(pdfBytes, attachmentCaptor.getValue().getFileBytes());

        @SuppressWarnings("unchecked")
        var files = (List<FileUploadInputElementItem>) result.getNodeData().get(FormTriggerNodeV1.DATA_KEY_CUSTOMER_SUMMARY_FILES);
        assertEquals(1, files.size());
        assertEquals("Kopie der Angaben.pdf", files.getFirst().getName());
        assertEquals("Kopie der Angaben.pdf", files.getFirst().getOriginalFileName());
        assertEquals(pdfBytes.length, files.getFirst().getSize());
        assertTrue(files.getFirst().getUri().startsWith(FileUploadMultipartInputService.PROCESS_INSTANCE_ATTACHMENT_URI_PREFIX));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void activatesPaymentDeadlineOnlyForAnActualPaymentRequest(boolean requestsPayment) throws Exception {
        var configuration = configuration("antrag-online", validFormLayout());
        var providerKey = UUID.randomUUID();
        configuration.payment = new PaymentConfigElementValue(providerKey, null, null, null, null, null, null, null);
        when(pdfService.generateCustomerSummary(any(), any(), any(), any(), any(), any())).thenReturn(new byte[]{1, 2, 3});
        when(processInstanceAttachmentSetService.create(any(ProcessInstanceAttachmentSetEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, ProcessInstanceAttachmentSetEntity.class).setId(321));
        when(processInstanceAttachmentService.create(any(ProcessInstanceAttachmentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, ProcessInstanceAttachmentEntity.class)
                        .setKey(UUID.randomUUID()).setStorageProviderId(7).setStoragePathFromRoot("summary.pdf"));
        when(paymentProviderRepository.findById(providerKey)).thenReturn(Optional.of(paymentProvider(providerKey)));
        when(paymentPayloadCreationService.createRequest(any(), any(), any()))
                .thenReturn(requestsPayment ? Optional.of(new PaymentPayload()) : Optional.empty());
        when(paymentTransactionService.create(any(), any(), any()))
                .thenReturn(paymentTransaction(providerKey, XBezahldienstStatus.INITIAL));

        var context = context(configuration, Map.of(FormTriggerNodeV1.DATA_KEY_UNMAPPED, Map.of()));
        var result = node.init(context);

        if (requestsPayment) {
            assertInstanceOf(ProcessNodeExecutionResultPaymentRequested.class, result);
            assertEquals("payment", result.getNewDeadlineKey());
        } else {
            assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, result);
            assertEquals(null, result.getNewDeadlineKey());
            verifyNoInteractions(paymentTransactionService);
            verify(context.getLogger()).logf(ProcessNodeExecutionLogLevel.Info, false, true,
                    "Keine Zahlung erforderlich",
                    "Es wurde keine Zahlungsanforderung erstellt, weil der Gesamtbetrag 0,00 Euro beträgt.");
        }
    }

    @Test
    void init_CompletesWithThePaymentOfAnEarlierAttemptInsteadOfRequestingAgain() throws Exception {
        var configuration = configuration("antrag-online", validFormLayout());
        var providerKey = UUID.randomUUID();
        configuration.payment = new PaymentConfigElementValue(providerKey, null, null, null, null, null, null, null);
        when(pdfService.generateCustomerSummary(any(), any(), any(), any(), any(), any())).thenReturn(new byte[]{1, 2, 3});
        when(processInstanceAttachmentSetService.create(any(ProcessInstanceAttachmentSetEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, ProcessInstanceAttachmentSetEntity.class).setId(321));
        when(processInstanceAttachmentService.create(any(ProcessInstanceAttachmentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, ProcessInstanceAttachmentEntity.class)
                        .setKey(UUID.randomUUID()).setStorageProviderId(7).setStoragePathFromRoot("summary.pdf"));
        var transaction = paymentTransaction(providerKey, XBezahldienstStatus.PAYED);
        Map<String, Object> earlierRuntimeData = Map.of(
                FormTriggerNodeV1.DATA_KEY_PAYMENT_PAYLOAD, new PaymentPayload(),
                FormTriggerNodeV1.DATA_KEY_PAYMENT_TRANSACTION_KEY, "tx-1"
        );
        var paidAttempt = new PaymentTaskAttemptService.PaidAttempt(
                task().setId(41L).setRuntimeData(earlierRuntimeData), transaction);
        when(paymentTaskAttemptService.findPaidEarlierAttempt(any())).thenReturn(Optional.of(paidAttempt));
        when(paymentTransactionService.retrieve("tx-1")).thenReturn(Optional.of(transaction));

        var context = context(configuration, Map.of(FormTriggerNodeV1.DATA_KEY_UNMAPPED, Map.of()));
        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(context)
        );

        assertNull(result.getNewDeadlineKey());
        assertEquals(earlierRuntimeData, result.getRuntimeData());
        assertEquals(transaction.getPaymentInformation(), result.getNodeData().get(FormTriggerNodeV1.DATA_KEY_PAYMENT_DETAILS));
        verify(paymentTaskAttemptService).logReuse(any(), same(paidAttempt));
        verify(paymentTransactionService, never()).create(any(), any(), any());
        verifyNoInteractions(paymentPayloadCreationService, paymentProviderRepository);
        verify(context.getLogger()).logf(ProcessNodeExecutionLogLevel.Info, false, true,
                "Zahlung bestätigt", "Die Zahlung wurde bestätigt. Zahlungsreferenz: %s.", "„tx-1“");
    }

    @Test
    void cleanConfigurationForExport_ShouldRemoveSystemLocalIdentityAndPaymentConfiguration() {
        var configuration = new AuthoredElementValues();
        configuration.putLiteral(FormTriggerConfigV1.FORM_LAYOUT, validFormLayout());
        configuration.putLiteral(FormTriggerConfigV1.IDENTITIES, List.of(Map.of("id", "identity")));
        configuration.putLiteral(FormTriggerConfigV1.PAYMENT, Map.of("paymentProviderKey", UUID.randomUUID()));

        var cleaned = node.cleanConfigurationForExport(configuration);

        assertFalse(cleaned.containsKey(FormTriggerConfigV1.IDENTITIES));
        assertFalse(cleaned.containsKey(FormTriggerConfigV1.PAYMENT));
    }

    @Test
    void getCompletedCustomerTaskView_ShouldRenderConfiguredPaymentSuccessMessage() throws Exception {
        var paymentProviderKey = UUID.randomUUID();
        var transactionKey = "tx-1";
        var paymentConfig = new PaymentConfigElementValue(
                paymentProviderKey,
                null,
                null,
                false,
                null,
                List.of(),
                "# Zahlung erhalten\nDanke **{{ $.name }}**.",
                "# Zahlung offen\nHallo **{{ $.name }}**."
        );
        var nodeConfiguration = new FormTriggerConfigV1();
        nodeConfiguration.formSlug = "antrag-online";
        nodeConfiguration.payment = paymentConfig;
        var task = task()
                .setRuntimeData(Map.of(
                        FormTriggerNodeV1.DATA_KEY_PAYMENT_TRANSACTION_KEY, transactionKey,
                        FormTriggerNodeV1.DATA_KEY_PAYMENT_PAYLOAD, new PaymentPayload()
                ))
                .setProcessData(Map.of("name", "Ada"));

        when(paymentTransactionService.retrieve(transactionKey))
                .thenReturn(Optional.of(paymentTransaction(paymentProviderKey, XBezahldienstStatus.PAYED)));
        var paymentProvider = paymentProvider(paymentProviderKey);
        when(paymentProviderRepository.findById(paymentProviderKey))
                .thenReturn(Optional.of(paymentProvider));
        var paymentProviderDefinition = paymentProviderDefinition();
        when(paymentProviderDefinitionsService.getProviderDefinition(
                paymentProvider.getPaymentProviderDefinitionKey(),
                paymentProvider.getPaymentProviderDefinitionVersion()
        )).thenReturn(Optional.of(paymentProviderDefinition));
        when(processService.retrieve(PROCESS_ID))
                .thenReturn(Optional.of(process()));

        var layout = node.getCompletedCustomerTaskView(new ProcessNodeExecutionContextUICustomer<>(
                mock(ProcessNodeExecutionLogger.class),
                processNode(),
                processInstance(Map.of()),
                task,
                null,
                null,
                nodeConfiguration,
                null
        )).layout();

        var richText = layout.findChild("rtx", RichTextContentElement.class).orElseThrow();
        assertTrue(richText.getContent().contains("# Zahlung bestätigt\n# Zahlung erhalten\nDanke **Ada**."));
    }

    @Test
    void getCompletedCustomerTaskView_ShouldRenderPaymentConfirmationDownloadUrl() throws Exception {
        var paymentProviderKey = UUID.randomUUID();
        var transactionKey = "tx-1";
        var paymentConfig = new PaymentConfigElementValue(
                paymentProviderKey,
                null,
                null,
                false,
                null,
                List.of(),
                null,
                null
        );
        var nodeConfiguration = new FormTriggerConfigV1();
        nodeConfiguration.formSlug = "antrag-online";
        nodeConfiguration.payment = paymentConfig;
        var task = task()
                .setAccessKey("task-access")
                .setRuntimeData(Map.of(
                        FormTriggerNodeV1.DATA_KEY_PAYMENT_TRANSACTION_KEY, transactionKey,
                        FormTriggerNodeV1.DATA_KEY_PAYMENT_PAYLOAD, new PaymentPayload()
                ));
        var instance = processInstance(Map.of()).setAccessKey("instance-access");

        when(paymentTransactionService.retrieve(transactionKey))
                .thenReturn(Optional.of(paymentTransaction(paymentProviderKey, XBezahldienstStatus.PAYED)));
        var paymentProvider = paymentProvider(paymentProviderKey);
        when(paymentProviderRepository.findById(paymentProviderKey))
                .thenReturn(Optional.of(paymentProvider));
        var paymentProviderDefinition = paymentProviderDefinition();
        when(paymentProviderDefinitionsService.getProviderDefinition(
                paymentProvider.getPaymentProviderDefinitionKey(),
                paymentProvider.getPaymentProviderDefinitionVersion()
        )).thenReturn(Optional.of(paymentProviderDefinition));
        when(processService.retrieve(PROCESS_ID))
                .thenReturn(Optional.of(process()));

        var layout = node.getCompletedCustomerTaskView(new ProcessNodeExecutionContextUICustomer<>(
                mock(ProcessNodeExecutionLogger.class),
                processNode(),
                instance,
                task,
                null,
                null,
                nodeConfiguration,
                null
        )).layout();

        var downloadButton = layout.findChild("download", LinkButtonContentElement.class).orElseThrow();
        assertEquals(
                "https://example.test/api/public/form/antrag-prozess/antrag-online/submit/instance-access/task-access/payment-confirmation/",
                downloadButton.getHref()
        );
    }

    @Test
    void getCompletedCustomerTaskView_ShouldFallBackToGenericConfirmationWithoutPaymentData() throws Exception {
        var configuration = new FormTriggerConfigV1();

        var view = node.getCompletedCustomerTaskView(new ProcessNodeExecutionContextUICustomer<>(
                mock(ProcessNodeExecutionLogger.class),
                processNode(),
                processInstance(Map.of()),
                task(),
                null,
                null,
                configuration,
                null
        ));

        var alert = view.layout()
                .findChild(
                        node.getKey() + "-completed-customer-task-alert",
                        AlertContentElement.class
                )
                .orElseThrow();
        assertEquals("Aufgabe abgeschlossen", alert.getTitle());
        assertTrue(view.events().isEmpty());
    }

    @Test
    void paymentMessageFailureDoesNotExposeTheTemplateOrParserDetails() throws Exception {
        var providerKey = UUID.randomUUID();
        var configuration = configuration("antrag-online", validFormLayout());
        configuration.payment = new PaymentConfigElementValue(providerKey, null, null, false, null, List.of(),
                "{{ privateTemplateFunction() }}", null);
        var task = task().setRuntimeData(Map.of(
                FormTriggerNodeV1.DATA_KEY_PAYMENT_TRANSACTION_KEY, "tx-1",
                FormTriggerNodeV1.DATA_KEY_PAYMENT_PAYLOAD, new PaymentPayload()));
        when(paymentTransactionService.retrieve("tx-1"))
                .thenReturn(Optional.of(paymentTransaction(providerKey, XBezahldienstStatus.PAYED)));
        when(paymentProviderRepository.findById(providerKey)).thenReturn(Optional.of(paymentProvider(providerKey)));
        var providerDefinition = paymentProviderDefinition();
        when(paymentProviderDefinitionsService.getProviderDefinition("test-provider", 1))
                .thenReturn(Optional.of(providerDefinition));
        when(processService.retrieve(PROCESS_ID)).thenReturn(Optional.of(process()));

        var error = assertThrows(ResponseException.class, () -> node.getCompletedCustomerTaskView(
                new ProcessNodeExecutionContextUICustomer<>(mock(ProcessNodeExecutionLogger.class), processNode(),
                        processInstance(Map.of()), task, null, null, configuration, null)));

        assertEquals("Die hinterlegte Nachricht zum Zahlungsstatus konnte nicht erstellt werden.", error.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, error.getStatus());
        assertNull(error.getDetails());
        assertNotNull(error.getCause());
    }

    private void prepareSummaryStorage() throws Exception {
        when(pdfService.generateCustomerSummary(any(), any(), any(), any(), any(), any())).thenReturn(new byte[]{1, 2, 3});
        when(processInstanceAttachmentSetService.create(any(ProcessInstanceAttachmentSetEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, ProcessInstanceAttachmentSetEntity.class).setId(321));
        when(processInstanceAttachmentService.create(any(ProcessInstanceAttachmentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, ProcessInstanceAttachmentEntity.class)
                        .setKey(UUID.randomUUID()).setStorageProviderId(7).setStoragePathFromRoot("summary.pdf"));
    }

    private static FormTriggerConfigV1 configuration(String formSlug, FormLayoutElement formLayout) {
        var configuration = new FormTriggerConfigV1();
        configuration.formSlug = formSlug;
        configuration.formLayout = formLayout;
        return configuration;
    }

    private static FormLayoutElement validFormLayout() {
        return new FormLayoutElement()
                .setPublicTitle("Antrag auf Leistung");
    }

    private static ProcessNodeEntity processNode() {
        return new ProcessNodeEntity()
                .setId(NODE_ID)
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setName("Formular")
                .setDataKey("formNode")
                .setProcessNodeDefinitionKey(FormTriggerNodeV1.NODE_KEY)
                .setProcessNodeDefinitionVersion(1)
                .setConfiguration(new AuthoredElementValues())
                .setOutputMappings(Map.of());
    }

    private static ProcessNodeExecutionInitContext<FormTriggerConfigV1> context(FormTriggerConfigV1 configuration,
                                                                                Map<String, Object> initialPayload) {
        return new ProcessNodeExecutionInitContext<>(
                mock(ProcessNodeExecutionLogger.class),
                processNode(),
                processInstance(initialPayload),
                task(),
                null,
                new ProcessExecutionData(),
                configuration
        );
    }

    private static ProcessInstanceEntity processInstance(Map<String, Object> initialPayload) {
        var now = Instant.now();

        return new ProcessInstanceEntity()
                .setId(PROCESS_INSTANCE_ID)
                .setAccessKey(UUID.randomUUID().toString())
                .setProcessId(PROCESS_ID)
                .setInitialProcessVersion(PROCESS_VERSION)
                .setStatus(ProcessInstanceStatus.Running)
                .setAssignedFileNumbers(List.of())
                .setIdentities(new IdentityDataMap())
                .setStarted(now)
                .setUpdated(now)
                .setInitialPayload(initialPayload)
                .setInitialNodeId(NODE_ID);
    }

    private static ProcessInstanceTaskEntity task() {
        var now = Instant.now();

        return new ProcessInstanceTaskEntity()
                .setId(TASK_ID)
                .setAccessKey(UUID.randomUUID().toString())
                .setProcessInstanceId(PROCESS_INSTANCE_ID)
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setProcessNodeId(NODE_ID)
                .setStatus(ProcessTaskStatus.Running)
                .setStarted(now)
                .setUpdated(now)
                .setRuntimeData(Map.of())
                .setNodeData(Map.of())
                .setProcessData(Map.of());
    }

    private static PaymentProviderEntity paymentProvider(UUID paymentProviderKey) {
        return new PaymentProviderEntity()
                .setKey(paymentProviderKey)
                .setPaymentProviderDefinitionKey("test-provider")
                .setPaymentProviderDefinitionVersion(1)
                .setName("Stadtkasse");
    }

    private static PaymentProviderDefinition paymentProviderDefinition() {
        var definition = mock(PaymentProviderDefinition.class);
        when(definition.getProviderName()).thenReturn("Testprovider");
        return definition;
    }

    private static PaymentTransactionEntity paymentTransaction(UUID paymentProviderKey, XBezahldienstStatus status) {
        var paymentInformation = new XBezahldienstePaymentInformation();
        paymentInformation.setStatus(status);

        return new PaymentTransactionEntity()
                .setKey("tx-1")
                .setPaymentProviderKey(paymentProviderKey)
                .setPaymentInformation(paymentInformation);
    }

    private static ProcessNodeDefinitionConfigurationLayoutContext configurationLayoutContext() {
        return new ProcessNodeDefinitionConfigurationLayoutContext(
                null,
                process(),
                processVersion(),
                processNode()
        );
    }

    private static ProcessEntity process() {
        return new ProcessEntity()
                .setId(PROCESS_ID)
                .setInternalTitle("Antrag")
                .setDepartmentId(1)
                .setAccessKey(UUID.randomUUID())
                .setSlug("antrag-prozess")
                .setVersionCount(PROCESS_VERSION)
                .setDraftedVersion(PROCESS_VERSION);
    }

    private static ProcessVersionEntity processVersion() {
        return new ProcessVersionEntity()
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setStatus(ProcessVersionStatus.Drafted)
                .setPublicTitle("Antrag");
    }

    private FormTriggerNodeV1 createNode(PublicUrlService publicUrlService) {
        return new FormTriggerNodeV1(
                publicUrlService,
                processNodeRepository,
                paymentPayloadCreationService,
                templateRenderService,
                paymentTransactionService,
                paymentProviderRepository,
                paymentProviderDefinitionsService,
                processService,
                prosunaConfig(),
                pdfService,
                processInstanceAttachmentService,
                processInstanceAttachmentSetService,
                JsonMapperTestUtils.createMapper(),
                new AuthoredInputValueService(JsonMapperTestUtils.createMapper()),
                paymentTaskAttemptService
        );
    }

    private static ProsunaConfig prosunaConfig() {
        var config = new ProsunaConfig();
        config.setProsunaHostname("https://example.test/");
        return config;
    }

    private static Specification<ProcessNodeEntity> anySpecification() {
        return any();
    }
}
