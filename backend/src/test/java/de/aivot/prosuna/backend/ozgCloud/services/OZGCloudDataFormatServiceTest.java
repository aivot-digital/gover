package de.aivot.prosuna.backend.ozgCloud.services;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RichTextInputElement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OZGCloudDataFormatServiceTest {
    @Test
    void buildFormDataSendsMarkdownAsReadablePlainText() {
        var field = markdownField();
        var values = new AuthoredElementValues()
                .putLiteral("nachricht", "**Wichtig:** siehe [Antrag](https://example.org/antrag)");

        var items = new OZGCloudDataFormatService().buildFormData(field, values, new DerivedRuntimeElementData());

        assertEquals(1, items.size());
        assertEquals("Nachricht", items.getFirst().label());
        assertEquals("Wichtig: siehe Antrag (https://example.org/antrag)", items.getFirst().stringValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  \n "})
    void buildFormDataSendsMissingMarkdownAsNoAnswer(String markdown) {
        var values = new AuthoredElementValues().putLiteral("nachricht", markdown);

        var items = new OZGCloudDataFormatService().buildFormData(markdownField(), values, new DerivedRuntimeElementData());

        assertEquals("Keine Angabe", items.getFirst().stringValue());
    }

    private static RichTextInputElement markdownField() {
        var field = new RichTextInputElement();
        field.setId("nachricht");
        field.setLabel("Nachricht");
        return field;
    }
}
