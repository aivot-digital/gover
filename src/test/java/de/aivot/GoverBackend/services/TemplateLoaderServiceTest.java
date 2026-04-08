package de.aivot.GoverBackend.services;

import de.aivot.GoverBackend.elements.models.elements.form.input.CheckboxInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.steps.IntroductionStepElement;
import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsEntity;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.pdf.enums.FormPdfScope;
import de.aivot.GoverBackend.pdf.models.FormPdfContext;
import de.aivot.GoverBackend.services.pdf.MarkdownDialect;
import org.junit.jupiter.api.Test;
import org.thymeleaf.templatemode.TemplateMode;

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
