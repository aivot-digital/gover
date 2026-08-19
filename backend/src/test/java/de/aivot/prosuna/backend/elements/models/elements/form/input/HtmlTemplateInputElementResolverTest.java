package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.asset.entities.AssetEntity;
import de.aivot.prosuna.backend.asset.services.AssetService;
import de.aivot.prosuna.backend.elements.models.elements.form.input.HtmlTemplateInputElementResolver;
import de.aivot.prosuna.backend.elements.models.elements.form.input.HtmlTemplateInputElementValue;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.services.TemplateRenderService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HtmlTemplateInputElementResolverTest {
    private AssetService assetService;
    private StorageService storageService;
    private HtmlTemplateInputElementResolver resolver;

    @BeforeEach
    void setUp() {
        assetService = mock(AssetService.class);
        storageService = mock(StorageService.class);
        resolver = new HtmlTemplateInputElementResolver(
                assetService,
                storageService,
                new PassthroughTemplateRenderService()
        );
    }

    @Test
    void resolveShouldApplyTextImageAndRichTextSlotValues() throws Exception {
        var templateAssetKey = UUID.randomUUID();
        var imageAssetKey = UUID.randomUUID();
        var templateHtml = """
                <html>
                    <body>
                        <h1 data-slot="title" data-slot-type="text">Default title</h1>
                        <p data-slot="title" data-slot-type="text">Default title again</p>
                        <img data-slot="logo" data-slot-type="image" src="/default-logo.png"/>
                        <section data-slot="content" data-slot-type="richtext">Default content</section>
                        <p data-slot="default_text" data-slot-type="text">Keep me</p>
                    </body>
                </html>
                """;

        when(assetService.retrieve(templateAssetKey)).thenReturn(Optional.of(
                new AssetEntity()
                        .setKey(templateAssetKey)
                        .setStorageProviderId(11)
                        .setStoragePathFromRoot("templates/template.html")
        ));
        when(storageService.getDocumentContent(11, "templates/template.html"))
                .thenReturn(new ByteArrayInputStream(templateHtml.getBytes(StandardCharsets.UTF_8)));
        when(assetService.createUrl(imageAssetKey))
                .thenReturn("/api/public/assets/" + imageAssetKey + "/");

        var result = resolver.resolve(
                new HtmlTemplateInputElementValue()
                        .setAssetKey(templateAssetKey.toString())
                        .setSlots(Map.of(
                                "title", "Neuer <Titel>",
                                "logo", imageAssetKey.toString(),
                                "content", "**Hallo** <script>alert(1)</script>"
                        )),
                new ProcessExecutionData()
        );

        assertTrue(result.contains("<h1 data-slot=\"title\" data-slot-type=\"text\">Neuer &lt;Titel&gt;</h1>"));
        assertTrue(result.contains("<p data-slot=\"title\" data-slot-type=\"text\">Neuer &lt;Titel&gt;</p>"));
        assertTrue(result.contains("src=\"/api/public/assets/" + imageAssetKey + "/\""));
        assertTrue(result.contains("<strong>Hallo</strong> &lt;script&gt;alert(1)&lt;/script&gt;"));
        assertTrue(result.contains("<p data-slot=\"default_text\" data-slot-type=\"text\">Keep me</p>"));
    }

    private static class PassthroughTemplateRenderService extends TemplateRenderService {
        private PassthroughTemplateRenderService() {
            super(null);
        }

        @Override
        public String interpolate(ProcessExecutionData foldedProcessData, String template) {
            return template;
        }
    }
}
