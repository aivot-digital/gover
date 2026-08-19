package de.aivot.prosuna.backend.plugins.form.v1.nodes;

import de.aivot.prosuna.backend.elements.models.elements.form.content.LinkButtonContentElement;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.FileUploadInputElementItem;
import de.aivot.prosuna.backend.elements.models.elements.form.input.FileUploadInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.PaymentConfigElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.PaymentConfigElementValue;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.steps.GenericStepElement;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.enums.XBezahldienstStatus;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.pdf.enums.FormPdfScope;
import de.aivot.prosuna.backend.payment.entities.PaymentProviderEntity;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.models.PaymentProviderDefinition;
import de.aivot.prosuna.backend.payment.models.PaymentPayload;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentInformation;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.payment.repositories.PaymentProviderRepository;
import de.aivot.prosuna.backend.payment.services.PaymentPayloadCreationService;
import de.aivot.prosuna.backend.payment.services.PaymentProviderDefinitionsService;
import de.aivot.prosuna.backend.payment.services.PaymentTransactionService;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinitionMetadata;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
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
import org.mockito.ArgumentCaptor;
import org.springframework.data.jpa.domain.Specification;

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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
    private PaymentTransactionService paymentTransactionService;
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
        paymentProviderRepository = mock(PaymentProviderRepository.class);
        paymentProviderDefinitionsService = mock(PaymentProviderDefinitionsService.class);
        processService = mock(ProcessService.class);
        templateRenderService = new TemplateRenderService(new JavascriptEngineFactoryService(List.of()));
        node = createNode(mock(PublicUrlService.class));
    }

    @Test
    void validateConfiguration_ShouldAllowValidLayoutAndUniqueSlug() throws Exception {
        when(processNodeRepository.exists(anySpecification())).thenReturn(false);

        var errors = node.validateConfiguration(
                processNode(),
                configuration("antrag-online", validFormLayout())
        );

        assertNull(errors);
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
        assertEquals("Eingangszeitstempel", output.label());
        assertEquals("Der Zeitstempel des Dateneingangs an den Auslöser", output.description());
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
        assertEquals("Formularzusammenfassung", output.label());
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
        assertEquals("Formularzusammenfassung", attachmentSet.label());
        assertFalse(attachmentSet.isMultifile());
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
    void validateConfiguration_ShouldReportLegacyLayoutFieldsMissingFromFormLayout() throws Exception {
        when(processNodeRepository.exists(anySpecification())).thenReturn(false);

        var errors = node.validateConfiguration(
                processNode(),
                configuration("antrag-online", new FormLayoutElement())
        );

        assertNotNull(errors);
        assertEquals(1, errors.size());

        var layoutError = errors.get(FormTriggerConfigV1.FORM_LAYOUT);
        assertNotNull(layoutError);
        assertTrue(layoutError.contains("Der öffentliche Titel muss hinterlegt sein."));
        assertTrue(layoutError.contains("Der fachliche Support muss eingerichtet sein."));
        assertTrue(layoutError.contains("Der technische Support muss eingerichtet sein."));
        assertTrue(layoutError.contains("Das Impressum muss eingerichtet sein."));
        assertTrue(layoutError.contains("Die Datenschutzerklärung muss eingerichtet sein."));
        assertTrue(layoutError.contains("Die Barrierefreiheitserklärung muss eingerichtet sein."));
    }

    @Test
    void validateConfiguration_ShouldRequireSubmittedFileNameForFileUploads() throws Exception {
        when(processNodeRepository.exists(anySpecification())).thenReturn(false);

        var upload = new FileUploadInputElement();
        upload.setId("document");
        upload.setLabel("Nachweis");
        var step = new GenericStepElement();
        step.setId("step");
        step.setChildren(List.of(upload));
        var layout = validFormLayout();
        layout.setChildren(List.of(step));

        var errors = node.validateConfiguration(
                processNode(),
                configuration("antrag-online", layout)
        );

        assertNotNull(errors);
        assertEquals(
                List.of("Für das Anlagen-Feld „Nachweis“ muss ein Dateiname bei Einreichung hinterlegt sein."),
                errors.get(FormTriggerConfigV1.FORM_LAYOUT)
        );
    }

    @Test
    void validateConfiguration_ShouldRejectDuplicateSlug() throws Exception {
        when(processNodeRepository.exists(anySpecification())).thenReturn(true);

        var errors = node.validateConfiguration(
                processNode(),
                configuration("antrag-online", validFormLayout())
        );

        assertNotNull(errors);
        assertEquals(
                List.of("Das URL-Segment des Formulars wird in dieser Prozessversion bereits von einem anderen Formulareingang verwendet."),
                errors.get(FormTriggerConfigV1.FORM_SLUG)
        );
    }

    @Test
    void validateConfiguration_ShouldReturnMultipleSlugErrors() throws Exception {
        when(processNodeRepository.exists(anySpecification())).thenReturn(true);

        var errors = node.validateConfiguration(
                processNode(),
                configuration("Antrag Online", validFormLayout())
        );

        assertNotNull(errors);
        assertEquals(
                List.of(
                        "Das URL-Segment des Formulars darf nur aus Kleinbuchstaben, Zahlen und Bindestrichen bestehen.",
                        "Das URL-Segment des Formulars wird in dieser Prozessversion bereits von einem anderen Formulareingang verwendet."
                ),
                errors.get(FormTriggerConfigV1.FORM_SLUG)
        );
    }

    @Test
    void validateConfiguration_ShouldReturnSlugAndLayoutErrorsTogether() throws Exception {
        when(processNodeRepository.exists(anySpecification())).thenReturn(true);

        var errors = node.validateConfiguration(
                processNode(),
                configuration("antrag-online", new FormLayoutElement())
        );

        assertNotNull(errors);
        assertEquals(2, errors.size());
        assertTrue(errors.containsKey(FormTriggerConfigV1.FORM_SLUG));
        assertTrue(errors.containsKey(FormTriggerConfigV1.FORM_LAYOUT));
    }

    @Test
    void init_ShouldGenerateCustomerSummaryAndKeepMappedPayloadAsProcessData() throws Exception {
        var pdfBytes = "pdf-bytes".getBytes(StandardCharsets.UTF_8);
        var formLayout = validFormLayout();
        var configuration = configuration("antrag-online", formLayout);
        when(pdfService.generateCustomerSummary(
                same(formLayout),
                any(AuthoredElementValues.class),
                eq(FormPdfScope.Citizen),
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
                eq(FormPdfScope.Citizen),
                processInstanceCaptor.capture(),
                same(configuration),
                processNodeCaptor.capture()
        );
        assertEquals("Ada", submissionCaptor.getValue().get("nameField"));
        assertEquals(PROCESS_INSTANCE_ID, processInstanceCaptor.getValue().getId());
        assertEquals(PROCESS_ID, processInstanceCaptor.getValue().getProcessId());
        assertEquals("formNode", processNodeCaptor.getValue().getDataKey());

        var attachmentSetCaptor = ArgumentCaptor.forClass(ProcessInstanceAttachmentSetEntity.class);
        verify(processInstanceAttachmentSetService).create(attachmentSetCaptor.capture());
        assertEquals("Formularzusammenfassung.pdf", attachmentSetCaptor.getValue().getName());
        assertEquals("formNode", attachmentSetCaptor.getValue().getDataKey());
        assertEquals(PROCESS_INSTANCE_ID, attachmentSetCaptor.getValue().getProcessInstanceId());
        assertEquals(TASK_ID, attachmentSetCaptor.getValue().getProcessInstanceTaskId());

        var attachmentCaptor = ArgumentCaptor.forClass(ProcessInstanceAttachmentEntity.class);
        verify(processInstanceAttachmentService).create(attachmentCaptor.capture());
        assertEquals("Formularzusammenfassung.pdf", attachmentCaptor.getValue().getFileName());
        assertEquals(321, attachmentCaptor.getValue().getAttachmentSetId());
        Assertions.assertArrayEquals(pdfBytes, attachmentCaptor.getValue().getFileBytes());

        @SuppressWarnings("unchecked")
        var files = (List<FileUploadInputElementItem>) result.getNodeData().get(FormTriggerNodeV1.DATA_KEY_CUSTOMER_SUMMARY_FILES);
        assertEquals(1, files.size());
        assertEquals("Formularzusammenfassung.pdf", files.getFirst().getName());
        assertEquals("Formularzusammenfassung.pdf", files.getFirst().getOriginalFileName());
        assertEquals(pdfBytes.length, files.getFirst().getSize());
        assertTrue(files.getFirst().getUri().startsWith(FileUploadMultipartInputService.PROCESS_INSTANCE_ATTACHMENT_URI_PREFIX));
    }

    @Test
    void cleanConfigurationForExport_ShouldRemoveSystemLocalIdentityAndPaymentConfiguration() {
        var configuration = new AuthoredElementValues();
        configuration.put(FormTriggerConfigV1.FORM_LAYOUT, validFormLayout());
        configuration.put(FormTriggerConfigV1.IDENTITIES, List.of(Map.of("id", "identity")));
        configuration.put(FormTriggerConfigV1.PAYMENT, Map.of("paymentProviderKey", UUID.randomUUID()));

        var cleaned = node.cleanConfigurationForExport(configuration);

        assertFalse(cleaned.containsKey(FormTriggerConfigV1.IDENTITIES));
        assertFalse(cleaned.containsKey(FormTriggerConfigV1.PAYMENT));
    }

    @Test
    void getCustomerTaskView_ShouldRenderConfiguredPaymentSuccessMessage() throws Exception {
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

        var layout = node.getCustomerTaskView(new ProcessNodeExecutionContextUICustomer<>(
                mock(ProcessNodeExecutionLogger.class),
                processNode(),
                processInstance(Map.of()),
                task,
                null,
                null,
                nodeConfiguration,
                null
        ));

        var richText = layout.findChild("rtx", RichTextContentElement.class).orElseThrow();
        assertTrue(richText.getContent().contains("# Zahlung erfolgreich\n# Zahlung erhalten\nDanke **Ada**."));
    }

    @Test
    void getCustomerTaskView_ShouldRenderPaymentConfirmationDownloadUrl() throws Exception {
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

        var layout = node.getCustomerTaskView(new ProcessNodeExecutionContextUICustomer<>(
                mock(ProcessNodeExecutionLogger.class),
                processNode(),
                instance,
                task,
                null,
                null,
                nodeConfiguration,
                null
        ));

        var downloadButton = layout.findChild("download", LinkButtonContentElement.class).orElseThrow();
        assertEquals(
                "https://example.test/api/public/form/antrag-prozess/antrag-online/submit/instance-access/task-access/payment-confirmation/",
                downloadButton.getHref()
        );
    }

    private static FormTriggerConfigV1 configuration(String formSlug, FormLayoutElement formLayout) {
        var configuration = new FormTriggerConfigV1();
        configuration.formSlug = formSlug;
        configuration.formLayout = formLayout;
        return configuration;
    }

    private static FormLayoutElement validFormLayout() {
        return new FormLayoutElement()
                .setPublicTitle("Antrag auf Leistung")
                .setLegalSupportDepartmentId(1)
                .setTechnicalSupportDepartmentId(2)
                .setImprintDepartmentId(3)
                .setPrivacyDepartmentId(4)
                .setAccessibilityDepartmentId(5);
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
                mock(PaymentPayloadCreationService.class),
                templateRenderService,
                paymentTransactionService,
                paymentProviderRepository,
                paymentProviderDefinitionsService,
                processService,
                prosunaConfig(),
                pdfService,
                processInstanceAttachmentService,
                processInstanceAttachmentSetService
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
