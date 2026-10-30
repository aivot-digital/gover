package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.pdf.enums.FormPdfScope;
import de.aivot.prosuna.backend.pdf.models.FormPdfContext;
import de.aivot.prosuna.backend.services.TemplateLoaderService;
import de.aivot.prosuna.backend.services.pdf.PdfElement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.thymeleaf.templatemode.TemplateMode;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MultiCheckboxInputElementTest {
    private MultiCheckboxInputElement field() {
        var field = new MultiCheckboxInputElement();
        field.setOptions(List.of("a", "b", "c").stream()
                .map(value -> new MultiCheckboxInputElementOption().setValue(value).setLabel(value)).toList());
        field.setMinimumRequiredOptions(2);
        field.setMaximumOptions(2);
        return field;
    }

    @Test
    void validatesBothLimitsWithoutTruncatingExistingValues() {
        var field = field();
        assertDoesNotThrow(() -> field.validate(null));
        assertDoesNotThrow(() -> field.validate(List.of()));
        assertThrows(ValidationException.class, () -> field.validate(List.of("a")));
        assertDoesNotThrow(() -> field.validate(List.of("a", "b")));
        var values = List.of("a", "b", "c");
        assertTrue(assertThrows(ValidationException.class, () -> field.validate(values)).getMessage().contains("Maximal 2"));
        assertEquals(values, field.formatValue(values));
        field.setRequired(true);
        assertThrows(ValidationException.class, () -> field.validate(null));
        assertDoesNotThrow(() -> field.validate(List.of("a", "b")));
        assertThrows(ValidationException.class, () -> field.validate(values));
    }

    @Test
    void missingOrNonPositiveMaximumIsUnlimited() {
        var field = field();
        field.setMaximumOptions(null);
        assertDoesNotThrow(() -> field.validate(List.of("a", "b", "c")));
        field.setMaximumOptions(0);
        assertDoesNotThrow(() -> field.validate(List.of("a", "b", "c")));
        field.setMaximumOptions(-1);
        assertDoesNotThrow(() -> field.validate(List.of("a", "b", "c")));
    }

    @Test
    void persistsTheLimitAndIncludesItInEquality() throws Exception {
        var mapper = JsonMapperFactory.getInstance();
        var field = field();
        var restored = mapper.readValue(mapper.writeValueAsString(field), MultiCheckboxInputElement.class);
        assertEquals(2, restored.getMaximumOptions());
        var copy = mapper.readValue(mapper.writeValueAsString(field), MultiCheckboxInputElement.class);
        assertEquals(copy, restored);
        assertEquals(copy.hashCode(), restored.hashCode());
        restored.setMaximumOptions(3);
        assertNotEquals(copy, restored);
    }

    @ParameterizedTest
    @CsvSource({
            "0, Es können mehrere Optionen ausgewählt werden",
            "1, Es kann höchstens 1 Option ausgewählt werden",
            "2, Es können höchstens 2 Optionen ausgewählt werden"
    })
    void includesTheMaximumInTheBlankPdf(int maximum, String expected) {
        var field = field().setMaximumOptions(maximum);
        var html = new TemplateLoaderService().processTemplate("form-parts/element-dispatcher.html", Map.of(
                "base", new FormPdfContext("Prosuna", null, new ProsunaConfig(), FormPdfScope.Blank),
                "child", new PdfElement(field, null, List.of())
        ), TemplateMode.HTML);
        assertTrue(html.contains(expected));
    }
}
