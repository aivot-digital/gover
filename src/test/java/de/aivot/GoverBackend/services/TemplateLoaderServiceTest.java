package de.aivot.GoverBackend.services;

import de.aivot.GoverBackend.elements.models.elements.form.input.CheckboxInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.ChipInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.DateInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.DateTimeInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.DateRangeInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.DateTimeRangeInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.MapPointInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.MapPointInputElementValue;
import de.aivot.GoverBackend.elements.models.elements.form.input.RangeInputElementValue;
import de.aivot.GoverBackend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.TimeRangeInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.steps.GenericStepElement;
import de.aivot.GoverBackend.elements.models.elements.steps.IntroductionStepElement;
import de.aivot.GoverBackend.enums.DateType;
import de.aivot.GoverBackend.enums.TimeType;
import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsEntity;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.pdf.enums.FormPdfScope;
import de.aivot.GoverBackend.pdf.models.FormPdfContext;
import de.aivot.GoverBackend.services.pdf.MarkdownDialect;
import de.aivot.GoverBackend.services.pdf.PdfElement;
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
    void generalInformationTemplate_RendersTeaserMarkdownAsHtml() {
        var introductionStep = new IntroductionStepElement()
                .setTeaserText("**Wichtig**\n\n- Punkt A");
        var form = new VFormVersionWithDetailsEntity()
                .setSlug("test-form")
                .setRootElement(new FormLayoutElement().setChildren(List.of(introductionStep)));
        var html = new TemplateLoaderService().processTemplate(
                "form-parts/allgemeine-informationen.html",
                Map.of(
                        "base", createBaseContext(FormPdfScope.Blank),
                        "form", form
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
                ZonedDateTime.of(2025, 1, 2, 0, 0, 0, 0, DateInputElement.zoneId),
                ZonedDateTime.of(2025, 1, 5, 0, 0, 0, 0, DateInputElement.zoneId)
        );
        var timeRangeValue = new RangeInputElementValue(
                ZonedDateTime.of(2025, 1, 2, 9, 15, 30, 0, DateInputElement.zoneId),
                ZonedDateTime.of(2025, 1, 2, 11, 45, 15, 0, DateInputElement.zoneId)
        );
        var dateTimeRangeValue = new RangeInputElementValue(
                ZonedDateTime.of(2025, 1, 2, 9, 15, 0, 0, DateInputElement.zoneId),
                ZonedDateTime.of(2025, 1, 5, 17, 0, 0, 0, DateInputElement.zoneId)
        );

        var form = new VFormVersionWithDetailsEntity()
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

        var form = new VFormVersionWithDetailsEntity()
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
                                                ZonedDateTime.of(2025, 1, 2, 9, 15, 30, 0, DateInputElement.zoneId),
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

    private int countOccurrences(String haystack, String needle) {
        var count = 0;
        var currentIndex = 0;

        while ((currentIndex = haystack.indexOf(needle, currentIndex)) >= 0) {
            count++;
            currentIndex += needle.length();
        }

        return count;
    }

    private FormPdfContext createBaseContext(FormPdfScope scope) {
        var goverConfig = new GoverConfig();
        goverConfig.setGoverHostname("https://gover.example/");
        return new FormPdfContext("", "", "", goverConfig, scope);
    }
}
