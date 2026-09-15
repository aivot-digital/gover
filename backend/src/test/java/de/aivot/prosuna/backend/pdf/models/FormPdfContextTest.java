package de.aivot.prosuna.backend.pdf.models;

import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.pdf.enums.FormPdfScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormPdfContextTest {
    @Test
    void createUrlWithQueryParameterAddsFirstQueryParameter() {
        var context = createContext();

        var result = context.createUrlWithQueryParameter("/form/example/", "dialog", "privacy");

        assertEquals("https://prosuna.example/form/example/?dialog=privacy", result);
    }

    @Test
    void createUrlWithQueryParameterAppendsAdditionalQueryParameter() {
        var context = createContext();

        var result = context.createUrlWithQueryParameter("/form/example/?version=7", "dialog", "privacy");

        assertEquals("https://prosuna.example/form/example/?version=7&dialog=privacy", result);
    }

    private FormPdfContext createContext() {
        var config = new ProsunaConfig();
        config.setProsunaHostname("https://prosuna.example");
        return new FormPdfContext(
                "Provider",
                null,
                config,
                FormPdfScope.Customer
        );
    }
}
