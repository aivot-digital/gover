package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.asset.entities.AssetEntity;
import de.aivot.prosuna.backend.asset.services.AssetService;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.services.TemplateRenderService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
                new TemplateRenderService(new JavascriptEngineFactoryService(List.of()))
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

    @Test
    void resolveShouldReplaceFilledSlotDefaultsBeforeRenderingAndRenderUnfilledDefaults() throws Exception {
        var value = template("<p data-slot=\"filled\" data-slot-type=\"text\">{{ invalid default</p>"
                + "<p data-slot=\"empty\" data-slot-type=\"text\">{{ $.fallback }}</p>"
                + "<p data-slot=\"missing\" data-slot-type=\"text\">{{ $.fallback }}</p>")
                .setSlots(Map.of("filled", "{{ $.input }}", "empty", ""));
        var data = new ProcessExecutionData()
                .addProcessData("input", "{{ $.secret }}")
                .addProcessData("fallback", "Default & text")
                .addProcessData("secret", "must-not-appear");

        assertEquals(
                "<p data-slot=\"filled\" data-slot-type=\"text\">{{ $.secret }}</p>"
                        + "<p data-slot=\"empty\" data-slot-type=\"text\">Default &amp; text</p>"
                        + "<p data-slot=\"missing\" data-slot-type=\"text\">Default &amp; text</p>",
                resolver.resolve(value, data)
        );
    }

    @Test
    void resolveShouldRenderSharedBlocksAndRepeatedSlotsWithinLoops() throws Exception {
        var value = template("{% useBlock rows %}{% if false %}<p data-slot=\"text\" data-slot-type=\"text\">Hidden</p>{% endif %}"
                + "{% block rows %}{% for item in $.items %}"
                + "<p>{{ item }}<span data-slot=\"text\" data-slot-type=\"text\">Default</span></p>"
                + "{% endfor %}{% endblock %}")
                .setSlots(Map.of("text", "{{ $.input }}"));
        var data = new ProcessExecutionData()
                .addProcessData("input", "{{ 7 * 7 }}")
                .addProcessData("items", List.of("A", "B"));

        assertEquals(
                "<p>A<span data-slot=\"text\" data-slot-type=\"text\">{{ 7 * 7 }}</span></p>"
                        + "<p>B<span data-slot=\"text\" data-slot-type=\"text\">{{ 7 * 7 }}</span></p>",
                resolver.resolve(value, data)
        );
    }

    @Test
    void resolveShouldPreserveSlotFormattingAndTreatImageUrlsAsData() throws Exception {
        var imageKey = UUID.randomUUID();
        var value = template("<p data-slot=\"text\" data-slot-type=\"text\">Default</p>"
                + "<section data-slot=\"richtext\" data-slot-type=\"richtext\">Default</section>"
                + "<img data-slot=\"image\" data-slot-type=\"image\" src=\"default.png\" alt=\"{{ $.alt }}\"/>")
                .setSlots(Map.of("text", "{! $.input !}", "richtext", "{! $.markdown !}", "image", "{{ $.image }}"));
        var data = new ProcessExecutionData()
                .addProcessData("input", "<tag> & {{ $.secret }} $5\\path")
                .addProcessData("markdown", "**Hello** <script>alert(1)</script> {{ $.secret }}")
                .addProcessData("image", imageKey.toString())
                .addProcessData("alt", "Logo")
                .addProcessData("secret", "must-not-appear");
        when(assetService.createUrl(imageKey)).thenReturn("/logo.png?literal={{ $.secret }}&size=10");

        assertEquals(
                "<p data-slot=\"text\" data-slot-type=\"text\">&lt;tag&gt; &amp; {{ $.secret }} $5\\path</p>"
                        + "<section data-slot=\"richtext\" data-slot-type=\"richtext\"><p><strong>Hello</strong> "
                        + "&lt;script&gt;alert(1)&lt;/script&gt; {{ $.secret }}</p>\n</section>"
                        + "<img data-slot=\"image\" data-slot-type=\"image\" src=\"/logo.png?literal={{ $.secret }}&amp;size=10\" alt=\"Logo\"/>",
                resolver.resolve(value, data)
        );
    }

    @Test
    void restoreSlotContentsShouldNotReplacePlaceholderTextWithinInsertedValues() {
        var firstPlaceholder = "PROSUNA_SLOT_" + UUID.randomUUID() + "_END";
        var secondPlaceholder = "PROSUNA_SLOT_" + UUID.randomUUID() + "_END";
        var html = "<p>" + firstPlaceholder + "</p><p>" + secondPlaceholder + "</p>";

        String result = ReflectionTestUtils.invokeMethod(resolver, "restoreSlotContents", html, Map.of(
                firstPlaceholder, secondPlaceholder,
                secondPlaceholder, "$5\\path {{ $.secret }}"
        ));

        assertEquals("<p>" + secondPlaceholder + "</p><p>$5\\path {{ $.secret }}</p>", result);
    }

    @Test
    void resolveShouldPreservePlaceholderLikeTextInTemplateAndSlotContent() throws Exception {
        var literal = "PROSUNA_SLOT_" + UUID.randomUUID() + "_END";
        var content = literal + " PROSUNA_SLOT_" + UUID.randomUUID() + "_END";
        var value = template(literal + "<p data-slot=\"text\" data-slot-type=\"text\">Default</p>")
                .setSlots(Map.of("text", content));

        assertEquals(
                literal + "<p data-slot=\"text\" data-slot-type=\"text\">" + content + "</p>",
                resolver.resolve(value, new ProcessExecutionData())
        );
    }

    @Test
    void resolveShouldStillRejectInvalidAuthoredTemplateSyntax() throws Exception {
        var value = template("{{ unclosed");

        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(value, new ProcessExecutionData()));
    }

    @Test
    void resolveShouldStillRejectInvalidAuthoredSlotSyntax() throws Exception {
        var value = template("<p data-slot=\"text\" data-slot-type=\"text\">Default</p>")
                .setSlots(Map.of("text", "{{ unclosed"));

        assertThrows(ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> resolver.resolve(value, new ProcessExecutionData()));
    }

    private HtmlTemplateInputElementValue template(String html) throws Exception {
        var assetKey = UUID.randomUUID();
        when(assetService.retrieve(assetKey)).thenReturn(Optional.of(
                new AssetEntity().setKey(assetKey).setStorageProviderId(11).setStoragePathFromRoot("templates/test.html")
        ));
        when(storageService.getDocumentContent(11, "templates/test.html"))
                .thenReturn(new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)));
        return new HtmlTemplateInputElementValue().setAssetKey(assetKey.toString()).setSlots(Map.of());
    }
}
