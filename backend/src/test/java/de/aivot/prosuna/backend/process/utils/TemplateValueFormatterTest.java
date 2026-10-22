package de.aivot.prosuna.backend.process.utils;

import de.aivot.prosuna.backend.process.enums.TemplateOutputFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplateValueFormatterTest {
    private static final String VALUE = "<b>O'Brien</b> & [Link](https://evil.example)\nZeile 2";

    @Test
    void htmlEscapesPrintedValuesForHtmlAndJavascriptAndKeepsRawValues() {
        assertEquals(
                "&lt;b&gt;O&#39;Brien&lt;/b&gt; &amp; [Link](https://evil.example)\\nZeile 2",
                TemplateValueFormatter.formatEscaped(TemplateOutputFormat.Html, VALUE)
        );
        assertEquals(VALUE, TemplateValueFormatter.formatRaw(TemplateOutputFormat.Html, VALUE));
    }

    @Test
    void markdownEscapesPrintedValuesAndRevealsLinkTargetsOfRawValues() {
        assertEquals(
                "\\<b>O'Brien\\</b> & \\[Link\\](https://evil.example)\nZeile 2",
                TemplateValueFormatter.formatEscaped(TemplateOutputFormat.Markdown, VALUE)
        );
        assertEquals(
                "<b>O'Brien</b> \\& Link (https://evil.example)\nZeile 2",
                TemplateValueFormatter.formatRaw(TemplateOutputFormat.Markdown, VALUE)
        );
    }

    @Test
    void plainTextKeepsValuesUnchanged() {
        assertEquals(VALUE, TemplateValueFormatter.formatEscaped(TemplateOutputFormat.PlainText, VALUE));
        assertEquals(VALUE, TemplateValueFormatter.formatRaw(TemplateOutputFormat.PlainText, VALUE));
    }

    @Test
    void singleLineTextReplacesEveryKindOfLineBreak() {
        var value = "Antrag\r\nBcc: someone@example.org\n\nTeil drei\rvier";

        assertEquals(
                "Antrag Bcc: someone@example.org Teil drei vier",
                TemplateValueFormatter.formatEscaped(TemplateOutputFormat.SingleLineText, value)
        );
        assertEquals(
                "Antrag Bcc: someone@example.org Teil drei vier",
                TemplateValueFormatter.formatRaw(TemplateOutputFormat.SingleLineText, value)
        );
    }

    @ParameterizedTest
    @EnumSource(TemplateOutputFormat.class)
    void missingValuesBecomeEmptyText(TemplateOutputFormat format) {
        assertEquals("", TemplateValueFormatter.formatEscaped(format, null));
        assertEquals("", TemplateValueFormatter.formatRaw(format, null));
    }
}
