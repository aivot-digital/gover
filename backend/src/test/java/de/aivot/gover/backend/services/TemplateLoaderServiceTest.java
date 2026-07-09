package de.aivot.gover.backend.services;

import de.aivot.gover.backend.elements.models.elements.form.input.CheckboxInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.ChipInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.DateTimeInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.DateRangeInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.DateTimeRangeInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.MapPointInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.MapPointInputElementValue;
import de.aivot.gover.backend.elements.models.elements.form.input.RangeInputElementValue;
import de.aivot.gover.backend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.TimeRangeInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.gover.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.gover.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.gover.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.gover.backend.elements.models.elements.steps.GenericStepElement;
import de.aivot.gover.backend.elements.models.elements.steps.IntroductionStepElement;
import de.aivot.gover.backend.department.entities.DepartmentEntity;
import de.aivot.gover.backend.department.entities.VDepartmentShadowedEntity;
import de.aivot.gover.backend.enums.DateType;
import de.aivot.gover.backend.enums.TimeType;
import de.aivot.gover.backend.models.config.GoverConfig;
import de.aivot.gover.backend.pdf.enums.FormPdfScope;
import de.aivot.gover.backend.pdf.models.FormPdfContext;
import de.aivot.gover.backend.pdf.models.PrintableFormPdfData;
import de.aivot.gover.backend.services.pdf.MarkdownDialect;
import de.aivot.gover.backend.services.pdf.PdfElement;
import de.aivot.gover.backend.services.pdf.PdfElementsGenerator;
import de.aivot.gover.backend.utils.ApplicationTimeZone;
import org.junit.jupiter.api.Test;
import org.thymeleaf.templatemode.TemplateMode;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateLoaderServiceTest {

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
                        "base", createBaseContext(FormPdfScope.Citizen),
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
        assertTrue(html.contains("Bewirtschaftende Stelle"));
        assertTrue(html.contains("Responsible Street 1"));
        assertTrue(html.contains("Managing Street 2"));
        assertFalse(html.contains("Responsible Department Name"));
        assertFalse(html.contains("Managing Department Name"));
        assertFalse(html.contains("Name der Kommune"));
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

        assertTrue(html.contains("Hinweise zur Einreichung des ausgefüllten Antrages"));
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
                        "value", new RangeInputElementValue()
                ),
                TemplateMode.HTML
        );
        var timeRangeHtml = new TemplateLoaderService().processTemplate(
                "form-parts/elements/time-range-field.html",
                Map.of(
                        "base", createBaseContext(FormPdfScope.Blank),
                        "element", timeRangeElement,
                        "value", new RangeInputElementValue()
                ),
                TemplateMode.HTML
        );
        var dateTimeRangeHtml = new TemplateLoaderService().processTemplate(
                "form-parts/elements/date-time-range-field.html",
                Map.of(
                        "base", createBaseContext(FormPdfScope.Blank),
                        "element", dateTimeRangeElement,
                        "value", new RangeInputElementValue()
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

        var dateRangeValue = new RangeInputElementValue(
                ZonedDateTime.of(2025, 1, 2, 0, 0, 0, 0, ApplicationTimeZone.getZoneId()),
                ZonedDateTime.of(2025, 1, 5, 0, 0, 0, 0, ApplicationTimeZone.getZoneId())
        );
        var timeRangeValue = new RangeInputElementValue(
                ZonedDateTime.of(2025, 1, 2, 9, 15, 30, 0, ApplicationTimeZone.getZoneId()),
                ZonedDateTime.of(2025, 1, 2, 11, 45, 15, 0, ApplicationTimeZone.getZoneId())
        );
        var dateTimeRangeValue = new RangeInputElementValue(
                ZonedDateTime.of(2025, 1, 2, 9, 15, 0, 0, ApplicationTimeZone.getZoneId()),
                ZonedDateTime.of(2025, 1, 5, 17, 0, 0, 0, ApplicationTimeZone.getZoneId())
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
                                                ZonedDateTime.of(2025, 1, 2, 9, 15, 30, 0, ApplicationTimeZone.getZoneId()),
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
        var goverConfig = new GoverConfig();
        goverConfig.setGoverHostname("https://gover.example/");
        return new FormPdfContext("", "", "", goverConfig, scope);
    }
}
