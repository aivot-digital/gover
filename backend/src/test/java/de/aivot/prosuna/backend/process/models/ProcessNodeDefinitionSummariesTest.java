package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.plugins.ai.v1.nodes.AiCompletionActionNodeV1;
import de.aivot.prosuna.backend.plugins.ai.v1.nodes.AiProcessDataTransformationActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.ApprovalActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.CommunicationMessageActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.CounterActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.DataChangeActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.DataMappingActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.flow.DataTypeValidationControlNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.terminators.DefaultTerminationNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.EMailActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.FitConnectSendJsonActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.triggers.fitconnect.FitConnectTriggerNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.FormRequestActionNodeV1;
import de.aivot.prosuna.backend.plugins.form.v1.nodes.FormTriggerNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.HttpActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.flow.IfFlowControlNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.InstanceAssignmentActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.InstanceUnassignmentActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.LowCodeActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.ManualActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.NoCodeActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.PaymentRequestActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.PdfActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.triggers.webhook.WebhookTriggerNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.WriteExternalStorageActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.HttpActionNodeV1Config;
import de.aivot.prosuna.backend.payment.models.PaymentPayload;
import de.aivot.prosuna.backend.payment.models.PaymentItem;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentInformation;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.elements.models.elements.form.input.HtmlTemplateInputElementResolver;
import de.aivot.prosuna.backend.elements.models.elements.form.input.HtmlTemplateInputElementValue;
import java.math.BigDecimal;
import java.net.URI;
import tools.jackson.databind.json.JsonMapper;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import de.aivot.prosuna.backend.process.entities.*;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionSummaryContext;
import de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class ProcessNodeDefinitionSummariesTest {
    private final ProcessInstanceTaskEntity task = new ProcessInstanceTaskEntity().setId(2L)
            .setFinished(Instant.parse("2026-10-01T10:00:00Z"));
    private final ProcessInstanceEntity instance = new ProcessInstanceEntity().setId(1L)
            .setFinished(Instant.parse("2026-10-01T10:00:00Z"))
            .setKeepUntil(Instant.parse("2027-10-01T10:00:00Z"));
    private final ProcessNodeEntity node = new ProcessNodeEntity().setName("Testformular").setDataKey("counter");
    private UserEntity actor = new UserEntity().setId("ada").setFullName("Ada Beispiel");
    private String port = "approved";
    private ZoneId previousZone;

    ProcessNodeDefinitionSummariesTest() {
        var data = new HashMap<String, Object>();
        data.put("processedAt", "2026-10-01T08:00:00Z");
        data.put("started", "2026-10-01T09:00:00Z");
        data.put("increment", 3);
        data.put("previousValue", 2);
        data.put("value", 5);
        data.put("assignedUserId", "ida");
        data.put("recipientIdentityId", "customer");
        data.put("identityId", "customer");
        data.put("fileName", "Bescheid.pdf");
        data.put("attachmentKey", "original-attachment");
        data.put("statusCode", 200);
        data.put("mappedRuleCount", 1);
        data.put("isValid", true);
        data.put("conditionValue", true);
        data.put("request", Map.of("headers", Map.of("ReFeReR", List.of("https://user:secret@example.org/form?token=secret#secret"))));
        data.put("paymentDetails", Map.of("transactionTimestamp", "2026-10-01T09:00:00Z"));
        task.setNodeData(data);
        task.setRuntimeData(ExecutionSummaryMarkdown.withMetadata(null, Map.of(
                "sentAt", "2026-10-01T07:00:00Z", "deliveryChannel", "E-Mail",
                "previousAssignedUserId", "ida", "previousAssignedUserName", "Ida Beispiel",
                "assignedUserName", "Ida Beispiel", "documents", List.of(Map.of(
                        "fileName", "Bescheid.pdf", "providerName", "Archiv", "path", "archive/Bescheid.pdf",
                        "dataKey", "bescheid", "attachmentKey", "original-attachment")))));
    }

    @BeforeEach
    void configureBusinessTimezone() {
        previousZone = ApplicationTimeZone.getZoneId();
        ApplicationTimeZone.configure(ZoneId.of("Europe/Berlin"));
    }

    @AfterEach
    void restoreBusinessTimezone() {
        ApplicationTimeZone.configure(previousZone);
    }

    static Stream<Arguments> definitions() {
        return Stream.of(
                Arguments.of(ApprovalActionNodeV1.class, "Die Freigabe wurde am 01.10.2026 um 10:00:00 durch „Ada Beispiel“ erteilt."),
                Arguments.of(DataChangeActionNodeV1.class, "Die Daten wurden am 01.10.2026 um 10:00:00 durch „Ada Beispiel“ geändert."),
                Arguments.of(ManualActionNodeV1.class, "Die manuelle Aktion wurde am 01.10.2026 um 10:00:00 durch „Ada Beispiel“ als durchgeführt gemeldet."),
                Arguments.of(CounterActionNodeV1.class, "Der Zähler „counter“ wurde um 3 erhöht."), 
                Arguments.of(CommunicationMessageActionNodeV1.class, "Die Nachricht wurde am 01.10.2026 um 09:00:00 erfolgreich versendet."),
                Arguments.of(EMailActionNodeV1.class, "Die E-Mail-Nachricht wurde am 01.10.2026 um 12:00:00 erfolgreich versendet."),
                Arguments.of(FormRequestActionNodeV1.class, "Es wurde die Identität „customer“ am 01.10.2026 um 09:00:00 zur Einreichung von Daten aufgefordert und via E\\-Mail informiert. Die Daten wurden am 01.10.2026 um 11:00:00 durch die Identität „customer“ eingereicht."),
                Arguments.of(PaymentRequestActionNodeV1.class, "Der Bezahlvorgang wurde am 01.10.2026 um 11:00:00 erfolgreich abgeschlossen."),
                Arguments.of(InstanceAssignmentActionNodeV1.class, "Der Vorgang wurde „Ida Beispiel“ (durch „Ada Beispiel“) zugewiesen."),
                Arguments.of(InstanceUnassignmentActionNodeV1.class, "Die bestehende Zuweisung des Vorgangs wurde entfernt (war zugewiesen an: „Ida Beispiel“)."),
                Arguments.of(PdfActionNodeV1.class, "Das Dokument „Bescheid\\.pdf“ wurde am 01.10.2026 um 12:00:00 erfolgreich erstellt."),
                Arguments.of(WriteExternalStorageActionNodeV1.class, "Das Dokument „Bescheid\\.pdf“ wurde erfolgreich beim Speicheranbieter „Archiv“ geschrieben."), 
                Arguments.of(HttpActionNodeV1.class, "Die externe HTTP-Schnittstelle (Endpunkt: https://example\\.org/call) wurde am 01.10.2026 um 12:00:00 aufgerufen."),
                Arguments.of(FitConnectSendJsonActionNodeV1.class, "Die Daten wurden erfolgreich an eine FIT-Connect-Schnittstelle übertragen."), 
                Arguments.of(NoCodeActionNodeV1.class, "Die No-Code-Logik wurde erfolgreich ausgeführt."), 
                Arguments.of(LowCodeActionNodeV1.class, "Die Low-Code-Logik wurde erfolgreich ausgeführt."), 
                Arguments.of(DataMappingActionNodeV1.class, "Die Vorgangsdaten wurden anhand von 1 Regel angepasst."), 
                Arguments.of(DataTypeValidationControlNodeV1.class, "Die Vorgangsdaten wurden erfolgreich validiert."), 
                Arguments.of(IfFlowControlNodeV1.class, "Der Vorgang wurde konditionell in den Ausführungspfad „Bedingung erfüllt“ eingeleitet."), 
                Arguments.of(DefaultTerminationNodeV1.class, "Das Ende der Aufbewahrungsfrist für die Vorgangsdaten wurde für 01.10.2027 um 12:00:00 festgelegt."),
                Arguments.of(WebhookTriggerNodeV1.class, "Der Vorgang wurde am 01.10.2026 um 11:00:00 durch einen Aufruf über einen Webhook ausgelöst (Quelle: https://example\\.org/form)."),
                Arguments.of(FitConnectTriggerNodeV1.class, "Der Vorgang wurde am 01.10.2026 um 11:00:00 durch einen Aufruf über eine FIT-Connect-Schnittstelle ausgelöst."),
                Arguments.of(FormTriggerNodeV1.class, "Der Vorgang wurde am 01.10.2026 um 11:00:00 durch die Übermittlung des Formulars „Testformular“ ausgelöst."),
                Arguments.of(AiCompletionActionNodeV1.class, "Die KI-Anfrage wurde erfolgreich ausgeführt."), 
                Arguments.of(AiProcessDataTransformationActionNodeV1.class, "Die Vorgangsdaten wurden erfolgreich mit KI transformiert.")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("definitions")
    void allExistingDefinitionsDescribeCompletedExecution(Class<? extends ProcessNodeDefinition<?>> type, String expected) throws Exception {
        var definition = instantiate(type);
        var config = definition.getNodeConfigurationClass().getConstructor().newInstance();
        if (config instanceof HttpActionNodeV1Config http) {
            http.url = "https://user:secret@example.org/call?token=secret#secret";
        }
        if (definition instanceof IfFlowControlNodeV1) {
            port = "true";
        }
        var markdown = summary(definition, config);
        assertNotNull(markdown);
        assertTrue(markdown.contains(expected), markdown);
        assertFalse(markdown.contains("secret"), markdown);
        assertFalse(markdown.contains("[TIMESTAMP]"), markdown);
    }

    @Test
    void approvalRejectionPreservesAuthoredMarkdownAndLinksModeledData() throws Exception {
        port = "rejected";
        var config = new ApprovalActionNodeV1.ApprovalConfiguration();
        config.criteria = "**Kriterien**";
        config.contentMode = "data";
        task.getNodeData().put("remark", "*Nicht erfüllt.*");
        var markdown = summary(new ApprovalActionNodeV1(null, null, null, null), config);
        assertTrue(markdown.contains("verweigert."));
        assertTrue(markdown.contains("**Kriterien**"));
        assertTrue(markdown.contains("*Nicht erfüllt.*"));
        assertTrue(markdown.contains("(/staff/tasks/1/2)"));
        assertFalse(markdown.contains("/edit"));
        config.contentMode = "custom";
        config.customContent = "## Eigener Prüfinhalt";
        assertTrue(summary(new ApprovalActionNodeV1(null, null, null, null), config).contains(config.customContent));
    }

    @Test
    void counterDescribesDecreaseAndNoChange() throws Exception {
        var definition = new CounterActionNodeV1(null);
        var config = new CounterActionNodeV1.CounterActionNodeV1Configuration();
        task.getNodeData().put("increment", -2);
        assertTrue(summary(definition, config).contains("um 2 verringert"));
        task.getNodeData().put("increment", 0);
        assertTrue(summary(definition, config).contains("nicht verändert"));
    }

    @Test
    void configuredHttpErrorStatusIsNotReportedAsSuccess() throws Exception {
        task.getNodeData().put("statusCode", 404);
        var definition = instantiate(HttpActionNodeV1.class);
        var markdown = summary(definition, new HttpActionNodeV1Config());
        assertTrue(markdown.contains("404 (Clientfehler)"));
        assertFalse(markdown.contains("Erfolgreich"));
    }

    @Test
    void validationAndMappingExplainNegativeAndDestructiveOutcomes() throws Exception {
        task.getNodeData().put("isValid", false);
        task.getNodeData().put("errors", List.of(Map.of("path", "name", "error", "Wert fehlt")));
        var validation = summary(new DataTypeValidationControlNodeV1(), new DataTypeValidationControlNodeV1.DataTypeValidationControlNodeConfig());
        assertTrue(validation.contains("Fehler festgestellt"));
        assertTrue(validation.contains("name: Wert fehlt"));
        task.getNodeData().put("mappedValues", List.of(
                Map.of("originalPath", "a", "newPath", "b"),
                Map.of("originalPath", "c", "newPath", "d", "cleanupSource", true),
                Map.of("originalPath", "e", "deleteOnly", true)));
        var mapping = summary(new DataMappingActionNodeV1(), new DataMappingActionNodeV1.DataMappingActionNodeV1Config());
        assertTrue(mapping.contains("Kopiert: a → b"));
        assertTrue(mapping.contains("Verschoben: c → d"));
        assertTrue(mapping.contains("Gelöscht: e"));
    }

    @Test
    void storageDocumentsLinkOriginalAttachmentAndListEachProviderOrEmptyResult() throws Exception {
        var definition = instantiate(WriteExternalStorageActionNodeV1.class);
        var config = new WriteExternalStorageActionNodeV1.WriteExternalStorageActionNodeConfig();
        var markdown = summary(definition, config);
        assertTrue(markdown.contains("/api/process-instance-attachments/original-attachment/file/?download=false"));
        assertTrue(markdown.contains("archive/Bescheid\\.pdf"));
        assertTrue(markdown.contains("bescheid"));
        task.setRuntimeData(Map.of());
        assertEquals("Es wurden keine Dokumente beim Speicheranbieter geschrieben.", summary(definition, config));
    }

    @Test
    void missingIdentityAndPreviousAssignmentHaveReadableFallbacks() throws Exception {
        actor = null;
        assertTrue(summary(instantiate(InstanceAssignmentActionNodeV1.class), new InstanceAssignmentActionNodeV1.Config()).contains("durch das System"));
        task.setRuntimeData(Map.of());
        assertEquals("Der Vorgang hatte keine bestehende Zuweisung.", summary(new InstanceUnassignmentActionNodeV1(), new InstanceUnassignmentActionNodeV1.Config()));
        var markdown = summary(instantiate(FormRequestActionNodeV1.class), new FormRequestActionNodeV1.NodeConfig());
        assertTrue(markdown.contains("die Identität „customer“"));
        assertFalse(markdown.contains("null"));
    }

    @Test
    void formUsesConfiguredPublicTitleAndRetainsSubmissionTimeAfterWaiting() throws Exception {
        var definition = instantiate(FormTriggerNodeV1.class);
        var config = new de.aivot.prosuna.backend.plugins.form.v1.nodes.FormTriggerConfigV1();
        config.formLayout = new de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement().setPublicTitle("Antrag *A*");
        var markdown = summary(definition, config);
        assertTrue(markdown.contains("„Antrag \\*A\\*“"));
        assertTrue(markdown.contains("11:00:00"));
        assertFalse(markdown.contains("12:00:00"));
        config.formLayout = null;
        node.setName(null);
        assertFalse(summary(definition, config).contains("„“"));
    }

    @Test
    void pdfSummaryIncludesTemplateNameAndPreview() throws Exception {
        var resolver = mock(HtmlTemplateInputElementResolver.class);
        var config = new PdfActionNodeV1.PdfActionNodeConfig();
        config.contentHtmlSource = PdfActionNodeV1.PdfActionNodeConfig.CONTENT_HTML_SOURCE_FIELD_OPTION_ASSET_KEY;
        config.contentHtmlTemplate = new HtmlTemplateInputElementValue().setAssetKey("template");
        when(resolver.getTemplateName(config.contentHtmlTemplate)).thenReturn("Vorlage.html");
        var markdown = summary(new PdfActionNodeV1(null, null, null, null, resolver), config);
        assertTrue(markdown.contains("Vorlage\\.html"));
        assertTrue(markdown.contains("/api/process-instance-attachments/original-attachment/file/?download=false"));
    }

    @Test
    void falseBranchAndAbsentWebhookReferrerAreAccurate() throws Exception {
        port = "false";
        task.getNodeData().put("conditionValue", false);
        var branch = summary(instantiate(IfFlowControlNodeV1.class), new IfFlowControlNodeV1.IfFlowControlNodeConfig());
        assertTrue(branch.contains("„Bedingung nicht erfüllt“"));
        assertTrue(branch.contains("Falsch"));
        task.getNodeData().remove("request");
        var definition = instantiate(WebhookTriggerNodeV1.class);
        var webhook = summary(definition, definition.getNodeConfigurationClass().getConstructor().newInstance());
        assertFalse(webhook.contains("Quelle:"));
        assertFalse(webhook.contains("null"));
    }

    @Test
    void paymentHandlesTypedAndPersistedDetailsAndListsPositionsWithoutPaymentUrls() throws Exception {
        var item = new PaymentItem();
        item.setDescription("Gebühr *A*");
        item.setQuantity(2L);
        item.setNetPrice(new BigDecimal("10.00"));
        item.setTaxRate(BigDecimal.ZERO);
        var payload = new PaymentPayload().setPaymentItems(List.of(item));
        var runtime = new HashMap<>(task.getRuntimeData());
        runtime.put("paymentPayload", payload);
        task.setRuntimeData(runtime);
        task.getNodeData().put("paymentTotal", new BigDecimal("20.00"));
        var information = new XBezahldienstePaymentInformation();
        information.setTransactionTimestamp("2026-10-01T09:00:00Z");
        information.setTransactionReference("ref-1");
        information.setTransactionUrl(URI.create("https://secret.example/token"));
        task.getNodeData().put("paymentDetails", information);
        var definition = instantiate(PaymentRequestActionNodeV1.class);
        var config = new PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig();
        var typed = summary(definition, config);
        assertTrue(typed.contains("Gebühr \\*A\\*: 20,00 Euro"));
        assertTrue(typed.contains("11:00:00"));
        assertFalse(typed.contains("secret.example"));
        var mapper = JsonMapperTestUtils.createMapper();
        task.setNodeData(mapper.readValue(mapper.writeValueAsString(task.getNodeData()), Map.class));
        task.setRuntimeData(mapper.readValue(mapper.writeValueAsString(task.getRuntimeData()), Map.class));
        assertEquals(typed, summary(definition, config));
    }

    private static ProcessNodeDefinition<?> instantiate(Class<? extends ProcessNodeDefinition<?>> type) throws Exception {
        var constructor = type.getConstructors()[0];
        var arguments = new Object[constructor.getParameterCount()];
        for (var index = 0; index < arguments.length; index++) {
            if (constructor.getParameterTypes()[index] == JsonMapper.class) {
                arguments[index] = JsonMapperTestUtils.createMapper();
            }
        }
        return (ProcessNodeDefinition<?>) constructor.newInstance(arguments);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private String summary(ProcessNodeDefinition definition, Object config) throws Exception {
        return definition.generateExecutionSummary(new ProcessNodeExecutionSummaryContext<>(config, node, instance, task, null, actor, port));
    }
}
