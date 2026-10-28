package de.aivot.prosuna.backend.markdown.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownRendererTest {
    // The frontend MarkdownRenderer spec runs the same cases, which keeps both renderers aligned.
    private static final String SHARED_RENDERING_CASES = "markdown/markdown-rendering-cases.json";

    static Stream<RenderingCase> sharedRenderingCases() throws IOException {
        try (var input = new ClassPathResource(SHARED_RENDERING_CASES).getInputStream()) {
            return JsonMapper.builder()
                    .build()
                    .readValue(input, RenderingCases.class)
                    .cases()
                    .stream();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sharedRenderingCases")
    void rendersSharedCase(RenderingCase renderingCase) {
        var html = MarkdownRenderer.renderHtml(renderingCase.markdown());

        for (var expected : renderingCase.contains()) {
            assertTrue(html.contains(expected), () -> "Expected <%s> in: %s".formatted(expected, html));
        }
        for (var unexpected : renderingCase.excludes()) {
            assertFalse(html.contains(unexpected), () -> "Unexpected <%s> in: %s".formatted(unexpected, html));
        }
    }

    @Test
    void rendersMissingMarkdownAsEmptyHtml() {
        assertEquals("", MarkdownRenderer.renderHtml(null));
        assertEquals("", MarkdownRenderer.renderHtml(""));
    }

    @Test
    void escapesImageAlternativeText() {
        assertEquals(
                "<p>&lt;b&gt;Logo&lt;/b&gt;</p>\n",
                MarkdownRenderer.renderHtml("![\\<b\\>Logo\\</b\\>](https://example.org/logo.png)")
        );
    }

    @Test
    void rendersReadablePlainText() {
        var markdown = """
                # Ihr Antrag

                Hallo **Ada**, siehe [Portal](https://portal.example), https://example.org und `AZ-1`.
                Zeile 2 mit ![Logo](https://example.org/logo.png)\\
                Zeile 3[^1]

                - eins
                - zwei

                > Zitat

                [^1]: Fußnote
                """;

        assertEquals("""
                Ihr Antrag

                Hallo Ada, siehe Portal (https://portal.example), https://example.org und AZ-1.
                Zeile 2 mit Logo
                Zeile 3[1]

                - eins
                - zwei

                «Zitat»

                [1] Fußnote""", MarkdownRenderer.renderPlainText(markdown));
    }

    @Test
    void rendersTemplateValuesAsTheyWereTypedInPlainText() {
        var value = "**Danke** für [Antrag](https://evil.example)\n\n5 * 3 & snake_case";

        assertEquals(
                "Danke für Antrag (https://evil.example)\n\n5 * 3 & snake_case",
                MarkdownRenderer.renderPlainText(value)
        );
        assertEquals(
                "**Danke** für [Antrag](https://evil.example)\n\n5 * 3 & snake_case",
                MarkdownRenderer.renderPlainText(MarkdownTemplateValues.escapeText(value))
        );
    }

    @Test
    void keepsTemplateValuesWithOtherLinkProtocolsAsText() {
        assertEquals(
                "<p>[Datei](file:///etc/passwd)</p>\n",
                MarkdownRenderer.renderHtml(MarkdownTemplateValues.escapeText("[Datei](file:///etc/passwd)"))
        );
    }

    @Test
    void rendersMissingMarkdownAsEmptyPlainText() {
        assertEquals("", MarkdownRenderer.renderPlainText(null));
        assertEquals("", MarkdownRenderer.renderPlainText(""));
    }

    record RenderingCases(String description, List<RenderingCase> cases) {
    }

    record RenderingCase(String name, String markdown, List<String> contains, List<String> excludes) {
        @Override
        public String toString() {
            return name;
        }
    }
}
