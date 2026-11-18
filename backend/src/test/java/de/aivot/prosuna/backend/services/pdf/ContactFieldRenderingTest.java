package de.aivot.prosuna.backend.services.pdf;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.form.input.ContactInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.EmailInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.PhoneNumberInputElement;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.ozgCloud.services.OZGCloudDataFormatService;
import de.aivot.prosuna.backend.pdf.enums.FormPdfScope;
import de.aivot.prosuna.backend.pdf.models.FormPdfContext;
import de.aivot.prosuna.backend.services.TemplateLoaderService;
import org.junit.jupiter.api.Test;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.List;
import java.util.Map;

import static de.aivot.prosuna.backend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.*;

class ContactFieldRenderingTest {
    @Test
    void rendersBothFieldsThroughThePdfDispatcherAndStructuredExport() {
        for (var field : new ContactInputElement[]{new PhoneNumberInputElement(), new EmailInputElement()}) {
            field.setId("contact");
            field.setLabel("Kontakt");
            field.setRequired(true);
            field.setHint("Für Rückfragen");
            var raw = field instanceof PhoneNumberInputElement ? "+4930123456" : "person@example.org";
            var display = field instanceof PhoneNumberInputElement ? "+49 30 123456" : raw;
            for (var scope : List.of(FormPdfScope.Customer, FormPdfScope.Blank)) {
                var context = new FormPdfContext("Prosuna", null, new ProsunaConfig(), scope);
                var html = new TemplateLoaderService().processTemplate("form-parts/element-dispatcher.html", Map.of(
                        "base", context,
                        "child", new PdfElement(field, scope == FormPdfScope.Blank ? null : raw, List.of())
                ), TemplateMode.HTML);
                assertTrue(html.contains("Kontakt"));
                if (scope == FormPdfScope.Blank) {
                    assertTrue(html.contains("Pflichtangabe"));
                    assertTrue(html.contains("Für Rückfragen"));
                } else {
                    assertTrue(html.contains(display));
                }
            }
            var exported = new OZGCloudDataFormatService().buildFormData(field,
                    new AuthoredElementValues().putLiteral("contact", raw), runtime("contact", raw));
            assertEquals(display, exported.getFirst().stringValue());
        }
    }
}
