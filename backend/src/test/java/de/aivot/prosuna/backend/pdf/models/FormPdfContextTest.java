package de.aivot.prosuna.backend.pdf.models;

import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.pdf.enums.FormPdfScope;
import de.aivot.prosuna.backend.pdf.models.FormPdfContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormPdfContextTest {
    @Test
    void createAssetUrlCreatesPublicAssetUrlByKey() {
        var config = new ProsunaConfig();
        config.setProsunaHostname("https://gover.example");
        var context = new FormPdfContext(
                "Provider",
                "123e4567-e89b-12d3-a456-426614174000",
                "Logo - Stadt.png",
                config,
                FormPdfScope.Citizen
        );

        var result = context.createAssetUrl(context.logoAssetKey(), context.logoAssetName());

        assertEquals(
                "https://gover.example/api/public/assets/123e4567-e89b-12d3-a456-426614174000/",
                result
        );
    }

    @Test
    void createUrlWithQueryParameterAddsFirstQueryParameter() {
        var context = createContext();

        var result = context.createUrlWithQueryParameter("/form/example/", "dialog", "privacy");

        assertEquals("https://gover.example/form/example/?dialog=privacy", result);
    }

    @Test
    void createUrlWithQueryParameterAppendsAdditionalQueryParameter() {
        var context = createContext();

        var result = context.createUrlWithQueryParameter("/form/example/?version=7", "dialog", "privacy");

        assertEquals("https://gover.example/form/example/?version=7&dialog=privacy", result);
    }

    private FormPdfContext createContext() {
        var config = new ProsunaConfig();
        config.setProsunaHostname("https://gover.example");
        return new FormPdfContext(
                "Provider",
                "123e4567-e89b-12d3-a456-426614174000",
                "Logo - Stadt.png",
                config,
                FormPdfScope.Citizen
        );
    }
}
