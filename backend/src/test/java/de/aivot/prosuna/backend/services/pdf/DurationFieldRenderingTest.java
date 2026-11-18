package de.aivot.prosuna.backend.services.pdf;

import de.aivot.prosuna.backend.elements.models.elements.form.input.DurationInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.steps.GenericStepElement;
import de.aivot.prosuna.backend.enums.DurationLargestUnit;
import de.aivot.prosuna.backend.enums.DurationPrecision;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.pdf.enums.FormPdfScope;
import de.aivot.prosuna.backend.pdf.models.FormPdfContext;
import de.aivot.prosuna.backend.services.TemplateLoaderService;
import de.aivot.prosuna.backend.utils.DurationValueUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static de.aivot.prosuna.backend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DurationFieldRenderingTest {
    @ParameterizedTest
    @MethodSource("displayValues")
    void rendersEffectiveValuesThroughTheGeneratorAndPdfDispatcher(Object value, String expected) {
        for (var scope : List.of(FormPdfScope.Customer, FormPdfScope.Staff)) {
            var field = new DurationInputElement();
            field.setId("duration");
            field.setLabel("Aufwand");
            field.setTechnical(scope == FormPdfScope.Staff);
            var root = createRoot(field);

            var step = PdfElementsGenerator.generatePdfElements(
                    root, runtime("duration", value), scope == FormPdfScope.Customer, false).getFirst();
            var html = render(step.children().getFirst(), scope);

            assertTrue(html.contains("Aufwand"));
            assertTrue(html.contains(expected));
        }
    }

    @Test
    void keepsTechnicalDurationsOutOfCustomerPdfs() {
        var field = new DurationInputElement();
        field.setId("duration");
        field.setTechnical(true);

        var step = PdfElementsGenerator.generatePdfElements(
                createRoot(field), runtime("duration", "abc"), true, false).getFirst();

        assertTrue(step.children().isEmpty());
    }

    @Test
    void keepsBlankPrintUnitLabelsIndependentOfInvalidEffectiveValues() {
        var field = new DurationInputElement().setLargestUnit(DurationLargestUnit.Day)
                .setPrecision(DurationPrecision.Second);
        field.setId("duration");
        field.setLabel("Aufwand");

        var step = PdfElementsGenerator.generatePdfElements(
                createRoot(field), runtime("duration", "abc"), true, true).getFirst();
        var html = render(step.children().getFirst(), FormPdfScope.Blank);

        assertTrue(html.contains("Tage / Stunden / Minuten / Sekunden"));
        assertFalse(html.contains("Ungültige Zeitdauer"));
    }

    private static Stream<Arguments> displayValues() {
        return Stream.of(
                Arguments.of(null, "Keine Angabe"),
                Arguments.of(0, "0 Minuten"),
                Arguments.of("99000", "27 Stunden 30 Minuten"),
                Arguments.of(75, "1 Minute 15 Sekunden"),
                Arguments.of("", "Ungültige Zeitdauer"),
                Arguments.of("abc", "Ungültige Zeitdauer"),
                Arguments.of(-1, "Ungültige Zeitdauer"),
                Arguments.of(1.5, "Ungültige Zeitdauer"),
                Arguments.of(DurationValueUtils.MAX_SECONDS + 1, "Ungültige Zeitdauer")
        );
    }

    private FormLayoutElement createRoot(DurationInputElement field) {
        return new FormLayoutElement().setChildren(List.of(
                new GenericStepElement().setChildren(List.of(field))
        ));
    }

    private String render(PdfElement element, FormPdfScope scope) {
        return new TemplateLoaderService().processTemplate("form-parts/element-dispatcher.html", Map.of(
                "base", new FormPdfContext("Prosuna", null, new ProsunaConfig(), scope),
                "child", element
        ), TemplateMode.HTML);
    }
}
