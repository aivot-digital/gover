package de.aivot.prosuna.backend.markdown.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownTemplateValuesTest {
    private static final String CUSTOMER_MESSAGE = """
            Sehr geehrte Damen und Herren,

            bitte prüfen Sie [meinen Antrag](https://evil.example).
            **Danke**""";

    @ParameterizedTest
    @ValueSource(strings = {
            "# Kein Titel",
            "> Kein Zitat",
            "- Kein Punkt",
            "+ Kein Punkt",
            "* Kein Punkt",
            "1. Kein Punkt",
            "2) Kein Punkt",
            "- [ ] Keine Aufgabe",
            "===",
            "---",
            "***",
            "___",
            "```",
            "~~~",
            "| a | b |",
            "<div>Kein HTML</div>",
            "[1]: /keine-definition",
            "[^1]: Keine Fußnote",
            "**fett** _kursiv_ *kursiv* ~durch~ ~~durch~~ `Code`",
            "[Kein Link](/ziel) ![Kein Bild](/bild.png)",
            "&amp; &#123; &#x7B; & 5 > 3",
            "C:\\Pfad\\ Ende\\",
            "snake_case und 5*3",
            "{{ $.secret }} {! $.secret !}"
    })
    void escapeTextRendersMarkdownSyntaxAsTyped(String text) {
        assertEquals(
                "<p>" + escapeHtml(text) + "</p>\n",
                MarkdownRenderer.renderHtml(MarkdownTemplateValues.escapeText(text))
        );
    }

    @Test
    void escapeTextKeepsLineBreaksWithoutLinksOrFormatting() {
        var html = MarkdownRenderer.renderHtml("Neue Nachricht:\n\n" + MarkdownTemplateValues.escapeText(CUSTOMER_MESSAGE));

        assertEquals("""
                <p>Neue Nachricht:</p>
                <p>Sehr geehrte Damen und Herren,<br />
                <br />
                bitte prüfen Sie [meinen Antrag](<a rel="nofollow" href="https://evil.example">https://evil.example</a>).<br />
                **Danke**</p>
                """, html);
    }

    @Test
    void escapeTextKeepsMultilineValuesInsideTheSurroundingListItem() {
        var html = MarkdownRenderer.renderHtml("- Antwort: " + MarkdownTemplateValues.escapeText("Zeile 1\n\n\n  - Zeile 4\n# Zeile 5\n"));

        assertEquals("""
                <ul>
                <li>Antwort: Zeile 1<br />
                <br />
                <br />
                - Zeile 4<br />
                # Zeile 5</li>
                </ul>
                """, html);
    }

    @Test
    void escapeTextDropsIndentationThatWouldStartACodeBlock() {
        assertEquals("<p>Kein Code</p>\n", MarkdownRenderer.renderHtml(MarkdownTemplateValues.escapeText("    Kein Code")));
    }

    @Test
    void escapeTextKeepsLinkTargetsWrittenByStaffWorking() {
        var html = MarkdownRenderer.renderHtml(
                "[Formular](" + MarkdownTemplateValues.escapeText("https://example.org/form_1?a=1&b=*2*") + ")"
        );

        assertEquals("<p><a rel=\"nofollow\" href=\"https://example.org/form_1?a=1&amp;b=*2*\">Formular</a></p>\n", html);
    }

    @Test
    void escapeTextKeepsBareAddressesAsLinksThatShowTheirTarget() {
        var html = MarkdownRenderer.renderHtml(MarkdownTemplateValues.escapeText("Siehe https://example.org/a_b~c und max_muster@example.org"));

        assertEquals(
                "<p>Siehe <a rel=\"nofollow\" href=\"https://example.org/a_b~c\">https://example.org/a_b~c</a> "
                        + "und <a rel=\"nofollow\" href=\"mailto:max_muster@example.org\">max_muster@example.org</a></p>\n",
                html
        );
    }

    @Test
    void escapeTextReturnsEmptyTextForMissingValues() {
        assertEquals("", MarkdownTemplateValues.escapeText(null));
        assertEquals("", MarkdownTemplateValues.escapeText(""));
        assertEquals("", MarkdownTemplateValues.escapeText("\n \n"));
    }

    @Test
    void revealLinkTargetsWritesTargetsNextToTheLinkText() {
        var markdown = MarkdownTemplateValues.revealLinkTargets(CUSTOMER_MESSAGE);

        assertEquals("""
                Sehr geehrte Damen und Herren,

                bitte prüfen Sie meinen Antrag (https://evil.example).
                **Danke**""", markdown);
        assertTrue(MarkdownRenderer.renderHtml(markdown).contains(
                "meinen Antrag (<a rel=\"nofollow\" href=\"https://evil.example\">https://evil.example</a>)"
        ));
    }

    @Test
    void revealLinkTargetsKeepsFormattingAndLinksThatShowTheirTarget() {
        var markdown = MarkdownTemplateValues.revealLinkTargets("""
                ## Angaben

                - **Name:** Ada
                - Web: https://example.org und <https://example.org/kontakt>""");
        var html = MarkdownRenderer.renderHtml(markdown);

        assertTrue(html.contains("<h2>Angaben</h2>"));
        assertTrue(html.contains("<li><strong>Name:</strong> Ada</li>"));
        assertTrue(html.contains("<a rel=\"nofollow\" href=\"https://example.org\">https://example.org</a>"));
        assertTrue(html.contains("<a rel=\"nofollow\" href=\"https://example.org/kontakt\">https://example.org/kontakt</a>"));
    }

    @Test
    void revealLinkTargetsResolvesReferenceLinksAndRemovesTheirDefinitions() {
        var value = MarkdownTemplateValues.revealLinkTargets("""
                Mein [Antrag][1]

                [1]: https://evil.example
                [portal]: https://evil.example""");
        var html = MarkdownRenderer.renderHtml("[Portal][portal]\n\n" + value + "\n\n[portal]: https://portal.example");

        assertEquals("Mein Antrag (https://evil.example)", value);
        assertTrue(html.contains("<a rel=\"nofollow\" href=\"https://portal.example\">Portal</a>"));
    }

    @Test
    void revealLinkTargetsHandlesLinkedImagesAndEmptyTargets() {
        var markdown = MarkdownTemplateValues.revealLinkTargets(
                "[![Logo](https://example.org/logo.png)](https://evil.example) und [Text]()"
        );

        assertEquals("![Logo](https://example.org/logo.png) (https://evil.example) und Text", markdown);
        assertEquals(
                "<p>Logo (<a rel=\"nofollow\" href=\"https://evil.example\">https://evil.example</a>) und Text</p>\n",
                MarkdownRenderer.renderHtml(markdown)
        );
    }

    @Test
    void revealLinkTargetsKeepsValuesWithoutHiddenTargetsAsWritten() {
        var markdown = "Preis: 5 & 6 *Euro*\n\n* Siehe https://example.org\n\n| Feld |\n| --- |\n| Ort |";

        assertEquals(markdown, MarkdownTemplateValues.revealLinkTargets(markdown));
    }

    @Test
    void revealLinkTargetsReturnsEmptyTextForMissingValues() {
        assertEquals("", MarkdownTemplateValues.revealLinkTargets(null));
        assertEquals("", MarkdownTemplateValues.revealLinkTargets(""));
    }

    /** The characters commonmark escapes in text. */
    private static String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
