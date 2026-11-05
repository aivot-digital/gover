package de.aivot.prosuna.backend.javascript.providers;

import de.aivot.prosuna.backend.javascript.models.JavascriptCode;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngine;
import de.aivot.prosuna.backend.plugins.core.v1.javascript.TextJavascriptV1;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

class TextJavascriptPluginTest {
    @Test
    void fromMarkdownReturnsTheTextAReaderSees() {
        try (var jsService = new JavascriptEngine(new TextJavascriptV1())) {
            var result = jsService.evaluateCode(new JavascriptCode().setCode("""
                    [
                        _text_v1.fromMarkdown('**Fett** und 5 \\\\* 3 \\\\< 4 & Co.\\n\\n- eins\\n- [Antrag](https://example.org/antrag)'),
                        _text_v1.fromMarkdown(null),
                        _text_v1.fromMarkdown(undefined)
                    ];
                    """));

            var values = assertInstanceOf(List.class, result.asObject());

            assertEquals("Fett und 5 * 3 < 4 & Co.\n\n- eins\n- Antrag (https://example.org/antrag)", values.get(0));
            assertEquals("", values.get(1));
            assertEquals("", values.get(2));
        } catch (Exception e) {
            fail(e);
        }
    }
}
