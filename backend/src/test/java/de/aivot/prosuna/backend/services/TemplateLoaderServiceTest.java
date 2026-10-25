package de.aivot.prosuna.backend.services;

import de.aivot.prosuna.backend.elements.models.elements.form.input.DurationInputElement;
import de.aivot.prosuna.backend.enums.DurationPrecision;
import de.aivot.prosuna.backend.enums.DurationLargestUnit;
import java.math.BigDecimal;

import de.aivot.prosuna.backend.elements.models.elements.form.input.CheckboxInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.ChipInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.DateTimeInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.DateRangeInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.DateTimeRangeInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.MapPointInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.MapPointInputElementValue;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RangeInputElementValue;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TimeRangeInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.steps.GenericStepElement;
import de.aivot.prosuna.backend.elements.models.elements.steps.IntroductionStepElement;
import de.aivot.prosuna.backend.department.entities.DepartmentEntity;
import de.aivot.prosuna.backend.department.entities.VDepartmentShadowedEntity;
import de.aivot.prosuna.backend.enums.DateType;
import de.aivot.prosuna.backend.enums.TimeType;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentInformation;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentRequest;
import de.aivot.prosuna.backend.pdf.enums.FormPdfScope;
import de.aivot.prosuna.backend.pdf.models.FormPdfContext;
import de.aivot.prosuna.backend.pdf.models.PrintableFormPdfData;
import de.aivot.prosuna.backend.services.pdf.MarkdownDialect;
import de.aivot.prosuna.backend.services.pdf.PdfElement;
import de.aivot.prosuna.backend.services.pdf.PdfElementsGenerator;
import org.junit.jupiter.api.Test;
import org.thymeleaf.exceptions.TemplateInputException;
import org.thymeleaf.templatemode.TemplateMode;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateLoaderServiceTest {

    @Test
    void durationFieldRendersThroughDispatcherAndSupportsBlankPrint() {
        var field = new DurationInputElement().setPrecision(DurationPrecision.Second);
        field.setLabel("Aufwand");
        field.setRequired(true);
        var pdfElement = new PdfElement(field, BigDecimal.valueOf(99015), List.of());
        var html = new TemplateLoaderService().processTemplate(
                "form-parts/element-dispatcher.html",
                Map.of("base", createBaseContext(FormPdfScope.Staff), "child", pdfElement),
                TemplateMode.HTML);
        assertTrue(html.contains("27 Stunden 30 Minuten 15 Sekunden"));
        assertTrue(html.contains("Aufwand"));
        var blank = new TemplateLoaderService().processTemplate(
                "form-parts/element-dispatcher.html",
                Map.of("base", createBaseContext(FormPdfScope.Blank), "child", pdfElement),
                TemplateMode.HTML);
        assertTrue(blank.contains("Stunden / Minuten / Sekunden"));
        assertTrue(blank.contains("Pflichtangabe"));
        assertFalse(blank.contains("27 Stunden"));
        field.setLargestUnit(DurationLargestUnit.Day);
        var withDays = new TemplateLoaderService().processTemplate(
                "form-parts/element-dispatcher.html",
                Map.of("base", createBaseContext(FormPdfScope.Staff), "child", pdfElement),
                TemplateMode.HTML);
        assertTrue(withDays.contains("1 Tag 3 Stunden 30 Minuten 15 Sekunden"));
        for (var precision : DurationPrecision.values()) {
            field.setPrecision(precision);
            var blankDays = new TemplateLoaderService().processTemplate(
                    "form-parts/element-dispatcher.html",
                    Map.of("base", createBaseContext(FormPdfScope.Blank), "child", pdfElement),
                    TemplateMode.HTML);
            assertTrue(blankDays.contains("Tage / Stunden / Minuten"));
            assertEquals(precision == DurationPrecision.Second, blankDays.contains(" / Sekunden"));
        }
    }

    @Test
    void processTemplate_DoesNotResolveTemplatesFromAssetStorage() throws IOException {
        // The default local asset storage is rooted at ./data/assets/. Files stored there must never become templates.
        var assetDirectory = Path.of("data", "assets");
        var createdDirectories = new LinkedList<Path>();
        for (var directory = assetDirectory; directory != null && !Files.exists(directory); directory = directory.getParent()) {
            createdDirectories.addFirst(directory);
        }
        Files.createDirectories(assetDirectory);
        var templateName = "template-loader-test-" + UUID.randomUUID() + ".html";
        var assetFile = assetDirectory.resolve(templateName);
        Files.writeString(assetFile, "<p>Vorlage aus den Dateien & Medien</p>");

        try {
            assertThrows(
                    TemplateInputException.class,
                    () -> new TemplateLoaderService().processTemplate(templateName, Map.of(), TemplateMode.HTML)
            );
        } finally {
            Files.deleteIfExists(assetFile);
            var directories = createdDirectories.descendingIterator();
            while (directories.hasNext()) {
                Files.deleteIfExists(directories.next());
            }
        }
    }

    @Test
    void processTemplateContent_RendersContentWithTemplateDataAndBundledFragments() {
        var html = new TemplateLoaderService().processTemplateContent(
                """
                        <style th:insert="./form-parts/style.html"></style>
                        <h1 th:text="${title}">Titel</h1>
                        """,
                Map.of(
                        "title", "Antrag auf Beispielleistung",
                        "base", createBaseContext(FormPdfScope.Blank)
                ),
                TemplateMode.HTML
        );

        assertTrue(html.contains("<h1>Antrag auf Beispielleistung</h1>"));
        assertFalse(html.contains("<style></style>"));
    }

    @Test
    void processTemplate_DoesNotResolveTemplatesOutsideTheTemplateDirectories() throws IOException {
        try (var outsideFiles = OutsideTemplateFiles.create()) {
            for (var templateName : outsideFiles.traversingTemplateNames()) {
                assertThrows(
                        TemplateInputException.class,
                        () -> new TemplateLoaderService().processTemplate(templateName, Map.of(), TemplateMode.HTML),
                        templateName
                );
            }
        }
    }

    @Test
    void processTemplateContent_DoesNotIncludeFilesOutsideTheTemplateDirectories() throws IOException {
        try (var outsideFiles = OutsideTemplateFiles.create()) {
            for (var templateName : outsideFiles.traversingTemplateNames()) {
                assertThrows(
                        TemplateInputException.class,
                        () -> new TemplateLoaderService().processTemplateContent(
                                "<div th:insert=\"~{" + templateName + "}\"></div>",
                                Map.of(),
                                TemplateMode.HTML
                        ),
                        templateName
                );
            }
        }
    }

    @Test
    void processTemplateContent_DoesNotIncludeBundledTemplatesOutsideTheFormFragments() {
        assertThrows(
                TemplateInputException.class,
                () -> new TemplateLoaderService().processTemplateContent(
                        "<div th:insert=\"~{pp_form_header.html}\"></div>",
                        Map.of(),
                        TemplateMode.HTML
                )
        );
    }

    @Test
    void normalizeTemplateName_RejectsNamesThatCouldLeaveTheTemplateDirectory() {
        assertEquals("form-parts/style.html", TemplateLoaderService.normalizeTemplateName("./form-parts//style.html"));
        assertNull(TemplateLoaderService.normalizeTemplateName("form-parts/../form.html"));
        assertNull(TemplateLoaderService.normalizeTemplateName("..\\application.yml"));
        assertNull(TemplateLoaderService.normalizeTemplateName("./"));
        assertNull(TemplateLoaderService.normalizeTemplateName(null));
    }

    /**
     * Files that template names with parent segments would reach: one in the working directory next to
     * {@code ./templates/} and one in the default local asset storage, and a resource on the classpath next to the
     * bundled templates.
     */
    private record OutsideTemplateFiles(
            Path workingDirectoryFile,
            Path assetFile,
            List<Path> createdDirectories
    ) implements AutoCloseable {
        static OutsideTemplateFiles create() throws IOException {
            var createdDirectories = new ArrayList<Path>();
            // The file resolver can only follow parent segments if ./templates/ exists, as on installations with overrides.
            createDirectories(Path.of("templates"), createdDirectories);
            var assetDirectory = Path.of("data", "assets");
            createDirectories(assetDirectory, createdDirectories);

            var fileName = "template-loader-test-" + UUID.randomUUID() + ".html";
            var workingDirectoryFile = Files.writeString(Path.of(fileName), "<p>Datei außerhalb der Vorlagen</p>");
            var assetFile = Files.writeString(assetDirectory.resolve(fileName), "<p>Vorlage aus den Dateien & Medien</p>");
            return new OutsideTemplateFiles(workingDirectoryFile, assetFile, createdDirectories);
        }

        List<String> traversingTemplateNames() {
            return List.of(
                    "../" + workingDirectoryFile.getFileName(),
                    "./form-parts/../../" + workingDirectoryFile.getFileName(),
                    "../data/assets/" + assetFile.getFileName(),
                    "../application.yml"
            );
        }

        @Override
        public void close() throws IOException {
            Files.deleteIfExists(workingDirectoryFile);
            Files.deleteIfExists(assetFile);
            for (var directory : createdDirectories.reversed()) {
                Files.deleteIfExists(directory);
            }
        }

        private static void createDirectories(Path directory, List<Path> createdDirectories) throws IOException {
            var missingDirectories = new LinkedList<Path>();
            for (var current = directory; current != null && !Files.exists(current); current = current.getParent()) {
                missingDirectories.addFirst(current);
            }
            Files.createDirectories(directory);
            createdDirectories.addAll(missingDirectories);
        }
    }

    @Test
    void checkboxTemplate_RendersHintOnlyOnceInBlankPrint() {
        var element = new CheckboxInputElement()
                .setLabel("Bestätigung")
                .setHint("Bitte bestätigen Sie die Angaben.")
                .setRequired(true);
        var html = new TemplateLoaderService().processTemplate(
                "form-parts/elements/checkbox.html",
                Map.of(
                        "base", createBaseContext(FormPdfScope.Blank),
                        "element", element,
                        "value", false
                ),
                TemplateMode.HTML
        );

        assertEquals(1, countOccurrences(html, "Bitte bestätigen Sie die Angaben."));
    }

    @Test
    void richTextFieldTemplate_RendersMarkdownAsHtml() {
        var element = new RichTextInputElement()
                .setLabel("Markdown-Eingabe");
        var html = new TemplateLoaderService().processTemplate(
                "form-parts/elements/rich-text-field.html",
                Map.of(
                        "base", createBaseContext(FormPdfScope.Customer),
                        "element", element,
                        "value", "~~Durchgestrichen~~\n\n- [ ] Offen\n- [x] Erledigt\n\n**Fett** und [Link](https://example.org)"
                ),
                TemplateMode.HTML
        );

        assertTrue(html.contains("<del>Durchgestrichen</del>"));
        assertTrue(html.contains("<li><input type=\"checkbox\" disabled=\"\"> Offen</li>"));
        assertTrue(html.contains("<li><input type=\"checkbox\" disabled=\"\" checked=\"\"> Erledigt</li>"));
        assertTrue(html.contains("<strong>Fett</strong>"));
        assertTrue(html.contains("href=\"https://example.org\""));
        assertTrue(html.contains(">Link<"));
        assertFalse(html.contains("~~Durchgestrichen~~"));
        assertFalse(html.contains("[ ] Offen"));
    }

    @Test
    void markdownDialect_RendersFrontendGfmFeatures() {
        var html = new MarkdownDialect().render("""
                ~Einfach~

                https://example.org/path

                | Spalte A | Spalte B |
                | --- | --- |
                | Wert 1 | Wert 2 |

                Referenz[^1]

                [^1]: Fussnote
                """);

        assertTrue(html.contains("<del>Einfach</del>"));
        assertTrue(html.contains("href=\"https://example.org/path\""));
        assertTrue(html.contains(">https://example.org/path<"));
        assertTrue(html.contains("<table>"));
        assertTrue(html.contains("<th>Spalte A</th>"));
        assertTrue(html.contains("<td>Wert 1</td>"));
        assertTrue(html.contains("<sup"));
        assertTrue(html.contains("Fussnote"));
        assertFalse(html.contains("| Spalte A |"));
        assertFalse(html.contains("[^1]"));
    }

    @Test
    void markdownDialect_PreservesSoftLineBreaks() {
        var html = new MarkdownDialect().render("Zeile 1\nZeile 2");

        assertTrue(html.contains("<p>Zeile 1<br />\nZeile 2</p>"));
    }

    @Test
    void briefkopfTemplate_RendersResponsibleAndManagingAddressesOnly() {
        var responsibleDepartment = new VDepartmentShadowedEntity()
                .setName("Responsible Department Name")
                .setPostalAddress("Responsible Street 1\n12345 Responsible City");
        var managingDepartment = new VDepartmentShadowedEntity()
                .setName("Managing Department Name")
                .setPostalAddress("Managing Street 2\n54321 Managing City");

        var html = new TemplateLoaderService().processTemplate(
                "form-parts/briefkopf.html",
                Map.of(
                        "base", createBaseContext(FormPdfScope.Blank),
                        "responsibleDepartment", responsibleDepartment,
                        "managingDepartment", managingDepartment
                ),
                TemplateMode.HTML
        );

        assertTrue(html.contains("Zuständige Stelle"));
        assertTrue(html.contains("Betreuende Stelle"));
        assertTrue(html.contains("Responsible Street 1"));
        assertTrue(html.contains("Managing Street 2"));
        assertFalse(html.contains("Responsible Department Name"));
        assertFalse(html.contains("Managing Department Name"));
        assertFalse(html.contains("Name der Kommune"));
        assertFalse(html.contains("<img"));
    }

    @Test
    void briefkopfTemplate_RendersInlineLogoWithoutExternalAssetUrl() {
        var html = new TemplateLoaderService().processTemplate(
                "form-parts/briefkopf.html",
                Map.of(
                        "base", createBaseContext(FormPdfScope.Blank, "data:image/png;base64,bG9nbw=="),
                        "responsibleDepartment", new VDepartmentShadowedEntity(),
                        "managingDepartment", new VDepartmentShadowedEntity()
                ),
                TemplateMode.HTML
        );

        assertTrue(html.contains("src=\"data:image/png;base64,bG9nbw==\""));
        assertFalse(html.contains("/api/public/assets/"));
    }

    @Test
    void paymentConfirmationTemplate_OmitsLogoContainerWithoutLogoUrl() {
        var paymentRequest = new XBezahldienstePaymentRequest();
        paymentRequest.setGrosAmount(BigDecimal.TEN);
        var paymentInformation = new XBezahldienstePaymentInformation();
        paymentInformation.setTransactionId("transaction-1");
        paymentInformation.setTransactionTimestamp("2026-09-14T10:00:00Z");
        var transaction = new PaymentTransactionEntity()
                .setPaymentRequest(paymentRequest)
                .setPaymentInformation(paymentInformation);
        var department = new VDepartmentShadowedEntity()
                .setName("Example Department")
                .setPostalAddress("Example Street 1");
        var context = new HashMap<String, Object>();
        context.put("transaction", transaction);
        context.put("caseNumber", "CASE-1");
        context.put("logoDataUrl", null);
        context.put("department", department);
        context.put("generatedAt", "2026-09-14T10:00:00Z");

        var html = new TemplateLoaderService().processTemplate(
                "payment-confirmation/form-trigger-payment-confirmation.html",
                context,
                TemplateMode.HTML
        );

        assertFalse(html.contains("id=\"logo\""));
        assertFalse(html.contains("<img"));
    }

    @Test
    void paymentConfirmationTemplate_RendersInlineLogoWithoutExternalAssetUrl() {
        var paymentRequest = new XBezahldienstePaymentRequest();
        paymentRequest.setGrosAmount(BigDecimal.TEN);
        var paymentInformation = new XBezahldienstePaymentInformation();
        paymentInformation.setTransactionId("transaction-1");
        paymentInformation.setTransactionTimestamp("2026-09-14T10:00:00Z");
        var transaction = new PaymentTransactionEntity()
                .setPaymentRequest(paymentRequest)
                .setPaymentInformation(paymentInformation);
        var context = new HashMap<String, Object>();
        context.put("transaction", transaction);
        context.put("caseNumber", "CASE-1");
        context.put("logoDataUrl", "data:image/svg+xml;base64,PHN2Zy8+");
        context.put("department", new VDepartmentShadowedEntity()
                .setName("Example Department")
                .setPostalAddress("Example Street 1"));
        context.put("generatedAt", "2026-09-14T10:00:00Z");

        var html = new TemplateLoaderService().processTemplate(
                "payment-confirmation/form-trigger-payment-confirmation.html",
                context,
                TemplateMode.HTML
        );

        assertTrue(html.contains("src=\"data:image/svg+xml;base64,PHN2Zy8+\""));
        assertFalse(html.contains("/api/public/assets/"));
    }

    @Test
    void formTemplate_BlankPrintDispatchesGroupLayoutAndReplicatingContainerLayout() {
        var groupField = new TextInputElement();
        groupField.setLabel("Feld in Gruppe");
        var group = new GroupLayoutElement()
                .setChildren(List.of(groupField));

        var replicatingField = new TextInputElement();
        replicatingField.setLabel("Feld in replizierender Liste");
        var replicatingContainer = new ReplicatingContainerLayoutElement()
                .setChildren(List.of(replicatingField));
        replicatingContainer.setLabel("Replizierende Liste");

        var rootElement = new FormLayoutElement()
                .setChildren(List.of(
                        new GenericStepElement()
                                .setTitle("Abschnitt")
                                .setChildren(List.of(group, replicatingContainer))
                ));

        var html = renderBlankForm(rootElement);

        assertTrue(html.contains("Feld in Gruppe"));
        assertTrue(html.contains("Replizierende Liste"));
        assertEquals(5, countOccurrences(html, "Feld in replizierender Liste"));
    }

    @Test
    void formTemplate_BlankPrintRendersOfflineSubmissionTextWithoutSignature() {
        var rootElement = new FormLayoutElement()
                .setOfflineSubmissionText("Bitte senden Sie den Antrag **postalisch** ein.")
                .setOfflineSignatureNeeded(false)
                .setChildren(List.of(
                        new GenericStepElement()
                                .setTitle("Abschnitt")
                ));

        var html = renderBlankForm(rootElement);

        assertTrue(html.contains("Hinweise zur Übermittlung des ausgefüllten Formulars"));
        assertTrue(html.contains("<strong>postalisch</strong>"));
        assertFalse(html.contains("Unterschrift"));
    }

    @Test
    void generalInformationTemplate_RendersTeaserMarkdownAsHtml() {
        var introductionStep = new IntroductionStepElement()
                .setTeaserText("**Wichtig**\n\n- Punkt A");
        var form = new PrintableFormPdfData()
                .setSlug("test-form")
                .setRootElement(new FormLayoutElement().setChildren(List.of(introductionStep)));
        var html = new TemplateLoaderService().processTemplate(
                "form-parts/allgemeine-informationen.html",
                Map.of(
                        "base", createBaseContext(FormPdfScope.Blank),
                        "form", form,
                        "step", new PdfElement(introductionStep, null, List.of())
                ),
                TemplateMode.HTML
        );

        assertTrue(html.contains("<strong>Wichtig</strong>"));
        assertTrue(html.contains("<li>Punkt A</li>"));
        assertFalse(html.contains("- Punkt A"));
    }

    @Test
    void rangeFieldTemplates_RenderBlankPrintPlaceholders() {
        var dateRangeElement = new DateRangeInputElement();
        dateRangeElement.setLabel("Monatszeitraum");
        dateRangeElement.setMode(DateType.Month);

        var timeRangeElement = new TimeRangeInputElement();
        timeRangeElement.setLabel("Sekundengenaue Zeitspanne");
        timeRangeElement.setMode(TimeType.Second);

        var dateTimeRangeElement = new DateTimeRangeInputElement();
        dateTimeRangeElement.setLabel("Sekundengenauer Terminzeitraum");
        dateTimeRangeElement.setMode(TimeType.Second);

        var dateRangeHtml = new TemplateLoaderService().processTemplate(
                "form-parts/elements/date-range-field.html",
                Map.of(
                        "base", createBaseContext(FormPdfScope.Blank),
                        "element", dateRangeElement,
                        "value", new RangeInputElementValue<>()
                ),
                TemplateMode.HTML
        );
        var timeRangeHtml = new TemplateLoaderService().processTemplate(
                "form-parts/elements/time-range-field.html",
                Map.of(
                        "base", createBaseContext(FormPdfScope.Blank),
                        "element", timeRangeElement,
                        "value", new RangeInputElementValue<>()
                ),
                TemplateMode.HTML
        );
        var dateTimeRangeHtml = new TemplateLoaderService().processTemplate(
                "form-parts/elements/date-time-range-field.html",
                Map.of(
                        "base", createBaseContext(FormPdfScope.Blank),
                        "element", dateTimeRangeElement,
                        "value", new RangeInputElementValue<>()
                ),
                TemplateMode.HTML
        );

        assertTrue(dateRangeHtml.contains("MM.JJJJ (Von/Bis)"));
        assertTrue(timeRangeHtml.contains("HH:MM:SS (Von/Bis)"));
        assertTrue(dateTimeRangeHtml.contains("TT.MM.JJJJ HH:MM:SS (Von/Bis)"));
    }

    @Test
    void additionalFieldTemplates_RenderBlankPrintPlaceholders() {
        var dateTimeElement = new DateTimeInputElement();
        dateTimeElement.setLabel("Termin");
        dateTimeElement.setMode(TimeType.Second);

        var chipInputElement = new ChipInputElement();
        chipInputElement.setLabel("Schlagwörter");
        chipInputElement.setMinItems(2);
        chipInputElement.setMaxItems(5);
        chipInputElement.setMaxItemLength(30);

        var mapPointElement = new MapPointInputElement()
                .setLabel("Position");

        var dateTimeHtml = new TemplateLoaderService().processTemplate(
                "form-parts/elements/date-time-field.html",
                Map.of(
                        "base", createBaseContext(FormPdfScope.Blank),
                        "element", dateTimeElement,
                        "value", ""
                ),
                TemplateMode.HTML
        );
        var chipInputHtml = new TemplateLoaderService().processTemplate(
                "form-parts/elements/chip-input-field.html",
                Map.of(
                        "base", createBaseContext(FormPdfScope.Blank),
                        "element", chipInputElement,
                        "value", List.of()
                ),
                TemplateMode.HTML
        );
        var mapPointHtml = new TemplateLoaderService().processTemplate(
                "form-parts/elements/map-point-field.html",
                Map.of(
                        "base", createBaseContext(FormPdfScope.Blank),
                        "element", mapPointElement,
                        "value", new MapPointInputElementValue()
                ),
                TemplateMode.HTML
        );

        assertTrue(dateTimeHtml.contains("TT.MM.JJJJ HH:MM:SS"));
        assertTrue(chipInputHtml.contains("Mehrere Einträge bitte kommasepariert nacheinander notieren."));
        assertTrue(chipInputHtml.contains("Mindestens"));
        assertTrue(chipInputHtml.contains("Maximal"));
        assertTrue(chipInputHtml.contains("Zeichen je Eintrag"));
        assertTrue(chipInputHtml.contains(">30</span>"));
        assertTrue(mapPointHtml.contains("Adresse oder Koordinaten"));
    }

    @Test
    void formTemplate_DispatchesAndRendersRangeFields() {
        var stepElement = new GenericStepElement()
                .setTitle("Zeitangaben");

        var dateRangeElement = new DateRangeInputElement();
        dateRangeElement.setLabel("Datumsspanne");
        dateRangeElement.setMode(DateType.Day);

        var timeRangeElement = new TimeRangeInputElement();
        timeRangeElement.setLabel("Zeitspanne");
        timeRangeElement.setMode(TimeType.Second);

        var dateTimeRangeElement = new DateTimeRangeInputElement();
        dateTimeRangeElement.setLabel("Terminspanne");
        dateTimeRangeElement.setMode(TimeType.Minute);

        var dateRangeValue = new RangeInputElementValue<>(
                LocalDate.of(2025, 1, 2),
                LocalDate.of(2025, 1, 5)
        );
        var timeRangeValue = new RangeInputElementValue<>(
                LocalTime.of(9, 15, 30),
                LocalTime.of(11, 45, 15)
        );
        var dateTimeRangeValue = new RangeInputElementValue<>(
                Instant.parse("2025-01-02T08:15:00Z"),
                Instant.parse("2025-01-05T16:00:00Z")
        );

        var form = new PrintableFormPdfData()
                .setPublicTitle("Testformular")
                .setRootElement(new FormLayoutElement());

        var html = new TemplateLoaderService().processTemplate(
                "form.html",
                Map.of(
                        "base", createBaseContext(FormPdfScope.Staff),
                        "form", form,
                        "elements", List.of(new PdfElement(
                                stepElement,
                                null,
                                List.of(
                                        new PdfElement(dateRangeElement, dateRangeValue, null),
                                        new PdfElement(timeRangeElement, timeRangeValue, null),
                                        new PdfElement(dateTimeRangeElement, dateTimeRangeValue, null)
                                )
                        )),
                        "attachments", List.of()
                ),
                TemplateMode.HTML
        );

        assertTrue(html.contains("Datumsspanne"));
        assertTrue(html.contains("02.01.2025 bis 05.01.2025"));
        assertTrue(html.contains("Zeitspanne"));
        assertTrue(html.contains("09:15:30 Uhr bis 11:45:15 Uhr"));
        assertTrue(html.contains("Terminspanne"));
        assertTrue(html.contains("02.01.2025 09:15 Uhr bis 05.01.2025 17:00 Uhr"));
    }

    @Test
    void formTemplate_DispatchesAndRendersDateTimeChipAndMapPointFields() {
        var stepElement = new GenericStepElement()
                .setTitle("Weitere Eingaben");

        var dateTimeElement = new DateTimeInputElement();
        dateTimeElement.setLabel("Termin");
        dateTimeElement.setMode(TimeType.Second);

        var chipInputElement = new ChipInputElement();
        chipInputElement.setLabel("Schlagwörter");

        var mapPointElement = new MapPointInputElement()
                .setLabel("Ort");

        var form = new PrintableFormPdfData()
                .setPublicTitle("Testformular")
                .setRootElement(new FormLayoutElement());

        var html = new TemplateLoaderService().processTemplate(
                "form.html",
                Map.of(
                        "base", createBaseContext(FormPdfScope.Staff),
                        "form", form,
                        "elements", List.of(new PdfElement(
                                stepElement,
                                null,
                                List.of(
                                        new PdfElement(
                                                dateTimeElement,
                                                Instant.parse("2025-01-02T08:15:30Z"),
                                                null
                                        ),
                                        new PdfElement(chipInputElement, List.of("Alpha", "Beta"), null),
                                        new PdfElement(
                                                mapPointElement,
                                                new MapPointInputElementValue(52.520008, 13.404954, "Alexanderplatz, Berlin"),
                                                null
                                        )
                                )
                        )),
                        "attachments", List.of()
                ),
                TemplateMode.HTML
        );

        assertTrue(html.contains("Termin"));
        assertTrue(html.contains("02.01.2025 09:15:30 Uhr"));
        assertTrue(html.contains("Schlagwörter"));
        assertTrue(html.contains("Alpha, Beta"));
        assertTrue(html.contains("Ort"));
        assertTrue(html.contains("Alexanderplatz, Berlin"));
        assertTrue(html.contains("52.520008") || html.contains("52,520008"));
        assertTrue(html.contains("13.404954") || html.contains("13,404954"));
    }

    @Test
    void formTemplate_BlankPrintDoesNotRenderPageBreakBeforeStepsWithoutIntroductionStep() {
        var firstStepElement = new GenericStepElement()
                .setTitle("Erster Abschnitt");
        var secondStepElement = new GenericStepElement()
                .setTitle("Zweiter Abschnitt");

        var form = new PrintableFormPdfData()
                .setSlug("testformular-ohne-intro")
                .setPublicTitle("Testformular")
                .setRootElement(new FormLayoutElement().setChildren(List.of(firstStepElement, secondStepElement)));
        var department = new DepartmentEntity()
                .setName("Formularservice")
                .setPostalAddress("Musterstrasse 1");

        var html = new TemplateLoaderService().processTemplate(
                "form.html",
                Map.of(
                        "base", createBaseContext(FormPdfScope.Blank),
                        "form", form,
                        "department", department,
                        "elements", List.of(
                                new PdfElement(firstStepElement, null, List.of()),
                                new PdfElement(secondStepElement, null, List.of())
                        ),
                        "attachments", List.of()
                ),
                TemplateMode.HTML
        );

        assertEquals(2, countOccurrences(html, "class=\"step\""));
        assertFalse(html.contains("page-break-before"));
    }

    @Test
    void formTemplate_BlankPrintDoesNotRenderPageBreakBeforeStepsWhenIntroductionStepExists() {
        var introductionStep = new IntroductionStepElement();
        var firstStepElement = new GenericStepElement()
                .setTitle("Erster Abschnitt");
        var secondStepElement = new GenericStepElement()
                .setTitle("Zweiter Abschnitt");

        var form = new PrintableFormPdfData()
                .setSlug("testformular-mit-intro")
                .setPublicTitle("Testformular")
                .setRootElement(new FormLayoutElement().setChildren(List.of(introductionStep, firstStepElement, secondStepElement)));
        var department = new DepartmentEntity()
                .setName("Formularservice")
                .setPostalAddress("Musterstrasse 1");

        var html = new TemplateLoaderService().processTemplate(
                "form.html",
                Map.of(
                        "base", createBaseContext(FormPdfScope.Blank),
                        "form", form,
                        "department", department,
                        "elements", List.of(
                                new PdfElement(firstStepElement, null, List.of()),
                                new PdfElement(secondStepElement, null, List.of())
                        ),
                        "attachments", List.of()
                ),
                TemplateMode.HTML
        );

        assertEquals(2, countOccurrences(html, "class=\"step\""));
        assertFalse(html.contains("page-break-before"));
    }

    private int countOccurrences(String haystack, String needle) {
        var count = 0;
        var currentIndex = 0;

        while ((currentIndex = haystack.indexOf(needle, currentIndex)) >= 0) {
            count++;
            currentIndex += needle.length();
        }

        return count;
    }

    private String renderBlankForm(FormLayoutElement rootElement) {
        var form = new PrintableFormPdfData()
                .setSlug("testformular")
                .setPublicTitle("Testformular")
                .setRootElement(rootElement);
        var department = new DepartmentEntity()
                .setName("Formularservice")
                .setPostalAddress("Musterstrasse 1");

        return new TemplateLoaderService().processTemplate(
                "form.html",
                Map.of(
                        "base", createBaseContext(FormPdfScope.Blank),
                        "form", form,
                        "department", department,
                        "elements", PdfElementsGenerator.generatePdfElements(rootElement, null, true),
                        "attachments", List.of()
                ),
                TemplateMode.HTML
        );
    }

    private FormPdfContext createBaseContext(FormPdfScope scope) {
        return createBaseContext(scope, null);
    }

    private FormPdfContext createBaseContext(FormPdfScope scope, String logoDataUrl) {
        var prosunaConfig = new ProsunaConfig();
        prosunaConfig.setProsunaHostname("https://prosuna.example/");
        return new FormPdfContext("", logoDataUrl, prosunaConfig, scope);
    }
}
