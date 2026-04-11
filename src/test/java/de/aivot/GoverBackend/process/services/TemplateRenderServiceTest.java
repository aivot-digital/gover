package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.GoverBackend.process.models.ProcessExecutionData;
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
    void shouldExtractBlocksBeforeRenderingAndAllowUseBeforeDefinition() {
        var result = createService().interpolate(
                createProcessData(),
                "A{% useBlock content %}B{% block content %}[{{ $.items[0].toUpperCase() }}]{% endblock %}"
        );

        assertEquals("A[ALPHA]B", result);
    }

    @Test
    void shouldRenderBlocksInTheScopeOfTheUseSite() {
        var result = createService().interpolate(
                createProcessData(),
                "{% block row %}[{{ item.toUpperCase() }}]{% endblock %}{% for item in $.items %}{% useBlock row %}{% endfor %}"
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
    void shouldReportUndefinedBlockUsage() {
        var diagnostics = createService().validateInterpolationSyntax("""
                before
                {% useBlock missing %}
                after
                """);

        assertEquals(1, diagnostics.size());
        assertEquals(2, diagnostics.getFirst().lineNumber());
        assertEquals("{% useBlock missing %}", diagnostics.getFirst().lineContent());
        assertEquals("useBlock directive references undefined block 'missing'.", diagnostics.getFirst().message());
    }

    @Test
    void shouldReportUnexpectedEndblockDirective() {
        var diagnostics = createService().validateInterpolationSyntax("""
                before
                {% endblock %}
                after
                """);

        assertEquals(1, diagnostics.size());
        assertEquals(2, diagnostics.getFirst().lineNumber());
        assertEquals("{% endblock %}", diagnostics.getFirst().lineContent());
        assertEquals("Unexpected directive '{% endblock %}'.", diagnostics.getFirst().message());
    }

    @Test
    void shouldReportDuplicateBlockDefinitions() {
        var diagnostics = createService().validateInterpolationSyntax("""
                {% block content %}one{% endblock %}
                {% block content %}two{% endblock %}
                """);

        assertEquals(1, diagnostics.size());
        assertEquals(2, diagnostics.getFirst().lineNumber());
        assertEquals("{% block content %}two{% endblock %}", diagnostics.getFirst().lineContent());
        assertEquals("Block 'content' is defined multiple times.", diagnostics.getFirst().message());
    }

    @Test
    void shouldReportCircularBlockReferences() {
        var diagnostics = createService().validateInterpolationSyntax("""
                {% block first %}
                {% useBlock second %}
                {% endblock %}
                {% block second %}
                {% useBlock first %}
                {% endblock %}
                """);

        assertEquals(1, diagnostics.size());
        assertEquals(5, diagnostics.getFirst().lineNumber());
        assertEquals("{% useBlock first %}", diagnostics.getFirst().lineContent());
        assertEquals("Circular block reference detected involving 'first'.", diagnostics.getFirst().message());
    }

    @Test
    void shouldReportMissingEndblockDirective() {
        var diagnostics = createService().validateInterpolationSyntax("""
                before
                {% block content %}
                after
                """);

        assertEquals(1, diagnostics.size());
        assertEquals(2, diagnostics.getFirst().lineNumber());
        assertEquals("{% block content %}", diagnostics.getFirst().lineContent());
        assertEquals("Block is missing '{% endblock %}'.", diagnostics.getFirst().message());
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

    private static ProcessExecutionData createProcessData() {
        return ProcessExecutionData.of(Map.of(
                "$", Map.of(
                        "enabled", true,
                        "items", List.of("alpha", "beta")
                ),
                "$$", Map.of(),
                "_unsafe", "<tag>\n"
        ));
    }
}
