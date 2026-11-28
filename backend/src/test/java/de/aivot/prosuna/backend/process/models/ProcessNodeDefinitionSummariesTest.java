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
import de.aivot.prosuna.backend.plugins.core.v1.nodes.triggers.fitconnect.FitConnectTriggerConfigV1;
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
import de.aivot.prosuna.backend.plugins.core.v1.nodes.triggers.webhook.WebhookTriggerConfigV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.WriteExternalStorageActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.HttpActionNodeV1Config;
import de.aivot.prosuna.backend.payment.models.PaymentPayload;
import de.aivot.prosuna.backend.payment.models.PaymentItem;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentInformation;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.elements.models.elements.form.input.HtmlTemplateInputElementResolver;
import de.aivot.prosuna.backend.elements.models.elements.form.input.HtmlTemplateInputElementValue;
import de.aivot.prosuna.backend.elements.uiPresets.SemiAutomaticMessageConfig;
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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.section;
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
                Arguments.of(ApprovalActionNodeV1.class, "Die Freigabe wurde am 01.10.2026 um 10:00:00 Uhr durch „Ada Beispiel“ erteilt."),
                Arguments.of(DataChangeActionNodeV1.class, "Die Bearbeitung der Vorgangsdaten wurde am 01.10.2026 um 10:00:00 Uhr durch „Ada Beispiel“ abgeschlossen."),
                Arguments.of(ManualActionNodeV1.class, "Die Erledigung der freien Aufgabe wurde am 01.10.2026 um 10:00:00 Uhr durch „Ada Beispiel“ bestätigt."),
                Arguments.of(CounterActionNodeV1.class, "Der Zähler „Testformular“ wurde um 3 erhöht."),
                Arguments.of(CommunicationMessageActionNodeV1.class, "Die Nachricht wurde am 01.10.2026 um 09:00:00 Uhr versendet."),
                Arguments.of(EMailActionNodeV1.class, "Die E-Mail wurde am 01.10.2026 um 12:00:00 Uhr versendet."),
                Arguments.of(FormRequestActionNodeV1.class, "Die Nachricht mit dem Formularlink wurde am 01.10.2026 um 09:00:00 Uhr über E\\-Mail versendet. Die angeforderten Angaben wurden am 01.10.2026 um 11:00:00 Uhr durch die Identität „customer“ übermittelt."),
                Arguments.of(PaymentRequestActionNodeV1.class, "Die Aufgabe wurde am 01.10.2026 um 12:00:00 Uhr mit bestätigter Zahlung abgeschlossen."),
                Arguments.of(InstanceAssignmentActionNodeV1.class, "Der Vorgang bleibt „Ida Beispiel“ zugewiesen. Die bestehende Zuweisung wurde durch „Ada Beispiel“ beibehalten."),
                Arguments.of(InstanceUnassignmentActionNodeV1.class, "Die Zuweisung des Vorgangs an „Ida Beispiel“ wurde automatisch aufgehoben."),
                Arguments.of(PdfActionNodeV1.class, "Das PDF-Dokument „Bescheid\\.pdf“ wurde am 01.10.2026 um 12:00:00 Uhr erstellt."),
                Arguments.of(WriteExternalStorageActionNodeV1.class, "Die Ablage wurde am 01.10.2026 um 12:00:00 Uhr abgeschlossen."),
                Arguments.of(HttpActionNodeV1.class, "Die HTTP-Schnittstelle wurde am 01.10.2026 um 12:00:00 Uhr aufgerufen."),
                Arguments.of(FitConnectSendJsonActionNodeV1.class, "Der JSON-Datensatz wurde am 01.10.2026 um 12:00:00 Uhr über FIT-Connect eingereicht."),
                Arguments.of(NoCodeActionNodeV1.class, "Die No-Code-Verarbeitung wurde abgeschlossen."),
                Arguments.of(LowCodeActionNodeV1.class, "Die Low-Code-Verarbeitung wurde abgeschlossen."),
                Arguments.of(DataMappingActionNodeV1.class, "Es wurde 1 Regel zur Bearbeitung der Vorgangsdaten ausgeführt."),
                Arguments.of(DataTypeValidationControlNodeV1.class, "Die Prüfung hat keine Abweichungen von den festgelegten Regeln ergeben."),
                Arguments.of(IfFlowControlNodeV1.class, "Für die Fortsetzung wurde der Ausgang „Bedingung erfüllt“ gewählt."
                        + "\n\nBedingung erfüllt\n:   Ja"),
                Arguments.of(DefaultTerminationNodeV1.class, "Die Aufbewahrungsfrist endet am 01.10.2027 um 12:00:00 Uhr."),
                Arguments.of(WebhookTriggerNodeV1.class, "Der Vorgang wurde am 01.10.2026 um 11:00:00 Uhr durch einen Aufruf über einen Webhook ausgelöst (angegebene Herkunft: https://example\\.org/form)."),
                Arguments.of(FitConnectTriggerNodeV1.class, "Der Vorgang wurde am 01.10.2026 um 11:00:00 Uhr durch eine über FIT-Connect empfangene Einreichung ausgelöst."),
                Arguments.of(FormTriggerNodeV1.class, "Der Vorgang wurde am 01.10.2026 um 11:00:00 Uhr durch die Übermittlung des Formulars „Testformular“ ausgelöst. Die Zahlung wurde bestätigt."),
                Arguments.of(AiCompletionActionNodeV1.class, "Die KI-Anfrage wurde am 01.10.2026 um 12:00:00 Uhr abgeschlossen."),
                Arguments.of(AiProcessDataTransformationActionNodeV1.class, "Die Umwandlung der Vorgangsdaten mit KI wurde am 01.10.2026 um 12:00:00 Uhr abgeschlossen.")
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
    void approvalRejectionPreservesAuthoredMarkdownWithoutTaskLinks() throws Exception {
        port = "rejected";
        var config = new ApprovalActionNodeV1.ApprovalConfiguration();
        config.criteria = "**Kriterien**";
        config.contentMode = "data";
        config.customContent = "## Eigener Prüfinhalt";
        task.getNodeData().put("remark", "*Nicht erfüllt.*");
        var markdown = summary(new ApprovalActionNodeV1(null, null, null, null), config);
        assertTrue(markdown.contains("abgelehnt."));
        assertTrue(markdown.contains("**Kriterien**"));
        assertTrue(markdown.contains("*Nicht erfüllt.*"));
        assertFalse(markdown.contains("/staff/tasks/"));
        assertFalse(markdown.contains("/edit"));
        assertFalse(markdown.contains(config.customContent));
        config.contentMode = "custom";
        var customContentSummary = summary(new ApprovalActionNodeV1(null, null, null, null), config);
        assertTrue(customContentSummary.endsWith(section("Geprüfte Inhalte", config.customContent)), customContentSummary);
    }

    @Test
    void manualActionRetainsRichCompletionDetailsAndIncludesOptionalDescription() throws Exception {
        var definition = new ManualActionNodeV1(null, null, null, null);
        var config = new ManualActionNodeV1.ManualActionNodeConfig();
        config.taskDescription = "<p>Führen Sie einen <strong>Ortstermin</strong> durch.</p>\n<p>Ergebnis festhalten.</p>";
        task.getNodeData().put("remark", "<p>Ortstermin durchgeführt.</p>\n<p><em>Keine Mängel.</em></p>");
        var expected = "Die Erledigung der freien Aufgabe wurde am 01.10.2026 um 10:00:00 Uhr durch „Ada Beispiel“ bestätigt."
                + "\n\nAufgabenbeschreibung\n:   <p>Führen Sie einen <strong>Ortstermin</strong> durch.</p>"
                + "\n    <p>Ergebnis festhalten.</p>"
                + "\n\nVermerk\n:   <p>Ortstermin durchgeführt.</p>\n    <p><em>Keine Mängel.</em></p>";

        assertEquals(expected, summary(definition, config));

        node.setDescription("Ortstermin *dokumentieren*");
        assertEquals(expected + "\n\nKurzbeschreibung\n:   Ortstermin \\*dokumentieren\\*",
                summary(definition, config));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void terminationUsesStoredCompletionAndRetentionAndIncludesOptionalDescription(boolean hasRetention) throws Exception {
        instance.setFinished(Instant.parse("2025-01-15T09:15:00Z"));
        instance.setKeepUntil(hasRetention ? Instant.parse("2027-06-15T08:15:00Z") : null);
        var definition = new DefaultTerminationNodeV1();
        var config = new DefaultTerminationNodeV1.DefaultTerminationNodeV1Config();
        config.retentionValue = 2;
        config.retentionUnit = "weeks";
        var expected = "Der Vorgang wurde am 15.01.2025 um 10:15:00 Uhr abgeschlossen."
                + (hasRetention ? " Die Aufbewahrungsfrist endet am 15.06.2027 um 10:15:00 Uhr."
                : " Für diesen Vorgang ist keine Aufbewahrungsfrist festgelegt.");

        assertEquals(expected, summary(definition, config));

        node.setDescription("Abschluss [nach] *Entscheidung*");
        assertEquals(expected + "\n\nKurzbeschreibung\n:   Abschluss \\[nach\\] \\*Entscheidung\\*",
                summary(definition, config));
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

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \n\t"})
    void counterUsesTheEscapedNodeNameWhenStorageTargetIsAbsent(String storageTarget) throws Exception {
        node.setName("**Wiederholungen** [zählen]");
        task.getNodeData().put("storageTarget", storageTarget);

        var markdown = summary(new CounterActionNodeV1(null), new CounterActionNodeV1.CounterActionNodeV1Configuration());

        assertEquals(
                "Der Zähler „\\*\\*Wiederholungen\\*\\* \\[zählen\\]“ wurde um 3 erhöht."
                        + "\n\nVorheriger Zählerstand\n:   2"
                        + "\n\nNeuer Zählerstand\n:   5",
                markdown
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \n\t"})
    void counterUsesTheProviderNameWhenTheNodeHasNoName(String nodeName) throws Exception {
        node.setName(nodeName);
        task.getNodeData().put("storageTarget", "");

        var markdown = summary(new CounterActionNodeV1(null), new CounterActionNodeV1.CounterActionNodeV1Configuration());

        assertTrue(markdown.startsWith("Der Zähler „Zähler“ wurde um 3 erhöht."), markdown);
        assertFalse(markdown.contains("„counter“"), markdown);
    }

    @Test
    void counterRetainsTheEscapedStorageTargetInsteadOfTheNodeName() throws Exception {
        node.setName("Wiederholungen zählen");
        task.getNodeData().put("storageTarget", "schleife.anzahl");

        var markdown = summary(new CounterActionNodeV1(null), new CounterActionNodeV1.CounterActionNodeV1Configuration());

        assertTrue(markdown.startsWith("Der Zähler „schleife\\.anzahl“ wurde um 3 erhöht."), markdown);
        assertFalse(markdown.contains(node.getName()), markdown);
    }

    @Test
    void configuredHttpErrorStatusIsNotReportedAsSuccess() throws Exception {
        task.getNodeData().put("statusCode", 404);
        var definition = instantiate(HttpActionNodeV1.class);
        var markdown = summary(definition, new HttpActionNodeV1Config());
        assertTrue(markdown.contains("404 (Fehler bei der Anfrage)"));
        assertFalse(markdown.contains("Erfolgreich"));
    }

    @Test
    void httpSummaryUsesTheSavedEndpointAndFileDetailsWithoutRepeatingResponseData() throws Exception {
        var config = new HttpActionNodeV1Config();
        config.url = "https://later.example.test/current?token=config-secret";
        task.setRuntimeData(ExecutionSummaryMarkdown.withMetadata(null, Map.of("endpoint",
                ExecutionSummaryMarkdown.safeUrl("https://user:password@snapshot.example.org/api?token=endpoint-secret#fragment-secret"))));
        task.setNodeData(Map.of(
                "statusCode", 201,
                "attachmentKey", "response/attachment?version=1",
                "fileName", "Antwort [Alt].pdf",
                "mimeType", "application/pdf",
                "sizeBytes", 0L,
                "rawBody", "response-secret",
                "processedResponse", Map.of("token", "processed-secret"),
                "headers", Map.of("Set-Cookie", List.of("session=header-secret"))
        ));
        node.setDescription("Antwort *zuordnen*\nund <prüfen>.");

        var markdown = summary(instantiate(HttpActionNodeV1.class), config);

        assertEquals("Die HTTP-Schnittstelle wurde am 01.10.2026 um 12:00:00 Uhr aufgerufen."
                + "\n\nEndpunkt\n:   https://snapshot\\.example\\.org/api"
                + "\n\nHTTP\\-Statuscode\n:   201 (Erfolgreiche HTTP-Antwort)"
                + "\n\nAntwortanlage\n:   [Antwortanlage ansehen](/api/process-instance-attachments/response%2Fattachment%3Fversion%3D1/file/?download=false)"
                + "\n\nDateiname\n:   Antwort \\[Alt\\]\\.pdf"
                + "\n\nDateityp \\(MIME\\-Typ\\)\n:   application/pdf"
                + "\n\nDateigröße in Bytes\n:   0"
                + "\n\nKurzbeschreibung\n:   Antwort \\*zuordnen\\* und \\<prüfen\\>\\.", markdown);
        assertFalse(markdown.contains("later.example"));
        assertFalse(markdown.contains("secret"));
        assertFalse(markdown.contains("Set-Cookie"));
    }

    @Test
    void historicalHttpSummaryUsesTheSanitizedConfigurationWhenTheEndpointWasNotSaved() throws Exception {
        var config = new HttpActionNodeV1Config();
        config.url = "https://user:password@legacy.example.test:8443/api?token=secret#secret";
        task.setRuntimeData(Map.of());
        task.setNodeData(Map.of());
        task.setFinished(null);
        node.setDescription(" \n\t");

        assertEquals("Die HTTP-Schnittstelle wurde aufgerufen."
                        + "\n\nEndpunkt\n:   https://legacy\\.example\\.test:8443/api",
                summary(instantiate(HttpActionNodeV1.class), config));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \n\t"})
    void httpSummaryOmitsEmptyStatusAndUnavailableOptionalDetails(String statusCode) throws Exception {
        var config = new HttpActionNodeV1Config();
        config.url = "{{ $.url }}";
        task.setRuntimeData(Map.of());
        var data = new HashMap<String, Object>();
        data.put("statusCode", statusCode);
        data.put("fileName", " \n\t");
        data.put("mimeType", "");
        data.put("sizeBytes", null);
        task.setNodeData(data);
        node.setDescription(null);

        assertEquals("Die HTTP-Schnittstelle wurde am 01.10.2026 um 12:00:00 Uhr aufgerufen.",
                summary(instantiate(HttpActionNodeV1.class), config));
    }

    @Test
    void validationAndMappingExplainNegativeAndDestructiveOutcomes() throws Exception {
        task.getNodeData().put("isValid", false);
        task.getNodeData().put("errors", List.of(Map.of("path", "name", "error", "Wert fehlt")));
        var validation = summary(new DataTypeValidationControlNodeV1(), new DataTypeValidationControlNodeV1.DataTypeValidationControlNodeConfig());
        assertTrue(validation.startsWith("Die Prüfung hat Abweichungen von den festgelegten Regeln ergeben."));
        assertTrue(validation.contains("name: Wert fehlt"));
        task.getNodeData().put("mappedValues", List.of(
                Map.of("originalPath", "a", "newPath", "b"),
                Map.of("originalPath", "c", "newPath", "d", "cleanupSource", true),
                Map.of("originalPath", "e", "deleteOnly", true)));
        var mapping = summary(new DataMappingActionNodeV1(), new DataMappingActionNodeV1.DataMappingActionNodeV1Config());
        assertTrue(mapping.contains("Kopierregel: a → b"));
        assertTrue(mapping.contains("Verschieberegel: c → d"));
        assertTrue(mapping.contains("Löschregel: e"));
    }

    @Test
    void storageDocumentsUseSavedTargetsAndLinkTheOriginalAttachmentWithANeutralLabel() throws Exception {
        var definition = instantiate(WriteExternalStorageActionNodeV1.class);
        var config = new WriteExternalStorageActionNodeV1.WriteExternalStorageActionNodeConfig();
        var changedConfig = new WriteExternalStorageActionNodeV1.WriteExternalStorageConfig();
        changedConfig.attachmentSetDataKeys = List.of("spaeterer-anlagensatz");
        changedConfig.fileName = "Spaeterer Dateiname";
        config.attachmentSets = List.of(changedConfig);
        task.setNodeData(Map.of("count", 2));
        task.setRuntimeData(ExecutionSummaryMarkdown.withMetadata(null, Map.of(
                ExecutionSummaryMarkdown.DOCUMENTS, List.of(
                        Map.of("fileName", "Kopie [Alt].pdf", "providerName", "Archiv *Alt*",
                                "path", "/archiv/[Alt]/Kopie.pdf", "dataKey", "nachweise.alt",
                                "attachmentKey", "original/attachment?mode=1"),
                        Map.of("fileName", "Bild.png", "providerName", "Bildarchiv",
                                "path", "/bilder/Bild.png", "dataKey", "bilder",
                                "attachmentKey", "second-original")))));
        node.setDescription("Anlagen *sichern*\nund <zuordnen>.");

        var markdown = summary(definition, config);

        assertEquals("Die Ablage wurde am 01.10.2026 um 12:00:00 Uhr abgeschlossen."
                + "\n\nDie Datei „Kopie \\[Alt\\]\\.pdf“ wurde beim Speicheranbieter „Archiv \\*Alt\\*“ gespeichert."
                + "\n\nZielpfad\n:   /archiv/\\[Alt\\]/Kopie\\.pdf"
                + "\n\nDatenschlüssel des Anlagensatzes\n:   nachweise\\.alt"
                + "\n\nUrsprüngliche Anlage\n:   [Ursprüngliche Anlage ansehen](/api/process-instance-attachments/original%2Fattachment%3Fmode%3D1/file/?download=false)"
                + "\n\nDie Datei „Bild\\.png“ wurde beim Speicheranbieter „Bildarchiv“ gespeichert."
                + "\n\nZielpfad\n:   /bilder/Bild\\.png"
                + "\n\nDatenschlüssel des Anlagensatzes\n:   bilder"
                + "\n\nUrsprüngliche Anlage\n:   [Ursprüngliche Anlage ansehen](/api/process-instance-attachments/second-original/file/?download=false)"
                + "\n\nKurzbeschreibung\n:   Anlagen \\*sichern\\* und \\<zuordnen\\>\\.", markdown);
        assertFalse(markdown.contains(changedConfig.fileName));
        assertFalse(markdown.contains(changedConfig.attachmentSetDataKeys.getFirst()));
        assertFalse(markdown.contains("[Kopie"));
        assertFalse(markdown.contains("Ausgeführte Speicherungen"));
    }

    @Test
    void storageDocumentsOmitUnavailableNamesAndOriginalAttachmentLinks() throws Exception {
        var config = new WriteExternalStorageActionNodeV1.WriteExternalStorageActionNodeConfig();
        task.setRuntimeData(ExecutionSummaryMarkdown.withMetadata(null, Map.of(
                ExecutionSummaryMarkdown.DOCUMENTS, List.of(Map.of("path", "/archiv/ohne-name")))));
        node.setDescription(" \n\t");

        assertEquals("Die Ablage wurde am 01.10.2026 um 12:00:00 Uhr abgeschlossen."
                        + "\n\nDie Datei wurde gespeichert."
                        + "\n\nZielpfad\n:   /archiv/ohne\\-name",
                summary(instantiate(WriteExternalStorageActionNodeV1.class), config));
    }

    static Stream<Arguments> storageCountsWithoutDocumentMetadata() {
        return Stream.of(
                Arguments.of(0, "\n\nEs wurden keine Anlagen gespeichert."),
                Arguments.of(3L, "\n\nAusgeführte Speicherungen\n:   3"),
                Arguments.of(null, ""),
                Arguments.of(-1, ""),
                Arguments.of("0", ""),
                Arguments.of("ungültig", "")
        );
    }

    @ParameterizedTest
    @MethodSource("storageCountsWithoutDocumentMetadata")
    void storageWithoutDocumentMetadataUsesOnlyTheSavedCount(Object count, String expectedDetails) throws Exception {
        var data = new HashMap<String, Object>();
        if (count != null) {
            data.put("count", count);
        }
        task.setNodeData(data);
        task.setRuntimeData(Objects.equals(count, 0)
                ? ExecutionSummaryMarkdown.withMetadata(null, Map.of(ExecutionSummaryMarkdown.DOCUMENTS, List.of()))
                : Map.of());
        node.setDescription("Ablage *dokumentieren*.");

        assertEquals("Die Ablage wurde am 01.10.2026 um 12:00:00 Uhr abgeschlossen." + expectedDetails
                        + "\n\nKurzbeschreibung\n:   Ablage \\*dokumentieren\\*\\.",
                summary(instantiate(WriteExternalStorageActionNodeV1.class),
                        new WriteExternalStorageActionNodeV1.WriteExternalStorageActionNodeConfig()));
    }

    @Test
    void missingIdentityAndPreviousAssignmentHaveReadableFallbacks() throws Exception {
        actor = null;
        assertTrue(summary(instantiate(InstanceAssignmentActionNodeV1.class), new InstanceAssignmentActionNodeV1.Config()).contains("durch das System"));
        task.setRuntimeData(Map.of());
        assertEquals("Dem Vorgang ist weiterhin keine Person zugewiesen.", summary(new InstanceUnassignmentActionNodeV1(), new InstanceUnassignmentActionNodeV1.Config()));
        var markdown = summary(instantiate(FormRequestActionNodeV1.class), new FormRequestActionNodeV1.NodeConfig());
        assertTrue(markdown.contains("die Identität „customer“"));
        assertFalse(markdown.contains("null"));
    }

    static Stream<Arguments> instanceAssignmentResults() {
        return Stream.of(
                Arguments.of(null, "Der Vorgang wurde „Ida \\[Beispiel\\]“ durch das System zugewiesen."),
                Arguments.of("ida", "Der Vorgang bleibt „Ida \\[Beispiel\\]“ zugewiesen. Die bestehende Zuweisung wurde durch „Ada Beispiel“ beibehalten."),
                Arguments.of("grace", "Die zuständige Person des Vorgangs wurde durch „Ada Beispiel“ von „Grace \\*Beispiel\\*“ auf „Ida \\[Beispiel\\]“ geändert.")
        );
    }

    @ParameterizedTest
    @MethodSource("instanceAssignmentResults")
    void instanceAssignmentDistinguishesSavedResultsAndIncludesOptionalDescription(
            String previousAssignedUserId, String expected) throws Exception {
        var metadata = new HashMap<String, Object>();
        metadata.put(ExecutionSummaryMarkdown.PREVIOUS_ASSIGNED_USER_ID, previousAssignedUserId);
        metadata.put(ExecutionSummaryMarkdown.PREVIOUS_ASSIGNED_USER_NAME,
                "ida".equals(previousAssignedUserId) ? "Ida [Beispiel]" : "Grace *Beispiel*");
        metadata.put(ExecutionSummaryMarkdown.ASSIGNED_USER_NAME, "Ida [Beispiel]");
        task.setRuntimeData(ExecutionSummaryMarkdown.withMetadata(null, metadata));
        instance.setAssignedUserId("a-later-assignee");
        if (previousAssignedUserId == null) {
            actor = null;
        }
        var definition = instantiate(InstanceAssignmentActionNodeV1.class);
        var config = new InstanceAssignmentActionNodeV1.Config();

        assertEquals(expected, summary(definition, config));

        node.setDescription("Zuständigkeit *festlegen*");
        assertEquals(expected + "\n\nKurzbeschreibung\n:   Zuständigkeit \\*festlegen\\*",
                summary(definition, config));
    }

    static Stream<Arguments> instanceUnassignmentResults() {
        return Stream.of(
                Arguments.of(null, "Dem Vorgang ist weiterhin keine Person zugewiesen."),
                Arguments.of("former", "Die Zuweisung des Vorgangs an „Ida \\[Beispiel\\]“ wurde automatisch aufgehoben.")
        );
    }

    @ParameterizedTest
    @MethodSource("instanceUnassignmentResults")
    void instanceUnassignmentUsesSavedPreviousAssignmentAndIncludesOptionalDescription(
            String previousAssignedUserId, String expected) throws Exception {
        var metadata = new HashMap<String, Object>();
        metadata.put(ExecutionSummaryMarkdown.PREVIOUS_ASSIGNED_USER_ID, previousAssignedUserId);
        metadata.put(ExecutionSummaryMarkdown.PREVIOUS_ASSIGNED_USER_NAME, "Ida [Beispiel]");
        task.setRuntimeData(ExecutionSummaryMarkdown.withMetadata(null, metadata));
        instance.setAssignedUserId("a-later-assignee");
        var definition = new InstanceUnassignmentActionNodeV1();
        var config = new InstanceUnassignmentActionNodeV1.Config();

        assertEquals(expected, summary(definition, config));

        node.setDescription("Zuständigkeit *aufheben*");
        assertEquals(expected + "\n\nKurzbeschreibung\n:   Zuständigkeit \\*aufheben\\*",
                summary(definition, config));
    }

    @Test
    void formUsesConfiguredPublicTitleAndRetainsSubmissionTimeAfterWaiting() throws Exception {
        var definition = instantiate(FormTriggerNodeV1.class);
        var config = new de.aivot.prosuna.backend.plugins.form.v1.nodes.FormTriggerConfigV1();
        config.formLayout = new de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement().setPublicTitle("Antrag *A*");
        instance.setStarted(Instant.parse("2026-10-01T07:00:00Z"));
        var submitted = "Der Vorgang wurde am 01.10.2026 um 11:00:00 Uhr durch die Übermittlung des Formulars „Antrag \\*A\\*“ ausgelöst.";
        assertEquals(submitted + " Die Zahlung wurde bestätigt.", summary(definition, config));

        node.setDescription("Eingang [prüfen] *und zuordnen*");
        var description = "\n\nKurzbeschreibung\n:   Eingang \\[prüfen\\] \\*und zuordnen\\*";
        assertEquals(submitted + " Die Zahlung wurde bestätigt." + description, summary(definition, config));

        task.getNodeData().remove(FormTriggerNodeV1.DATA_KEY_PAYMENT_DETAILS);
        assertEquals(submitted + description, summary(definition, config));

        config.formLayout = null;
        node.setName(null);
        assertFalse(summary(definition, config).contains("„“"));
    }

    static Stream<Arguments> formSubmissionTimeFallbacks() {
        return Stream.of(
                Arguments.of(null, true, " am 01.10.2026 um 09:00:00 Uhr"),
                Arguments.of("kein gültiger Zeitpunkt", true, " am 01.10.2026 um 09:00:00 Uhr"),
                Arguments.of(null, false, ""),
                Arguments.of("kein gültiger Zeitpunkt", false, "")
        );
    }

    @ParameterizedTest
    @MethodSource("formSubmissionTimeFallbacks")
    void formNeverUsesCompletionTimeWhenSubmissionTimeIsMissingOrInvalid(
            String started, boolean hasInstanceStart, String expectedTime) throws Exception {
        if (started == null) {
            task.getNodeData().remove(FormTriggerNodeV1.DATA_KEY_STARTED);
        } else {
            task.getNodeData().put(FormTriggerNodeV1.DATA_KEY_STARTED, started);
        }
        task.getNodeData().remove(FormTriggerNodeV1.DATA_KEY_PAYMENT_DETAILS);
        instance.setStarted(hasInstanceStart ? Instant.parse("2026-10-01T07:00:00Z") : null);
        var definition = instantiate(FormTriggerNodeV1.class);
        var config = new de.aivot.prosuna.backend.plugins.form.v1.nodes.FormTriggerConfigV1();

        assertEquals("Der Vorgang wurde" + expectedTime + " durch die Übermittlung des Formulars „Testformular“ ausgelöst.",
                summary(definition, config));
    }

    @Test
    void pdfSummaryUsesSavedFileAndEscapesItsTemplateNameAndDescription() throws Exception {
        var resolver = mock(HtmlTemplateInputElementResolver.class);
        var config = new PdfActionNodeV1.PdfActionNodeConfig();
        config.fileName = "Späterer Dateiname.pdf";
        config.contentHtmlSource = PdfActionNodeV1.PdfActionNodeConfig.CONTENT_HTML_SOURCE_FIELD_OPTION_ASSET_KEY;
        config.contentHtmlTemplate = new HtmlTemplateInputElementValue().setAssetKey("template");
        when(resolver.getTemplateName(config.contentHtmlTemplate)).thenReturn("Vorlage [Alt].html");
        task.setNodeData(Map.of("fileName", "Bescheid [Alt].pdf", "attachmentKey", "original/attachment?mode=1"));
        node.setDescription("Dokument *prüfen*\nund <ablegen>.");

        var markdown = summary(new PdfActionNodeV1(null, null, null, null, resolver), config);

        assertEquals("Das PDF-Dokument „Bescheid \\[Alt\\]\\.pdf“ wurde am 01.10.2026 um 12:00:00 Uhr erstellt."
                + "\n\nDokumentvorlage\n:   Vorlage \\[Alt\\]\\.html"
                + "\n\nDokument\n:   [Bescheid \\[Alt\\]\\.pdf](/api/process-instance-attachments/original%2Fattachment%3Fmode%3D1/file/?download=false)"
                + "\n\nKurzbeschreibung\n:   Dokument \\*prüfen\\* und \\<ablegen\\>\\.", markdown);
        assertFalse(markdown.contains(config.fileName));
    }

    @Test
    void pdfSummaryCanLinkAnOlderResultWithoutAFileName() throws Exception {
        var config = new PdfActionNodeV1.PdfActionNodeConfig();
        task.setNodeData(Map.of("attachmentKey", "original-attachment"));

        assertEquals("Das PDF-Dokument wurde am 01.10.2026 um 12:00:00 Uhr erstellt."
                        + "\n\nDokumentvorlage\n:   Direkt hinterlegte HTML\\-Dokumentvorlage"
                        + "\n\nDokument\n:   [Dokument ansehen](/api/process-instance-attachments/original-attachment/file/?download=false)",
                summary(new PdfActionNodeV1(null, null, null, null, null), config));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \n\t"})
    void pdfSummaryOmitsUnavailableTemplateAndDocumentLink(String templateName) throws Exception {
        var resolver = mock(HtmlTemplateInputElementResolver.class);
        var config = new PdfActionNodeV1.PdfActionNodeConfig();
        config.fileName = "Nicht erstelltes Dokument.pdf";
        config.contentHtmlSource = PdfActionNodeV1.PdfActionNodeConfig.CONTENT_HTML_SOURCE_FIELD_OPTION_ASSET_KEY;
        config.contentHtmlTemplate = new HtmlTemplateInputElementValue().setAssetKey("template");
        when(resolver.getTemplateName(config.contentHtmlTemplate)).thenReturn(templateName);
        task.setNodeData(Map.of("fileName", "Älteres Dokument.pdf"));
        node.setDescription(" \n\t");

        assertEquals("Das PDF-Dokument „Älteres Dokument\\.pdf“ wurde am 01.10.2026 um 12:00:00 Uhr erstellt.",
                summary(new PdfActionNodeV1(null, null, null, null, resolver), config));
    }

    @Test
    void falseBranchAndAbsentWebhookReferrerAreAccurate() throws Exception {
        port = "false";
        task.getNodeData().put("conditionValue", false);
        node.setDescription("Liegt der Betrag über 1000 Euro?");
        var branch = summary(instantiate(IfFlowControlNodeV1.class), new IfFlowControlNodeV1.IfFlowControlNodeConfig());
        assertEquals(
                "Für die Fortsetzung wurde der Ausgang „Bedingung nicht erfüllt“ gewählt."
                        + "\n\nBedingung erfüllt\n:   Nein"
                        + "\n\nKurzbeschreibung\n:   Liegt der Betrag über 1000 Euro?",
                branch
        );
        node.setDescription(null);
        task.getNodeData().remove("request");
        var definition = instantiate(WebhookTriggerNodeV1.class);
        var config = new WebhookTriggerConfigV1();
        var expected = "Der Vorgang wurde am 01.10.2026 um 11:00:00 Uhr durch einen Aufruf über einen Webhook ausgelöst.";
        assertEquals(expected, summary(definition, config));

        task.getNodeData().put("request", Map.of("headers", Map.of("referer", List.of("javascript:alert(1)"))));
        assertEquals(expected, summary(definition, config));

        task.getNodeData().remove(WebhookTriggerNodeV1.INITIAL_DATA_KEY_STARTED);
        assertEquals("Der Vorgang wurde durch einen Aufruf über einen Webhook ausgelöst.", summary(definition, config));
    }

    @Test
    void webhookPreservesSafeReportedSourceAndFallsBackToInstanceStart() throws Exception {
        task.getNodeData().put(WebhookTriggerNodeV1.INITIAL_DATA_KEY_STARTED, "kein gültiger Zeitpunkt");
        instance.setStarted(Instant.parse("2026-10-01T07:00:00Z"));
        var definition = instantiate(WebhookTriggerNodeV1.class);
        var config = new WebhookTriggerConfigV1();
        var expected = "Der Vorgang wurde am 01.10.2026 um 09:00:00 Uhr durch einen Aufruf über einen Webhook ausgelöst"
                + " (angegebene Herkunft: https://example\\.org/form).";
        assertEquals(expected, summary(definition, config));

        node.setDescription("Eingang [aus Fachsystem] *übernehmen*");
        var description = "\n\nKurzbeschreibung\n:   Eingang \\[aus Fachsystem\\] \\*übernehmen\\*";
        assertEquals(expected + description, summary(definition, config));

        task.getNodeData().remove(WebhookTriggerNodeV1.INITIAL_DATA_KEY_STARTED);
        assertEquals(expected + description, summary(definition, config));
    }

    static Stream<Arguments> fitConnectSubmissionTimestamps() {
        return Stream.of(
                Arguments.of(Instant.parse("2026-09-30T12:00:00Z"),
                        "\n\nBei FIT\\-Connect eingereicht am\n:   30\\.09\\.2026 um 14:00:00 Uhr"),
                Arguments.of("kein gültiger Zeitpunkt", ""),
                Arguments.of(null, "")
        );
    }

    @ParameterizedTest
    @MethodSource("fitConnectSubmissionTimestamps")
    void fitConnectUsesSavedSubmissionDetailsAndIncludesOptionalDescription(Object submittedAt, String expectedSubmissionTime) throws Exception {
        var submission = new HashMap<String, Object>();
        submission.put("destinationId", "destination-old");
        submission.put("submissionId", "submission-old");
        submission.put("caseId", "case-old");
        submission.put("submittedAt", submittedAt);
        task.getNodeData().put(FitConnectTriggerNodeV1.INITIAL_DATA_KEY_SUBMISSION, submission);
        instance.setStarted(Instant.parse("2026-10-01T07:00:00Z"));
        var definition = instantiate(FitConnectTriggerNodeV1.class);
        var config = new FitConnectTriggerConfigV1();
        config.destinationId = "a-later-destination";
        var expected = "Der Vorgang wurde am 01.10.2026 um 11:00:00 Uhr durch eine über FIT-Connect empfangene Einreichung ausgelöst."
                + "\n\nZustellpunkt\\-ID\n:   destination\\-old"
                + "\n\nEinreichungs\\-ID\n:   submission\\-old"
                + "\n\nFall\\-ID bei FIT\\-Connect\n:   case\\-old"
                + expectedSubmissionTime;

        assertEquals(expected, summary(definition, config));

        node.setDescription("Eingang [zuordnen] *und prüfen*");
        assertEquals(expected + "\n\nKurzbeschreibung\n:   Eingang \\[zuordnen\\] \\*und prüfen\\*",
                summary(definition, config));
    }

    @Test
    void fitConnectNeverUsesCompletionTimeWhenIncomingTimestampIsMissingOrInvalid() throws Exception {
        var definition = instantiate(FitConnectTriggerNodeV1.class);
        var config = new FitConnectTriggerConfigV1();
        task.getNodeData().put(FitConnectTriggerNodeV1.INITIAL_DATA_KEY_STARTED, "kein gültiger Zeitpunkt");
        instance.setStarted(Instant.parse("2026-10-01T07:00:00Z"));
        var expected = "Der Vorgang wurde am 01.10.2026 um 09:00:00 Uhr durch eine über FIT-Connect empfangene Einreichung ausgelöst.";
        assertEquals(expected, summary(definition, config));

        task.getNodeData().remove(FitConnectTriggerNodeV1.INITIAL_DATA_KEY_STARTED);
        assertEquals(expected, summary(definition, config));

        instance.setStarted(null);
        assertEquals("Der Vorgang wurde durch eine über FIT-Connect empfangene Einreichung ausgelöst.", summary(definition, config));
    }

    @Test
    void fitConnectSendSummaryDescribesTheSubmissionWithEscapedConfigurationDetails() throws Exception {
        var definition = instantiate(FitConnectSendJsonActionNodeV1.class);
        var config = new FitConnectSendJsonActionNodeV1.SendDataFitConnectActionNodeV1Config();
        config.serviceName = "Leistung [Alt] *melden*\n<zuordnen>";
        config.destinationId = "22cc92dc-6145-4d29-ab18-4e8b4baaf3a8";
        config.environment = " stage ";
        task.setNodeData(Map.of());
        task.setRuntimeData(Map.of());
        instance.setFinished(Instant.parse("2026-10-01T03:00:00Z"));
        node.setDescription("Daten *einreichen*\nund <zuordnen>.");

        var markdown = summary(definition, config);

        assertEquals("Der JSON-Datensatz wurde am 01.10.2026 um 12:00:00 Uhr über FIT-Connect eingereicht."
                + "\n\nBezeichnung der Verwaltungsleistung\n:   Leistung \\[Alt\\] \\*melden\\* \\<zuordnen\\>"
                + "\n\nEmpfänger\\-Zustellpunkt\\-ID\n:   22cc92dc\\-6145\\-4d29\\-ab18\\-4e8b4baaf3a8"
                + "\n\nFIT\\-Connect\\-Umgebung\n:   STAGE"
                + "\n\nKurzbeschreibung\n:   Daten \\*einreichen\\* und \\<zuordnen\\>\\.", markdown);
        assertTrue(definition.getOutputs().isEmpty());
        assertTrue(task.getNodeData().isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \n\t"})
    void fitConnectSendSummaryOmitsMissingHistoricalDetailsAndCompletionTime(String missingValue) throws Exception {
        var config = new FitConnectSendJsonActionNodeV1.SendDataFitConnectActionNodeV1Config();
        config.serviceName = missingValue;
        config.destinationId = missingValue;
        config.environment = missingValue;
        task.setNodeData(Map.of());
        task.setRuntimeData(Map.of());
        task.setFinished(null);
        node.setDescription(missingValue);

        assertEquals("Der JSON-Datensatz wurde über FIT-Connect eingereicht.",
                summary(instantiate(FitConnectSendJsonActionNodeV1.class), config));
    }

    @Test
    void emailUsesSavedMessageAndReconstructsRecipientsFromArraysAndJsonLists() throws Exception {
        var definition = instantiate(EMailActionNodeV1.class);
        var config = new EMailActionNodeV1.EMailActionNodeConfig();
        config.to = "later@example.test";
        config.bcc = "later-bcc@example.test";
        config.messageConfig = new SemiAutomaticMessageConfig.LayoutConfig();
        config.messageConfig.automaticContent = new SemiAutomaticMessageConfig.AutomaticContent();
        config.messageConfig.automaticContent.subject = "Späterer Betreff";
        config.messageConfig.automaticContent.content = "Spätere Nachricht";
        task.getNodeData().put("to", new String[]{"\"Ada", " Lovelace\" <ada@example.test>", " team@example.test"});
        task.getNodeData().put("bcc", new String[]{"archiv@example.test"});
        task.getNodeData().put("subject", "Hinweis [A] *prüfen*");
        task.getNodeData().put("content", "**Erster Absatz**\n\n- Geprüft\n- Zweiter Punkt");
        var expected = "Die E-Mail wurde am 01.10.2026 um 12:00:00 Uhr versendet."
                + "\n\nE\\-Mail\\-Adressen\n:   \"Ada, Lovelace\" \\<ada@example\\.test\\>, team@example\\.test"
                + "\n\nBlindkopie \\(BCC\\)\n:   archiv@example\\.test"
                + "\n\nBetreff der E\\-Mail\n:   Hinweis \\[A\\] \\*prüfen\\*"
                + "\n\nNachricht\n:   **Erster Absatz**\n    \n    - Geprüft\n    - Zweiter Punkt";
        assertEquals(expected, summary(definition, config));

        node.setDescription("Eingang [zuordnen] *und prüfen*");
        expected += "\n\nKurzbeschreibung\n:   Eingang \\[zuordnen\\] \\*und prüfen\\*";
        assertEquals(expected, summary(definition, config));

        var mapper = JsonMapperTestUtils.createMapper();
        task.setNodeData(mapper.readValue(mapper.writeValueAsString(task.getNodeData()), Map.class));
        assertInstanceOf(List.class, task.getNodeData().get("to"));
        assertInstanceOf(List.class, task.getNodeData().get("bcc"));
        assertEquals(expected, summary(definition, config));
    }

    @Test
    void emailOmitsAbsentAndEmptyMessageDetails() throws Exception {
        var definition = instantiate(EMailActionNodeV1.class);
        var config = new EMailActionNodeV1.EMailActionNodeConfig();
        var expected = "Die E-Mail wurde am 01.10.2026 um 12:00:00 Uhr versendet.";
        task.setNodeData(new HashMap<>());
        assertEquals(expected, summary(definition, config));

        task.getNodeData().put("to", new String[0]);
        task.getNodeData().put("bcc", List.of());
        task.getNodeData().put("subject", " \n\t");
        task.getNodeData().put("content", " \n\t");
        node.setDescription(" \n\t");
        assertEquals(expected, summary(definition, config));

        task.getNodeData().put("to", List.of());
        task.getNodeData().put("bcc", new String[0]);
        task.getNodeData().put("subject", null);
        task.getNodeData().put("content", null);
        node.setDescription(null);
        assertEquals(expected, summary(definition, config));
    }

    @Test
    void identityMessageUsesSavedRecipientAndContentAndIncludesOptionalDescription() throws Exception {
        var definition = instantiate(CommunicationMessageActionNodeV1.class);
        var config = new CommunicationMessageActionNodeV1.Configuration();
        config.identityId = "a-later-recipient";
        config.messageConfig = new SemiAutomaticMessageConfig.LayoutConfig();
        config.messageConfig.automaticContent = new SemiAutomaticMessageConfig.AutomaticContent();
        config.messageConfig.automaticContent.subject = "Späterer Betreff";
        config.messageConfig.automaticContent.content = "Spätere Nachricht";
        var identities = new IdentityDataMap();
        identities.put("customer", new IdentityData("session", "customer", IdentityType.Email,
                null, null, null, "person@example.test", Map.of(), null, Map.of(), "Empfang [A] *B*"));
        instance.setIdentities(identities);
        task.getNodeData().put("subject", "Betreff [A] *prüfen*");
        task.getNodeData().put("body", "**Erster Absatz**\n\n- Frage\n- Zweiter Punkt");
        task.getNodeData().put("sentAt", Instant.parse("2026-10-01T05:00:00Z"));
        task.setRuntimeData(ExecutionSummaryMarkdown.withMetadata(null, Map.of(
                ExecutionSummaryMarkdown.SENT_AT, "2026-10-01T07:00:00Z",
                ExecutionSummaryMarkdown.DELIVERY_CHANNEL, "Postfach [Alt]")));
        var expected = "Die Nachricht wurde am 01.10.2026 um 09:00:00 Uhr versendet."
                + "\n\nEmpfangende Identität\n:   die Identität „Empfang \\[A\\] \\*B\\*“"
                + "\n\nZustellweg\n:   Postfach \\[Alt\\]"
                + "\n\nBetreff der Nachricht\n:   Betreff \\[A\\] \\*prüfen\\*"
                + "\n\nNachricht\n:   **Erster Absatz**\n    \n    - Frage\n    - Zweiter Punkt";
        assertEquals(expected, summary(definition, config));

        node.setDescription("Antwort [zuordnen] *und dokumentieren*");
        assertEquals(expected + "\n\nKurzbeschreibung\n:   Antwort \\[zuordnen\\] \\*und dokumentieren\\*",
                summary(definition, config));
    }

    static Stream<Arguments> identityMessageDispatchTimes() {
        return Stream.of(
                Arguments.of(Instant.parse("2026-10-01T07:00:00Z"), true, " am 01.10.2026 um 09:00:00 Uhr"),
                Arguments.of(null, true, " am 01.10.2026 um 12:00:00 Uhr"),
                Arguments.of("kein gültiger Zeitpunkt", true, " am 01.10.2026 um 12:00:00 Uhr"),
                Arguments.of(null, false, ""),
                Arguments.of("kein gültiger Zeitpunkt", false, "")
        );
    }

    @ParameterizedTest
    @MethodSource("identityMessageDispatchTimes")
    void identityMessageUsesDispatchReceiptOrCompletionInsteadOfPreparationTime(
            Object dispatchTime, boolean hasFinished, String expectedTime) throws Exception {
        var receipt = new HashMap<String, Object>();
        receipt.put(ExecutionSummaryMarkdown.SENT_AT, dispatchTime);
        receipt.put(ExecutionSummaryMarkdown.DELIVERY_CHANNEL, " \n\t");
        task.setRuntimeData(ExecutionSummaryMarkdown.withMetadata(null, receipt));
        task.setNodeData(new HashMap<>(Map.of(
                "sentAt", Instant.parse("2026-10-01T05:00:00Z"),
                "subject", " \n\t", "body", " \n\t")));
        if (!hasFinished) {
            task.setFinished(null);
        }
        node.setDescription(" \n\t");
        var definition = instantiate(CommunicationMessageActionNodeV1.class);

        assertEquals("Die Nachricht wurde" + expectedTime + " versendet.",
                summary(definition, new CommunicationMessageActionNodeV1.Configuration()));
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
        task.getNodeData().put("paymentPurpose", "Gebühr [A]");
        task.getNodeData().put("paymentDescription", "Beschreibung *A*");
        task.getNodeData().put("paymentProviderName", "Zahlungsanbieter [Alt]");
        task.setDeadlineKey("payment");
        task.setDeadline(Instant.parse("2026-10-03T10:00:00Z"));
        node.setDescription("Zahlung *zuordnen*\nund <prüfen>.");
        var information = new XBezahldienstePaymentInformation();
        information.setTransactionTimestamp("2026-10-01T09:00:00Z");
        information.setTransactionReference("ref-1");
        information.setTransactionUrl(URI.create("https://secret.example/token"));
        task.getNodeData().put("paymentDetails", information);
        var definition = instantiate(PaymentRequestActionNodeV1.class);
        var config = new PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig();
        var typed = summary(definition, config);
        assertEquals("Die Zahlungsaufforderung wurde am 01.10.2026 um 09:00:00 Uhr an die Identität „customer“ über E\\-Mail versendet."
                + " Die Aufgabe wurde am 01.10.2026 um 12:00:00 Uhr mit bestätigter Zahlung abgeschlossen."
                + "\n\nVerwendungszweck\n:   Gebühr \\[A\\]"
                + "\n\nBeschreibung\n:   Beschreibung \\*A\\*"
                + "\n\nGesamtbetrag \\(Euro\\)\n:   20,00"
                + "\n\nZahlungsanbieter\n:   Zahlungsanbieter \\[Alt\\]"
                + "\n\nZahlungsreferenz\n:   ref\\-1"
                + "\n\nZahlungsfrist\n:   03\\.10\\.2026 um 12:00:00 Uhr"
                + "\n\nZahlungspositionen\n:   - Gebühr \\*A\\*: 20,00 Euro"
                + "\n\nKurzbeschreibung\n:   Zahlung \\*zuordnen\\* und \\<prüfen\\>\\.", typed);
        assertFalse(typed.contains("11:00:00"));
        assertFalse(typed.contains("secret.example"));
        var mapper = JsonMapperTestUtils.createMapper();
        task.setNodeData(mapper.readValue(mapper.writeValueAsString(task.getNodeData()), Map.class));
        task.setRuntimeData(mapper.readValue(mapper.writeValueAsString(task.getRuntimeData()), Map.class));
        assertEquals(typed, summary(definition, config));
    }

    static Stream<Arguments> paymentDispatchReceipts() {
        return Stream.of(
                Arguments.of(null, false, ""),
                Arguments.of("", true, " am 01.10.2026 um 12:00:00 Uhr"),
                Arguments.of("kein gültiger Zeitpunkt", true, " am 01.10.2026 um 12:00:00 Uhr"),
                Arguments.of(" \n\t", true, " am 01.10.2026 um 12:00:00 Uhr")
        );
    }

    @ParameterizedTest
    @MethodSource("paymentDispatchReceipts")
    void paymentWithoutAValidDispatchReceiptDoesNotClaimAMessageWasSent(
            String sentAt, boolean hasFinished, String expectedCompletionTime) throws Exception {
        task.setNodeData(Map.of(
                "recipientIdentityId", "customer",
                "paymentDetails", Map.of("transactionTimestamp", "2026-10-01T09:00:00Z")
        ));
        task.setRuntimeData(sentAt == null ? null : ExecutionSummaryMarkdown.withMetadata(null, Map.of(
                ExecutionSummaryMarkdown.SENT_AT, sentAt,
                ExecutionSummaryMarkdown.DELIVERY_CHANNEL, "E-Mail")));
        task.setStarted(Instant.parse("2026-10-01T01:00:00Z"));
        if (!hasFinished) {
            task.setFinished(null);
        }

        assertEquals("Die Zahlungsaufforderung richtet sich an die Identität „customer“. Die Aufgabe wurde"
                        + expectedCompletionTime + " mit bestätigter Zahlung abgeschlossen.",
                summary(instantiate(PaymentRequestActionNodeV1.class), new PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig()));
    }

    @ParameterizedTest
    @CsvSource({"messageDispatch, true", ", true", "payment, false"})
    void paymentSummaryDoesNotShowADispatchDeadlineOrAnAbsentPaymentDeadline(
            String deadlineKey, boolean hasDeadline) throws Exception {
        task.setDeadlineKey(deadlineKey);
        task.setDeadline(hasDeadline ? Instant.parse("2026-10-03T10:00:00Z") : null);

        var markdown = summary(instantiate(PaymentRequestActionNodeV1.class),
                new PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig());

        assertFalse(markdown.contains("Zahlungsfrist"));
        assertFalse(markdown.contains("03\\.10\\.2026"));
    }

    @Test
    void paymentListsPositionsWithAllFourTaxRateDecimals() throws Exception {
        var item = new PaymentItem();
        item.setDescription("Gebühr");
        item.setQuantity(1L);
        item.setNetPrice(new BigDecimal("10000.00"));
        item.setTaxRate(new BigDecimal("7.1234"));
        var runtime = new HashMap<>(task.getRuntimeData());
        runtime.put("paymentPayload", new PaymentPayload().setPaymentItems(List.of(item)));
        task.setRuntimeData(runtime);
        task.getNodeData().put("paymentTotal", item.getTotalPrice());

        var markdown = summary(instantiate(PaymentRequestActionNodeV1.class), new PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig());

        assertTrue(markdown.contains("Gebühr: 10.712,34 Euro (inkl. 7,1234 % Steuern)"), markdown);
    }

    @ParameterizedTest
    @CsvSource({"Antragstellende, Antragstellende", "'  Titel *A*  ', Titel \\*A\\*", ", customer", "'', customer", "'   ', customer"})
    void allIdentitySummariesUseTheTitleOrIdWithoutRepeatingTheId(String title, String expected) throws Exception {
        var identities = new IdentityDataMap();
        identities.put("customer", new IdentityData("session", "customer", IdentityType.Email,
                null, null, null, "person@example.test", Map.of(), null, Map.of(), title));
        instance.setIdentities(identities);
        for (var type : List.of(FormRequestActionNodeV1.class, PaymentRequestActionNodeV1.class, CommunicationMessageActionNodeV1.class)) {
            var definition = instantiate(type);
            var markdown = summary(definition, definition.getNodeConfigurationClass().getConstructor().newInstance());
            assertTrue(markdown.contains("die Identität „" + expected + "“"), markdown);
            assertFalse(markdown.contains("(customer)"), markdown);
        }
    }

    @Test
    void formRequestDoesNotAttributeTheInvitationToTheIdentityCollectedLater() throws Exception {
        var config = new FormRequestActionNodeV1.NodeConfig();
        config.recipientMode = "new";
        config.recipientEmailAddress = "invitation@example.test";
        var identities = new IdentityDataMap();
        identities.put("customer", new IdentityData("session", "customer", IdentityType.Email,
                null, null, null, "representative@example.test", Map.of(), null, Map.of(), "Vertretung"));
        instance.setIdentities(identities);

        var markdown = summary(instantiate(FormRequestActionNodeV1.class), config);
        var submissionStart = markdown.indexOf(" Die angeforderten Angaben wurden");

        assertTrue(submissionStart > 0, markdown);
        var dispatchStatement = markdown.substring(0, submissionStart);
        assertFalse(dispatchStatement.contains("Vertretung"), dispatchStatement);
        assertFalse(dispatchStatement.contains("Identität"), dispatchStatement);
        assertFalse(markdown.contains(config.recipientEmailAddress), markdown);
        assertTrue(markdown.endsWith("durch die Identität „Vertretung“ übermittelt."), markdown);
    }

    @Test
    void aiSummaryOmitsThePromptAndLongResponseAndEscapesSavedDetails() throws Exception {
        var response = "## Ergebnis\n\n- **Erster Punkt**\n\n| Feld | Wert |\n| --- | --- |\n| Ergebnis | 42 |\n\n"
                + "```json\n{\"ergebnis\": 42}\n```\n\n" + "Weiterer Absatz.\n\n".repeat(500);
        task.getNodeData().put("completion", response);
        task.getNodeData().put("responseModel", "test-model [Alt] <api>");
        task.getNodeData().put("prompt", "Nicht Teil der Antwort");
        task.getNodeData().put("finishReason", "length");
        task.getNodeData().put("usage", Map.of("prompt_tokens", 11, "completion_tokens", 7, "total_tokens", 18));
        node.setDescription("Antwort *weiterverwenden*\nund <prüfen>.");
        var config = new AiCompletionActionNodeV1.AiCompletionActionNodeConfig();
        config.model = "later-model";
        config.prompt = "Spätere Anfrage";

        var markdown = summary(new AiCompletionActionNodeV1(null, null, null), config);

        assertEquals("Die KI-Anfrage wurde am 01.10.2026 um 12:00:00 Uhr abgeschlossen."
                + "\n\nVerwendetes Modell\n:   test\\-model \\[Alt\\] \\<api\\>"
                + "\n\nKurzbeschreibung\n:   Antwort \\*weiterverwenden\\* und \\<prüfen\\>\\.", markdown);
        assertFalse(markdown.contains("Weiterer Absatz"));
        assertFalse(markdown.contains("Nicht Teil der Antwort"));
        assertFalse(markdown.contains(config.model));
    }

    @Test
    void aiSummaryOmitsMissingHistoricalMetadataAndDoesNotUseTheCurrentConfigurationOrAnotherTimestamp() throws Exception {
        task.setNodeData(Map.of());
        task.setRuntimeData(null);
        task.setFinished(null);
        task.setStarted(Instant.parse("2026-10-01T05:00:00Z"));
        node.setDescription(" \n\t");
        var config = new AiCompletionActionNodeV1.AiCompletionActionNodeConfig();
        config.model = "current-model";
        config.prompt = "Aktuelle Anfrage";

        assertEquals("Die KI-Anfrage wurde abgeschlossen.",
                summary(new AiCompletionActionNodeV1(null, null, null), config));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \n\t"})
    void aiSummaryOmitsEmptyModelAndDescription(String missingValue) throws Exception {
        task.getNodeData().put("completion", "Die Antwort wird ausschließlich in den Elementdaten angezeigt.");
        task.getNodeData().put("responseModel", missingValue);
        node.setDescription(missingValue);
        var markdown = summary(new AiCompletionActionNodeV1(null, null, null), new AiCompletionActionNodeV1.AiCompletionActionNodeConfig());
        assertEquals("Die KI-Anfrage wurde am 01.10.2026 um 12:00:00 Uhr abgeschlossen.", markdown);
    }

    @Test
    void aiTransformationSummaryOmitsLongResultsAndFieldListsAndEscapesSavedDetails() throws Exception {
        var keys = new ArrayList<String>();
        for (var index = 0; index < 500; index++) {
            keys.add("ergebnisfeld-" + index);
        }
        task.getNodeData().put("topLevelKeys", keys);
        task.getNodeData().put("prompt", "Vorbereiteter Auftrag mit vertraulichen Angaben");
        task.getNodeData().put("responseModel", "gespeichert [Alt] *Modell* <api>");
        task.getNodeData().put("finishReason", "length");
        task.getNodeData().put("usage", Map.of("prompt_tokens", 42, "completion_tokens", 17, "total_tokens", 59));
        task.setProcessData(Map.of("ergebnis", "Langer Ergebnisabschnitt. ".repeat(500)));
        node.setDescription("Vorgangsdaten *umwandeln*\nund <übernehmen>.");
        var config = new AiProcessDataTransformationActionNodeV1.AiProcessDataTransformationActionNodeConfig();
        config.model = "later-model";
        config.prompt = "Späterer Auftrag";

        var markdown = summary(new AiProcessDataTransformationActionNodeV1(null, null, null), config);

        assertEquals("Die Umwandlung der Vorgangsdaten mit KI wurde am 01.10.2026 um 12:00:00 Uhr abgeschlossen."
                + "\n\nVerwendetes Modell\n:   gespeichert \\[Alt\\] \\*Modell\\* \\<api\\>"
                + "\n\nKurzbeschreibung\n:   Vorgangsdaten \\*umwandeln\\* und \\<übernehmen\\>\\.", markdown);
        assertFalse(markdown.contains("ergebnisfeld"));
        assertFalse(markdown.contains("Langer Ergebnisabschnitt"));
        assertFalse(markdown.contains("vertraulichen Angaben"));
        assertFalse(markdown.contains(config.model));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \n\t"})
    void aiTransformationSummaryOmitsMissingHistoricalDetailsAndCompletionTime(String missingValue) throws Exception {
        var data = new HashMap<String, Object>();
        if (missingValue != null) {
            data.put("responseModel", missingValue);
        }
        task.setNodeData(data);
        task.setRuntimeData(null);
        task.setFinished(null);
        task.setStarted(Instant.parse("2026-10-01T05:00:00Z"));
        node.setDescription(missingValue);
        var config = new AiProcessDataTransformationActionNodeV1.AiProcessDataTransformationActionNodeConfig();
        config.model = "current-model";
        config.prompt = "Aktueller Auftrag";

        assertEquals("Die Umwandlung der Vorgangsdaten mit KI wurde abgeschlossen.",
                summary(new AiProcessDataTransformationActionNodeV1(null, null, null), config));
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
        return definition.generateExecutionSummary(new ProcessNodeExecutionSummaryContext<>(config, node, instance, task, null, actor, null, port));
    }
}
