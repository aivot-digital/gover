package de.aivot.GoverBackend.pdf.models;

import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.pdf.enums.FormPdfScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormPdfContextTest {
    @Test
    void createAssetUrlCreatesPublicAssetUrlByKey() {
        var config = new GoverConfig();
        config.setGoverHostname("https://gover.example");
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
}
