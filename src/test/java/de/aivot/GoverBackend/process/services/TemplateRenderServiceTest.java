package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.javascript.services.JavascriptEngineFactoryService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TemplateRenderServiceTest {
    @Test
    void shouldEscapeHtmlAndJsInPrintTags() {
        var result = createService().interpolate(
                createProcessData(),
                "{{ _unsafe }}"
        );

        assertEquals("&lt;tag&gt;\\n", result);
    }

    @Test
    void shouldBypassEscapingInRawTagsAndStripComments() {
        var result = createService().interpolate(
                createProcessData(),
                "A{# hidden #}{! _unsafe !}B"
        );

        assertEquals("A<tag>\nB", result);
    }

    @Test
    void shouldRenderMultilineIfForAndPrintTags() {
        var result = createService().interpolate(
                createProcessData(),
                "{% if\n$.enabled\n%}{% for\nitem in $.items\n%}[{{\nitem.toUpperCase()\n}}]{% endfor %}{% else %}nope{% endif %}"
        );

        assertEquals("[ALPHA][BETA]", result);
    }

    @Test
    void shouldReturnLineBasedDiagnosticsForInvalidSyntax() {
        var diagnostics = createService().validateInterpolationSyntax("""
                before
                {% if $.enabled %}
                after
                """);

        assertEquals(1, diagnostics.size());
        assertEquals(2, diagnostics.getFirst().lineNumber());
        assertEquals("{% if $.enabled %}", diagnostics.getFirst().lineContent());
        assertEquals("If block is missing '{% endif %}'.", diagnostics.getFirst().message());
    }

    @Test
    void shouldRejectInterpolationWhenSyntaxIsInvalid() {
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> createService().interpolate(
                        createProcessData(),
                        "before\n{% if $.enabled %}\nafter"
                )
        );

        assertEquals(
                "Invalid template syntax.\nLine 2: If block is missing '{% endif %}'. [{% if $.enabled %}]",
                exception.getMessage()
        );
    }

    private static TemplateRenderService createService() {
        return new TemplateRenderService(new JavascriptEngineFactoryService(List.of()));
    }

    private static Map<String, Object> createProcessData() {
        return Map.of(
                "$", Map.of(
                        "enabled", true,
                        "items", List.of("alpha", "beta")
                ),
                "$$", Map.of(),
                "_unsafe", "<tag>\n"
        );
    }
}
