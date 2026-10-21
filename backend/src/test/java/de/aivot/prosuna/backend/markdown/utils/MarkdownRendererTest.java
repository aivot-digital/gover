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

    record RenderingCases(String description, List<RenderingCase> cases) {
    }

    record RenderingCase(String name, String markdown, List<String> contains, List<String> excludes) {
        @Override
        public String toString() {
            return name;
        }
    }
}
