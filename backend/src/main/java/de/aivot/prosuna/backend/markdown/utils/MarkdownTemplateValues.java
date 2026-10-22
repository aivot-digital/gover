package de.aivot.prosuna.backend.markdown.utils;

import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Link;
import org.commonmark.node.LinkReferenceDefinition;
import org.commonmark.node.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Prepares process data for insertion into Markdown templates, such as the mail texts staff write with
 * {@code {{ ... }}} and {@code {! ... !}}.
 *
 * <p>Process data regularly contains text entered by customers. Inserted unchanged, it could add formatting and
 * links that look like part of the authority's message, including links whose target is hidden behind other text.</p>
 */
public final class MarkdownTemplateValues {
    private static final Pattern LINE_BREAK = Pattern.compile("\\R");
    private static final Pattern ENTITY_REFERENCE = Pattern.compile("&(?:#[0-9]{1,7}|#[xX][0-9a-fA-F]{1,6}|[A-Za-z][A-Za-z0-9]{1,31});");
    private static final Pattern ORDERED_LIST_MARKER = Pattern.compile("^[0-9]{1,9}[.)]");
    private static final String ALWAYS_ESCAPED = "\\`*_~[]<|";
    private static final String ESCAPED_AT_LINE_START = "#>-+=";
    private static final org.commonmark.renderer.markdown.MarkdownRenderer MARKDOWN_WRITER = org.commonmark.renderer.markdown.MarkdownRenderer.builder()
            .extensions(MarkdownRenderer.EXTENSIONS)
            .build();

    private MarkdownTemplateValues() {
    }

    /**
     * Escapes text so it renders exactly as typed wherever it is inserted, including the middle of a list item or a
     * link target.
     *
     * <p>Line breaks are kept. An empty line becomes a hard line break instead of a paragraph break, so a multi-line
     * value cannot end the list item or quote it was inserted into. Leading whitespace is dropped because Markdown
     * would otherwise read four spaces at the start of a block as code. Bare web and e-mail addresses still become
     * links, but those show their target.</p>
     */
    @Nonnull
    public static String escapeText(@Nullable String text) {
        if (StringUtils.isNullOrEmpty(text)) {
            return "";
        }

        var lines = new ArrayList<>(List.of(LINE_BREAK.split(text, -1)));
        while (!lines.isEmpty() && lines.getFirst().isBlank()) {
            lines.removeFirst();
        }
        while (!lines.isEmpty() && lines.getLast().isBlank()) {
            lines.removeLast();
        }

        var escaped = new StringBuilder(text.length() + 16);
        for (var i = 0; i < lines.size(); i++) {
            if (i > 0) {
                escaped.append('\n');
            }
            var line = lines.get(i).stripLeading();
            if (line.isEmpty()) {
                // A backslash at the end of a line is a hard line break. Empty lines are never the last line here.
                escaped.append('\\');
            } else {
                appendEscapedLine(escaped, line);
            }
        }
        return escaped.toString();
    }

    /**
     * Keeps the Markdown formatting of a value, but writes the target of every link next to its text, for example
     * {@code [Antrag](https://example.org)} becomes {@code Antrag (https://example.org)}. Links that already show
     * their target, such as bare web addresses, stay links.
     *
     * <p>Link reference definitions are removed as well. They apply to the whole document, so a value could otherwise
     * change the target of a reference link in the surrounding template.</p>
     *
     * <p>A value with nothing to change is returned as it was written. Otherwise the Markdown writer writes the whole
     * value again, which can change how its syntax is spelled, but not what it shows.</p>
     */
    @Nonnull
    public static String revealLinkTargets(@Nullable String markdown) {
        if (StringUtils.isNullOrEmpty(markdown)) {
            return "";
        }

        var document = MarkdownRenderer.PARSER.parse(markdown);
        var collector = new LinkCollector();
        document.accept(collector);

        var linksHidingTheirTarget = collector.links
                .stream()
                .filter(link -> !MarkdownRenderer.showsItsTarget(link))
                .toList();
        if (linksHidingTheirTarget.isEmpty() && collector.definitions.isEmpty()) {
            return markdown;
        }

        for (var definition : collector.definitions) {
            definition.unlink();
        }
        for (var link : linksHidingTheirTarget) {
            var child = link.getFirstChild();
            while (child != null) {
                var next = child.getNext();
                link.insertBefore(child);
                child = next;
            }
            if (!link.getDestination().isBlank()) {
                link.insertBefore(new Text(" (" + link.getDestination() + ")"));
            }
            link.unlink();
        }

        return MARKDOWN_WRITER.render(document).stripTrailing();
    }

    private static void appendEscapedLine(@Nonnull StringBuilder escaped, @Nonnull String line) {
        var orderedListMarker = ORDERED_LIST_MARKER.matcher(line);
        var orderedListDelimiter = orderedListMarker.lookingAt() ? orderedListMarker.end() - 1 : -1;

        for (var i = 0; i < line.length(); i++) {
            var character = line.charAt(i);
            var escape = ALWAYS_ESCAPED.indexOf(character) >= 0
                    || (i == 0 && ESCAPED_AT_LINE_START.indexOf(character) >= 0)
                    || i == orderedListDelimiter
                    || (character == '&' && ENTITY_REFERENCE.matcher(line).region(i, line.length()).lookingAt());
            if (escape) {
                escaped.append('\\');
            }
            escaped.append(character);
        }
    }

    private static final class LinkCollector extends AbstractVisitor {
        private final List<Link> links = new ArrayList<>();
        private final List<LinkReferenceDefinition> definitions = new ArrayList<>();

        @Override
        public void visit(Link link) {
            links.add(link);
            visitChildren(link);
        }

        @Override
        public void visit(LinkReferenceDefinition linkReferenceDefinition) {
            definitions.add(linkReferenceDefinition);
        }
    }
}
